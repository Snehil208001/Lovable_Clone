package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.chat.ChatResponse;
import com.snehil.project.lovable_clone.entity.ChatMessage;
import com.snehil.project.lovable_clone.entity.ChatSession;
import com.snehil.project.lovable_clone.entity.ChatSessionId;
import com.snehil.project.lovable_clone.mapper.ChatMapper;
import com.snehil.project.lovable_clone.repository.ChatMessageRepository;
import com.snehil.project.lovable_clone.repository.ChatSessionRepository;
import com.snehil.project.lovable_clone.security.AuthUtil;
import com.snehil.project.lovable_clone.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AuthUtil authUtil;
    private final ChatMapper chatMapper;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        ChatSession chatSession = chatSessionRepository.getReferenceById(
                new ChatSessionId(projectId, userId)
        );

        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatSession(chatSession);

        return chatMapper.fromListOfChatMessage(chatMessageList);
    }
}
