package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.chat.StreamResponse;
import com.snehil.project.lovable_clone.entity.*;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.llm.LlmResponseParser;
import com.snehil.project.lovable_clone.llm.PromptUtils;
import com.snehil.project.lovable_clone.llm.advisors.FileTreeContextAdvisor;
import com.snehil.project.lovable_clone.llm.tools.CodeGenerationTools;
import com.snehil.project.lovable_clone.repository.*;
import com.snehil.project.lovable_clone.security.AuthUtil;
import com.snehil.project.lovable_clone.service.AiGenerationService;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import com.snehil.project.lovable_clone.service.UsageService;
import com.snehil.project.lovable_clone.enums.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private static final Pattern FILE_BLOCK_PATTERN = Pattern.compile(
            "(<file\\s+path=[\"']([^\"']+)[\"'][^>]*>).*?(</file>|$)",
            Pattern.DOTALL
    );
    private static final int HISTORY_MESSAGE_CHAR_LIMIT = 4_000;
    private static final int HISTORY_MESSAGE_LIMIT = 20;

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final ProjectFileService projectFileService;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UsageService usageService;
    private final ChatFinalizerService chatFinalizerService;
    private final LlmResponseParser llmResponseParser;

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        usageService.checkDailyTokenLimit(userId);

        ChatSession session = createChatSessionIfNotExists(projectId, userId);
        List<Message> history = loadRecentHistory(session);

        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();
        Set<String> appliedPaths = ConcurrentHashMap.newKeySet();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService, projectId, userId);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .messages(history)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .concatMap(response -> Mono.fromCallable(() -> {
                    String content = extractText(response);

                    if (!content.isEmpty() && endTime.get() == 0) {
                        endTime.set(System.currentTimeMillis());
                    }

                    if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    List<StreamResponse> events = new ArrayList<>();
                    if (!content.isEmpty()) {
                        synchronized (fullResponseBuffer) {
                            fullResponseBuffer.append(content);
                        }
                        events.add(StreamResponse.chunk(content));
                    }

                    String snapshot;
                    synchronized (fullResponseBuffer) {
                        snapshot = fullResponseBuffer.toString();
                    }
                    List<LlmResponseParser.ClosedFile> newlyClosed =
                            llmResponseParser.extractNewlyClosedFiles(snapshot, appliedPaths);
                    for (LlmResponseParser.ClosedFile closed : newlyClosed) {
                        try {
                            projectFileService.saveFile(projectId, closed.path(), closed.content());
                            appliedPaths.add(closed.path());
                            events.add(StreamResponse.fileReady(closed.path()));
                            log.info("Mid-stream saved file {} for project {}", closed.path(), projectId);
                        } catch (Exception e) {
                            log.warn("Mid-stream save failed for {}: {}", closed.path(), e.getMessage());
                        }
                    }

                    return events;
                }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable))
                .doOnComplete(() -> {
                    Schedulers.boundedElastic().schedule(() -> {
                        long duration = endTime.get() > 0 ? (endTime.get() - startTime.get()) / 1000 : 0;
                        try {
                            chatFinalizerService.finalizeChat(projectId, userId, userMessage,
                                    fullResponseBuffer.toString(), duration, usageRef.get(),
                                    Set.copyOf(appliedPaths));
                        } catch (Exception e) {
                            log.error("Failed to persist chat generation for projectId: {}", projectId, e);
                        }
                    });
                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId, error));
    }

    private static String extractText(ChatResponse response) {
        if (response == null) {
            return "";
        }
        Generation generation = response.getResult();
        if (generation == null || generation.getOutput() == null) {
            return "";
        }
        String text = generation.getOutput().getText();
        return text != null ? text : "";
    }

    private List<Message> loadRecentHistory(ChatSession session) {
        List<com.snehil.project.lovable_clone.entity.ChatMessage> recent =
                chatMessageRepository.findTop20ByChatSessionOrderByCreatedAtDesc(session);
        if (recent.size() > HISTORY_MESSAGE_LIMIT) {
            recent = recent.subList(0, HISTORY_MESSAGE_LIMIT);
        }
        recent.sort(Comparator.comparing(com.snehil.project.lovable_clone.entity.ChatMessage::getCreatedAt));

        List<Message> history = new ArrayList<>();
        for (com.snehil.project.lovable_clone.entity.ChatMessage message : recent) {
            String content = message.getContent();
            if (content == null || content.isBlank()) continue;

            if (message.getRole() == MessageRole.ASSISTANT) {
                content = FILE_BLOCK_PATTERN.matcher(content)
                        .replaceAll("$1[content omitted — current version is in the project files; use read_files if needed]$3");
            }
            if (content.length() > HISTORY_MESSAGE_CHAR_LIMIT) {
                content = content.substring(0, HISTORY_MESSAGE_CHAR_LIMIT) + "\n[truncated]";
            }

            history.add(message.getRole() == MessageRole.ASSISTANT
                    ? new AssistantMessage(content)
                    : new UserMessage(content));
        }
        return history;
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if (chatSession == null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .project(project)
                    .user(user)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }
}
