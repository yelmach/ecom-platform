package ecom.media_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import io.minio.MinioClient;

class MediaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(MinioConfig.class)
            .withPropertyValues(
                    "media.minio.endpoint=http://localhost:9000",
                    "media.minio.access-key=test-access",
                    "media.minio.secret-key=test-secret",
                    "media.minio.bucket=ecom-media",
                    "media.public-base-url=http://localhost:8080/ecom-media");

    @Test
    void validSettingsBindAndCreateMinioClient() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MinioClient.class);
            MediaProperties properties = context.getBean(MediaProperties.class);
            assertThat(properties.getMinio().getBucket()).isEqualTo("ecom-media");
            assertThat(properties.getPublicBaseUrl()).isEqualTo("http://localhost:8080/ecom-media");
            assertThat(properties.getMaxFileSizeBytes()).isEqualTo(2L * 1024 * 1024);
        });
    }

    @Test
    void missingSettingsFailStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(MinioConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void blankSettingsAndNonpositiveSizeFailStartup() {
        for (String invalidSetting : new String[] {
                "media.minio.endpoint=", "media.minio.access-key=", "media.minio.secret-key=",
                "media.minio.bucket=", "media.public-base-url= ",
                "media.max-file-size-bytes=0", "media.max-file-size-bytes=-1" }) {
            contextRunner.withPropertyValues(invalidSetting).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
            });
        }
    }
}
