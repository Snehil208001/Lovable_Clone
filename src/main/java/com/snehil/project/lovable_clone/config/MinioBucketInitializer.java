package com.snehil.project.lovable_clone.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the configured project bucket exists. If MinIO is unreachable, logs guidance;
 * {@link com.snehil.project.lovable_clone.service.impl.ProjectFileServiceImpl} still stores files in Postgres.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;

    @Value("${minio.project-bucket}")
    private String projectBucket;

    @Value("${minio.url}")
    private String minioUrl;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(projectBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(projectBucket).build());
                log.info("MinIO: created bucket \"{}\"", projectBucket);
            }
        } catch (Exception e) {
            log.warn(
                    "MinIO: cannot reach server or create bucket \"{}\" at {}. "
                            + "File uploads will fall back to the database until this is fixed. "
                            + "Use the S3 API port (container 9000), not the web console port (often 9001). "
                            + "If Docker maps the API to another host port, set minio.url to that (e.g. http://localhost:9002). "
                            + "Cause: {}",
                    projectBucket,
                    minioUrl,
                    e.getMessage());
        }
    }
}
