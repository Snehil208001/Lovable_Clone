package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.chat.StreamResponse;
import com.snehil.project.lovable_clone.entity.*;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.llm.PromptUtils;
import com.snehil.project.lovable_clone.llm.advisors.FileTreeContextAdvisor;
import com.snehil.project.lovable_clone.llm.tools.CodeGenerationTools;
import com.snehil.project.lovable_clone.repository.*;
import com.snehil.project.lovable_clone.security.AuthUtil;
import com.snehil.project.lovable_clone.service.AiGenerationService;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import com.snehil.project.lovable_clone.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final ProjectFileService projectFileService;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UsageService usageService;
    private final ChatFinalizerService chatFinalizerService;

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        usageService.checkDailyTokenLimit(userId);

        createChatSessionIfNotExists(projectId, userId);

        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService, projectId, userId);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String content = response.getResult().getOutput().getText();

                    if(content != null && !content.isEmpty() && endTime.get() == 0) { // first non-empty chunk received
                        endTime.set(System.currentTimeMillis());
                    }

                    if(response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    fullResponseBuffer.append(content);
                })
                .doOnComplete(() -> {
                    Schedulers.boundedElastic().schedule(() -> {
                        long duration = (endTime.get() - startTime.get()) /  1000;
                        try {
                            chatFinalizerService.finalizeChat(projectId, userId, userMessage,
                                    fullResponseBuffer.toString(), duration, usageRef.get());
                        } catch (Exception e) {
                            log.error("Failed to persist chat generation for projectId: {}", projectId, e);
                        }
                    });
                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId, error))
                .map(response -> {
                    String text = response.getResult().getOutput().getText();
                    return new StreamResponse(text != null ? text : "");
                });
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if(chatSession == null) {
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
