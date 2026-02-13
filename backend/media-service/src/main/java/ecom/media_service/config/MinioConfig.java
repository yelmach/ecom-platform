package ecom.media_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MediaProperties properties) {
        MediaProperties.Minio minio = properties.getMinio();

        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
