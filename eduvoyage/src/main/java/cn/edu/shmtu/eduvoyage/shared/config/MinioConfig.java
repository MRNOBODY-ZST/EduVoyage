package cn.edu.shmtu.eduvoyage.shared.config;

import io.minio.MinioAsyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a {@link MinioAsyncClient}. The async client is {@code CompletableFuture}
 * based, which bridges cleanly into Reactor via {@code Mono.fromFuture} without
 * blocking event-loop threads (unlike the synchronous {@code MinioClient}).
 */
@Configuration(proxyBeanMethods = false)
public class MinioConfig {

    @Bean
    public MinioAsyncClient minioAsyncClient(EduVoyageProperties properties) {
        EduVoyageProperties.Minio minio = properties.storage().minio();
        return MinioAsyncClient.builder()
                .endpoint(minio.endpoint())
                .credentials(minio.accessKey(), minio.secretKey())
                .build();
    }
}
