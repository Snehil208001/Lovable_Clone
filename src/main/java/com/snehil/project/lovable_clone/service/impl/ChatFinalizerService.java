package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.entity.ChatEvent;
import com.snehil.project.lovable_clone.entity.ChatMessage;
import com.snehil.project.lovable_clone.entity.ChatSession;
import com.snehil.project.lovable_clone.entity.ChatSessionId;
import com.snehil.project.lovable_clone.enums.ChatEventType;
import com.snehil.project.lovable_clone.enums.MessageRole;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.llm.LlmResponseParser;
import com.snehil.project.lovable_clone.repository.ChatEventRepository;
import com.snehil.project.lovable_clone.repository.ChatMessageRepository;
import com.snehil.project.lovable_clone.repository.ChatSessionRepository;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import com.snehil.project.lovable_clone.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists everything produced by one completed AI generation (usage, messages,
 * events, files) in a single transaction. Separate bean from
 * {@link AiGenerationServiceImpl} so @Transactional proxying applies when called
 * from the streaming completion callback.
 */
@Service
@RequiredArgsConstructor
public class ChatFinalizerService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final LlmResponseParser llmResponseParser;
    private final ProjectFileService projectFileService;
    private final UsageService usageService;

    @Transactional
    public void finalizeChat(Long projectId, Long userId, String userMessage,
                             String fullText, long durationSeconds, Usage usage) {
        // Re-load a managed session: the callback runs on a different thread,
        // long after the request-time entity instance has detached.
        ChatSession chatSession = chatSessionRepository.findById(new ChatSessionId(projectId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", projectId + ":" + userId));

        if (usage != null) {
            usageService.recordTokenUsage(userId, usage.getTotalTokens());
        }

        Integer promptTokens = usage != null ? usage.getPromptTokens() : 0;
        Integer completionTokens = usage != null ? usage.getCompletionTokens() : 0;

        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(promptTokens)
                        .build()
        );

        ChatMessage assistantChatMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .role(MessageRole.ASSISTANT)
                        .content(fullText)
                        .chatSession(chatSession)
                        .tokensUsed(completionTokens)
                        .build()
        );

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .chatMessage(assistantChatMessage)
                .content("Thought for " + durationSeconds + "s")
                .sequenceOrder(0)
                .build());

        chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT && e.getFilePath() != null)
                .forEach(e -> projectFileService.saveFile(projectId, e.getFilePath(), e.getContent()));

        chatEventRepository.saveAll(chatEventList);
    }
}
