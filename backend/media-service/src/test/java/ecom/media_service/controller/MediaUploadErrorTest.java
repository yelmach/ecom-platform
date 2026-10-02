package ecom.media_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import ecom.media_service.exception.BadRequestException;
import ecom.media_service.exception.GlobalExceptionHandler;
import ecom.media_service.service.MediaService;

class MediaUploadErrorTest {
    @Test
    void uploadErrorsHaveExpectedHttpStatusesAndResponseBodies() throws Exception {
        MediaService service = mock(MediaService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new MediaController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (RuntimeException failure : new RuntimeException[] {
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product"),
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"),
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product unavailable"),
                new BadRequestException("Invalid image"), new IllegalStateException("Metadata save failed") }) {
            doThrow(failure).when(service).uploadProductImages(eq("seller-1"), eq("prod-1"), any());
            int expectedStatus = failure instanceof ResponseStatusException response ? response.getStatusCode().value()
                    : failure instanceof BadRequestException ? 400 : 500;
            mvc.perform(multipart("/media/images")
                    .file(new MockMultipartFile("files", "image.png", "image/png", new byte[] { 1 }))
                    .param("productId", "prod-1").header("X-User-Id", "seller-1"))
                    .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.status").value(expectedStatus));
        }
    }
}
