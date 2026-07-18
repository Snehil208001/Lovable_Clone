package com.snehil.project.lovable_clone.llm.advisors;

import com.snehil.project.lovable_clone.dto.project.FileNode;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final ProjectFileService projectFileService;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
        Map<String, Object> context = request.context();
        Long projectId = Long.parseLong(context.getOrDefault("projectId", 0).toString());

        ChatClientRequest augmentedChatClientRequest = augmentRequestWithFileTree(request, projectId);

        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest augmentRequestWithFileTree(ChatClientRequest request, Long projectId) {

        List<Message> incomingMessages = request.prompt().getInstructions();

        Message systemMessage = incomingMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .findFirst()
                .orElse(null);

        List<Message> userMessages = incomingMessages.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<Message> allMessages = new ArrayList<>();

        // Add original system message
        if (systemMessage != null) {
            allMessages.add(systemMessage);
        }

        List<FileNode> fileTree = projectFileService.getFileTree(projectId);
        StringBuilder ctx = new StringBuilder("\n\n ---- FILE_TREE ----\n");
        ctx.append(fileTree.toString());
        ctx.append("\n\n ---- PROJECT_SUMMARY ----\n");
        ctx.append(buildProjectSummary(fileTree));
        allMessages.add(new SystemMessage(ctx.toString()));

        allMessages.addAll(userMessages);

        return request
                .mutate()
                .prompt(new Prompt(allMessages, request.prompt().getOptions()))
                .build();
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private static String buildProjectSummary(List<FileNode> fileTree) {
        List<String> pages = new ArrayList<>();
        List<String> components = new ArrayList<>();
        List<String> entry = new ArrayList<>();
        for (FileNode node : fileTree) {
            String path = node.path();
            if (path == null) continue;
            if (path.startsWith("src/pages/") || path.contains("/pages/")) {
                pages.add(path);
            } else if (path.startsWith("src/components/") || path.contains("/components/")) {
                components.add(path);
            } else if (path.equals("src/App.tsx") || path.equals("src/main.tsx") || path.equals("src/index.css")) {
                entry.add(path);
            }
        }
        return "entry=" + entry + "; pages=" + pages + "; components=" + components;
    }
}












