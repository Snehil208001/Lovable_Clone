package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.chat.ChatRequest;
import com.snehil.project.lovable_clone.dto.chat.ChatResponse;
import com.snehil.project.lovable_clone.dto.chat.StreamResponse;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final AiGenerationService aiGenerationService;
    private final ChatService chatService;

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
        return aiGenerationService.streamResponse(request.message(), request.projectId())
                .map(this::toSseEvent)
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(JSON_MAPPER.writeValueAsString(
                                error.getMessage() != null ? error.getMessage() : "Stream failed"))
                        .build()));
    }

    private ServerSentEvent<String> toSseEvent(StreamResponse data) {
        String type = data.type() != null ? data.type() : "chunk";
        if ("file_ready".equals(type)) {
            return ServerSentEvent.<String>builder()
                    .event("file_ready")
                    .data(JSON_MAPPER.writeValueAsString(Map.of("path", data.path() != null ? data.path() : "")))
                    .build();
        }
        return ServerSentEvent.<String>builder()
                .event("chunk")
                .data(JSON_MAPPER.writeValueAsString(data.text() != null ? data.text() : ""))
                .build();
    }

    @GetMapping("/api/projects/{projectId}/messages")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(chatService.getProjectChatHistory(projectId));
    }
}
