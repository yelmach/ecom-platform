package ecom.media_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ecom.media_service.dto.response.ProductImagesResponse;
import ecom.media_service.exception.GlobalExceptionHandler;
import ecom.media_service.service.MediaService;

@SpringBootTest(classes = MultipartLimitTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.location=classpath:application.yaml",
                "MEDIA_SERVICE_PORT=0", "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
        })
class MultipartLimitTest {
    @LocalServerPort
    private int port;
    @MockitoBean
    private MediaService service;

    @Test
    void fiveFilesAtIndividualLimitFitAndOversizedFileIsRejected() throws Exception {
        when(service.uploadProductImages(eq("seller-1"), eq("product-1"), any()))
                .thenAnswer(invocation -> {
                    // Inspect file sizes before the servlet deletes its temporary multipart files.
                    org.springframework.web.multipart.MultipartFile[] files = invocation.getArgument(2);
                    assertEquals(5, files.length);
                    for (var file : files) {
                        assertEquals(2 * 1024 * 1024, file.getSize());
                    }
                    return new ProductImagesResponse("product-1", List.of());
                });
        assertEquals(201, sendMultipart(5, 2 * 1024 * 1024));
        verify(service).uploadProductImages(eq("seller-1"), eq("product-1"), any());
        // Service remains mocked: this test verifies servlet multipart limits, not image contents.
        clearInvocations(service);
        assertEquals(400, sendMultipart(1, 2 * 1024 * 1024 + 1));
        verifyNoInteractions(service);
    }

    private int sendMultipart(int count, int size) throws Exception {
        String boundary = "media-limit-test";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"productId\"\r\n\r\nproduct-1\r\n")
                .getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < count; i++) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"image.png\"\r\nContent-Type: image/png\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(new byte[size]);
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/media/images"))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-User-Id", "seller-1")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({MediaController.class, GlobalExceptionHandler.class})
    static class TestApplication {}
}
