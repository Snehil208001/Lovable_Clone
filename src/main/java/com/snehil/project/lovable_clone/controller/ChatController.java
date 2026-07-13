package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.chat.ChatRequest;
import com.snehil.project.lovable_clone.dto.chat.ChatResponse;
import com.snehil.project.lovable_clone.service.AiGenerationService;
import com.snehil.project.lovable_clone.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    // Chunks are JSON-encoded because raw SSE framing cannot represent a
    // token's leading space (parsers strip one space after "data:"), which
    // silently deletes the whitespace between words in the streamed code.
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final AiGenerationService aiGenerationService;
    private final ChatService chatService;

    @PostMapping(value = "/api/chat/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat (
            @RequestBody ChatRequest request){

        return aiGenerationService.streamResponse(request.message(),request.projectId())
                .map(data -> ServerSentEvent.<String>builder()
                        .data(JSON_MAPPER.writeValueAsString(data.text()))
                        .build());
    }

    @GetMapping("/api/projects/{projectId}/messages")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable Long projectId
            ) {
        return ResponseEntity.ok(chatService.getProjectChatHistory(projectId));
    }
}
