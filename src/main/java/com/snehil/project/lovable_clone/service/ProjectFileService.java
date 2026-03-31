package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.project.FileContentResponse;
import com.snehil.project.lovable_clone.dto.project.FileNode;

import java.util.List;

public interface ProjectFileService {
    List<FileNode> getFileTree(Long projectId);

    FileContentResponse getFileContent(Long projectId, String path, Long userId);

    void saveFile(Long projectId, String filePath, String fileContent);
}
