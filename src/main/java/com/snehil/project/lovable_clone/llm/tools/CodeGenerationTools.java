package com.snehil.project.lovable_clone.llm.tools;

import com.snehil.project.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CodeGenerationTools {
    private final ProjectFileService projectFileService;
    private final Long projectId;
    private final Long userId;

    @Tool(name = "read_files",
            description = "Read the content of files. Only input the file names present inside the FILE_TREE. DO NOT input any path which is not present under the FILE_TREE.")
    public List<String> readFiles(
            @ToolParam(description = "List of relative paths (e.g., ['src/App.tsx'])")
            List<String> paths
    ) {

        List<String> result = new ArrayList<>();

        for(String path: paths) {
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;

            log.info("Requested file: {}", cleanPath);

            // A hallucinated path must degrade to an error entry the model can react to;
            // a thrown exception here aborts the tool call and kills the whole stream.
            try {
                String content = projectFileService.getFileContent(projectId, cleanPath, userId).content();
                result.add(String.format(
                        "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---",
                        cleanPath, content
                ));
            } catch (Exception e) {
                log.warn("read_files failed for {}: {}", cleanPath, e.getMessage());
                result.add(String.format(
                        "--- ERROR: file '%s' does not exist. Only request paths listed in FILE_TREE. ---",
                        cleanPath
                ));
            }
        }

        return result;
    }
}

