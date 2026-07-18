package com.snehil.project.lovable_clone.llm;

import com.snehil.project.lovable_clone.entity.ChatEvent;
import com.snehil.project.lovable_clone.enums.ChatEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void parsesWellFormedResponse() {
        String response = """
                <message phase="start">Building it.</message>
                <file path="src/App.tsx">const a = 1;</file>
                <message phase="completed">Done!</message>
                """;

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(3);
        assertThat(events.get(0).getType()).isEqualTo(ChatEventType.MESSAGE);
        assertThat(events.get(1).getType()).isEqualTo(ChatEventType.FILE_EDIT);
        assertThat(events.get(1).getFilePath()).isEqualTo("src/App.tsx");
        assertThat(events.get(1).getContent()).isEqualTo("const a = 1;");
        assertThat(events.get(2).getContent()).isEqualTo("Done!");
    }

    @Test
    void fileClosedWithStrayArgValueTagDoesNotSwallowFollowingFiles() {
        // Observed in production: the model closes some <file> blocks with its
        // native </arg_value> template tag instead of </file>.
        String response = """
                <file path="src/lib/api.ts">export const api = 1;</arg_value>
                <file path="src/pages/Workspace.tsx">export default function Workspace() { return null; }</file>
                """;

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFilePath()).isEqualTo("src/lib/api.ts");
        assertThat(events.get(0).getContent()).isEqualTo("export const api = 1;");
        assertThat(events.get(1).getFilePath()).isEqualTo("src/pages/Workspace.tsx");
        assertThat(events.get(1).getContent()).doesNotContain("<file");
    }

    @Test
    void unterminatedFileClosesAtNextOpeningTag() {
        String response = """
                <file path="src/index.css">body { margin: 0; }
                <message phase="completed">Done!</message>
                """;

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFilePath()).isEqualTo("src/index.css");
        assertThat(events.get(0).getContent()).isEqualTo("body { margin: 0; }");
        assertThat(events.get(1).getContent()).isEqualTo("Done!");
    }

    @Test
    void unterminatedFinalBlockClosesAtEndOfResponse() {
        String response = "<file path=\"src/index.css\">body { margin: 0; }";

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getContent()).isEqualTo("body { margin: 0; }");
    }

    @Test
    void toolTagStoresArgsInMetadata() {
        String response = "<tool args=\"src/App.tsx,src/main.tsx\">Reading files...</tool>";

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo(ChatEventType.TOOL_LOG);
        assertThat(events.get(0).getMetadata()).isEqualTo("src/App.tsx,src/main.tsx");
    }

    @Test
    void pascalCaseJsxComponentsInsideFileContentDoNotSplitTheBlock() {
        // Generated React code legitimately contains <File>, <Tool> or <Message>
        // PascalCase components (e.g. lucide icons). Tag matching is case-sensitive
        // so they must stay part of the file content, not open/close protocol blocks.
        String response = """
                <file path="src/components/Inbox.tsx">import { File } from "lucide-react";
                export function Inbox() {
                  return <Message from="Ana"><File className="size-4" />attachment</Message>;
                }</file>
                <message phase="completed">Done!</message>
                """;

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getType()).isEqualTo(ChatEventType.FILE_EDIT);
        assertThat(events.get(0).getContent()).contains("<Message from=\"Ana\">")
                .contains("</Message>");
        assertThat(events.get(1).getContent()).isEqualTo("Done!");
    }

    @Test
    void stripsMarkdownFencesFromFileContent() {
        String response = "<file path=\"src/App.tsx\">```tsx\nconst a = 1;\n```</file>";

        List<ChatEvent> events = parser.parseChatEvents(response, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getContent()).isEqualTo("const a = 1;");
    }
}
