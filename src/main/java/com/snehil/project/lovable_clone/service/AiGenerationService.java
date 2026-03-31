package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long projectId);
}
