package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.project.FileContentResponse;
import com.snehil.project.lovable_clone.dto.project.FileNode;
import com.snehil.project.lovable_clone.entity.Project;
import com.snehil.project.lovable_clone.entity.ProjectFile;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.mapper.ProjectFileMapper;
import com.snehil.project.lovable_clone.repository.ProjectFileRepository;
import com.snehil.project.lovable_clone.repository.ProjectRepository;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final MinioClient minioClient;
    private final ProjectFileMapper projectFileMapper;

    @Value("${minio.project-bucket}")
    private String projectBucket;

    private static final String BUCKET_NAME = "projects";


    @Override
    public List<FileNode> getFileTree(Long projectId) {
        List<ProjectFile> projectFilesList = projectFileRepository.findByProjectId(projectId);
        return projectFileMapper.toListOfFileNode(projectFilesList);
    }

    @Override
    public FileContentResponse getFileContent(Long projectId, String path, Long userId) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        ProjectFile file = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                .orElseThrow(() -> new ResourceNotFoundException("File", cleanPath));

        if (file.getContent() != null && !file.getContent().isEmpty()) {
            return new FileContentResponse(cleanPath, file.getContent());
        }

        String key = file.getMinioObjectKey();
        if (key != null && !key.isEmpty()) {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(key)
                            .build())) {
                String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return new FileContentResponse(cleanPath, body);
            } catch (Exception e) {
                log.error("MinIO read failed for project key={}", key, e);
                throw new ResourceNotFoundException("File content", cleanPath);
            }
        }

        throw new ResourceNotFoundException("File content", cleanPath);
    }

    @Override
    public void saveFile(Long projectId, String path, String content) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString())
        );

        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String objectKey = projectId + "/" + cleanPath;

        boolean uploadedToMinio = false;
        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(projectBucket)
                                .object(objectKey)
                                .stream(inputStream, contentBytes.length, -1)
                                .contentType(determineContentType(path))
                                .build());
            }
            uploadedToMinio = true;
        } catch (Exception e) {
            log.warn(
                    "MinIO upload failed for {}/{} — storing content in database. Check minio.url / that MinIO is running. Cause: {}",
                    projectId,
                    cleanPath,
                    e.getMessage());
        }

        ProjectFile file = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                .orElseGet(() -> ProjectFile.builder()
                        .project(project)
                        .path(cleanPath)
                        .createdAt(Instant.now())
                        .build());

        if (uploadedToMinio) {
            file.setMinioObjectKey(objectKey);
            file.setContent(null);
            log.info("Saved file to MinIO: {}", objectKey);
        } else {
            file.setMinioObjectKey(null);
            file.setContent(content);
            log.info("Saved file in database only: projectId={} path={}", projectId, cleanPath);
        }

        file.setUpdatedAt(Instant.now());
        projectFileRepository.save(file);
    }
    private String determineContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        if (type != null) return type;
        if (path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx")) return "text/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".css")) return "text/css";

        return "text/plain";
    }
}
