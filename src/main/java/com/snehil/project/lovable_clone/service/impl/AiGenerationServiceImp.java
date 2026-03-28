package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.service.AiGenerationService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiGenerationServiceImp implements AiGenerationService {


    @Override
    public Flux<String> streamResponse(String message, Long projectId) {
        return null;
    }
}
