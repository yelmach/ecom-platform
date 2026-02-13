package ecom.media_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "media")
@Getter
@Setter
public class MediaProperties {

    private final Minio minio = new Minio();
    private String publicBaseUrl;
    private long maxFileSizeBytes = 2L * 1024L * 1024L;

    @Getter
    @Setter
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
