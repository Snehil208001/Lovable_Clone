package com.snehil.project.lovable_clone.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class StorageConfig {

    private String url;
    private String accessKey;
    private String secretKey;

    /**
     * Region used for SigV4 signing. MinIO works with {@code us-east-1}; leave empty to skip (some setups).
     */
    private String region = "us-east-1";

    @Bean
    public MinioClient minioClient() {
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("minio.url must use http:// or https://");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("minio.url must include a host, e.g. http://127.0.0.1:9000");
        }
        boolean secure = "https".equalsIgnoreCase(scheme);
        int port = uri.getPort();
        if (port < 0) {
            port = secure ? 443 : 9000;
        }

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(host, port, secure)
                .credentials(accessKey, secretKey);
        if (region != null && !region.isBlank()) {
            builder.region(region);
        }
        return builder.build();
    }
}

