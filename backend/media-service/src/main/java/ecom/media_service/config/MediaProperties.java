package ecom.media_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "media")
@Validated
@Getter
@Setter
public class MediaProperties {

    @Valid
    private final Minio minio = new Minio();
    @NotBlank
    private String publicBaseUrl;
    @Positive
    private long maxFileSizeBytes = 2L * 1024L * 1024L;

    @Getter
    @Setter
    public static class Minio {
        @NotBlank
        private String endpoint;
        @NotBlank
        private String accessKey;
        @NotBlank
        private String secretKey;
        @NotBlank
        private String bucket;
    }
}
