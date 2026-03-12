package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.config.MediaProperties;
import ecom.media_service.exception.BadRequestException;

class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        MediaProperties mediaProperties = new MediaProperties();
        mediaProperties.setMaxFileSizeBytes(2L * 1024L * 1024L);
        fileValidationService = new FileValidationService(mediaProperties);
    }

    @Test
    void validateImage_ShouldReturnValidatedFileForSupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8));

        ValidatedFile validatedFile = fileValidationService.validateImage(file);

        assertEquals("image/jpeg", validatedFile.contentType());
        assertEquals(".jpg", validatedFile.extension());
        assertEquals("image-bytes", new String(validatedFile.bytes(), StandardCharsets.UTF_8));
    }

    @Test
    void validateImage_ShouldThrowWhenFileMissingOrEmpty() {
        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(null));

        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(empty));
    }

    @Test
    void validateImage_ShouldThrowWhenFileTooLarge() {
        byte[] large = new byte[(int) (2L * 1024L * 1024L + 1L)];
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", large);

        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(file));
    }

    @Test
    void validateImage_ShouldThrowWhenContentTypeInvalid() {
        MockMultipartFile noContentType = new MockMultipartFile("file", "a.jpg", null, "x".getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(noContentType));

        MockMultipartFile notImage = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(notImage));

        MockMultipartFile unsupportedImage = new MockMultipartFile("file", "a.bmp", "image/bmp", "x".getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(unsupportedImage));
    }

    @Test
    void validateImage_ShouldThrowWhenReadFails() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(3L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThrows(BadRequestException.class, () -> fileValidationService.validateImage(file));
    }
}
