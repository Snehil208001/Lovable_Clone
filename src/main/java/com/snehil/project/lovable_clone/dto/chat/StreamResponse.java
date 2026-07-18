package com.snehil.project.lovable_clone.dto.chat;

/**
 * SSE payload for chat streaming.
 * {@code type} is {@code "chunk"} (default text) or {@code "file_ready"} (path in {@code path}).
 */
public record StreamResponse(String type, String text, String path) {

    public static StreamResponse chunk(String text) {
        return new StreamResponse("chunk", text != null ? text : "", null);
    }

    public static StreamResponse fileReady(String path) {
        return new StreamResponse("file_ready", "", path);
    }
}
