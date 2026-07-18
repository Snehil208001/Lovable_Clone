package com.snehil.project.lovable_clone.llm;

import com.snehil.project.lovable_clone.entity.ChatEvent;
import com.snehil.project.lovable_clone.entity.ChatMessage;
import com.snehil.project.lovable_clone.enums.ChatEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LlmResponseParser {

    // Deliberately case-SENSITIVE: the protocol tags are lowercase, while generated
    // React code legitimately contains PascalCase <File>, <Tool> or <Message>
    // components (e.g. lucide icons) that must not terminate or open a block.
    private static final Pattern OPEN_TAG_PATTERN = Pattern.compile(
            "<(message|file|tool)((?:\\s[^>]*)?)>"
    );

    // Some models close a block with their native tool-call template tag
    // instead of the protocol's closing tag.
    private static final String STRAY_CLOSER = "</arg_value>";

    // Helper to extract specific attributes (path="..." or path='...') from the attribute string
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(path|args)=[\"']([^\"']+)[\"']"
    );

    /**
     * Splits the response into message/file/tool events. Tolerant of malformed
     * closings: a block ends at its proper closing tag, a stray {@code </arg_value>},
     * the next opening tag, or the end of the response — whichever comes first.
     * A strict {@code <tag>...</tag>} regex silently merges every block whose
     * closing tag the model botched into the previous file's content.
     */
    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentMessage) {
        List<ChatEvent> events = new ArrayList<>();
        int orderCounter = 1;

        Matcher opener = OPEN_TAG_PATTERN.matcher(fullResponse);
        int cursor = 0;

        while (opener.find(cursor)) {
            String tagName = opener.group(1);
            Map<String, String> attrMap = extractAttributes(opener.group(2));
            int contentStart = opener.end();

            String properCloser = "</" + tagName + ">";
            int properIdx = fullResponse.indexOf(properCloser, contentStart);
            int strayIdx = fullResponse.indexOf(STRAY_CLOSER, contentStart);
            Matcher nextOpener = OPEN_TAG_PATTERN.matcher(fullResponse);
            int nextIdx = nextOpener.find(contentStart) ? nextOpener.start() : -1;

            int contentEnd = fullResponse.length();
            int resumeAt = fullResponse.length();
            if (isEarliest(properIdx, strayIdx, nextIdx)) {
                contentEnd = properIdx;
                resumeAt = properIdx + properCloser.length();
            } else if (isEarliest(strayIdx, properIdx, nextIdx)) {
                contentEnd = strayIdx;
                resumeAt = strayIdx + STRAY_CLOSER.length();
            } else if (nextIdx >= 0) {
                log.warn("Unterminated <{}> block in LLM response; closing it at the next tag", tagName);
                contentEnd = nextIdx;
                resumeAt = nextIdx;
            }

            String content = fullResponse.substring(contentStart, contentEnd).trim();

            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .chatMessage(parentMessage)
                    .content(content) // This is your Markdown content
                    .sequenceOrder(orderCounter++);

            switch (tagName) {
                case "message" -> builder.type(ChatEventType.MESSAGE);
                case "file" -> {
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.filePath(attrMap.get("path")); // Required for files

                    // Strip markdown backticks if present
                    if (content.startsWith("```")) {
                        content = content.replaceFirst("^```[a-zA-Z]*\\n?", "");
                        if (content.endsWith("```")) {
                            content = content.substring(0, content.length() - 3).trim();
                        }
                    }
                    builder.content(content);
                }
                case "tool" -> {
                    builder.type(ChatEventType.TOOL_LOG);
                    builder.metadata(attrMap.get("args")); // Store raw file list in metadata
                }
                default -> {
                    cursor = resumeAt;
                    continue;
                }
            }

            events.add(builder.build());
            cursor = resumeAt;
        }

        return events;
    }

    private boolean isEarliest(int candidate, int... others) {
        if (candidate < 0) return false;
        for (int other : others) {
            if (other >= 0 && other < candidate) return false;
        }
        return true;
    }

    /**
     * File blocks that already have a proper {@code </file>} closer and whose path
     * is not in {@code alreadyApplied}. Used for mid-stream persistence.
     */
    public List<ClosedFile> extractNewlyClosedFiles(String buffer, Set<String> alreadyApplied) {
        List<ClosedFile> closed = new ArrayList<>();
        if (buffer == null || buffer.isEmpty()) {
            return closed;
        }
        Set<String> seen = alreadyApplied != null ? alreadyApplied : Set.of();

        Matcher opener = OPEN_TAG_PATTERN.matcher(buffer);
        int cursor = 0;
        while (opener.find(cursor)) {
            String tagName = opener.group(1);
            Map<String, String> attrMap = extractAttributes(opener.group(2));
            int contentStart = opener.end();
            String properCloser = "</" + tagName + ">";
            int properIdx = buffer.indexOf(properCloser, contentStart);

            if (!"file".equals(tagName) || properIdx < 0) {
                cursor = properIdx >= 0 ? properIdx + properCloser.length() : contentStart;
                continue;
            }

            String path = attrMap.get("path");
            String content = buffer.substring(contentStart, properIdx).trim();
            if (content.startsWith("```")) {
                content = content.replaceFirst("^```[a-zA-Z]*\\n?", "");
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3).trim();
                }
            }

            if (path != null && !path.isBlank() && !seen.contains(path)) {
                closed.add(new ClosedFile(path, content));
            }
            cursor = properIdx + properCloser.length();
        }
        return closed;
    }

    public record ClosedFile(String path, String content) {}

    private Map<String, String> extractAttributes(String attributeString) {
        Map<String, String> attributes = new HashMap<>();
        if (attributeString == null) return attributes;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributeString);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2));
        }
        return attributes;
    }

}
