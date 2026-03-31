package com.snehil.project.lovable_clone.dto.chat;

import com.snehil.project.lovable_clone.enums.MessageRole;

import java.time.Instant;

public record ChatResponse(
        Long id,
        String content,
        MessageRole role,
        Integer tokensUsed,
        Instant createdAt
) {}
