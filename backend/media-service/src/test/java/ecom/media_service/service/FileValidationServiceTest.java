package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.config.MediaProperties;
import ecom.media_service.exception.BadRequestException;

class FileValidationServiceTest {
    private MediaProperties properties;
    private FileValidationService validator;

    @BeforeEach
    void setUp() {
        properties = new MediaProperties();
        validator = new FileValidationService(properties);
    }

    @Test
    void detectsRealJpegAndPngAndPreservesBytes() throws IOException {
        for (String format : new String[] { "jpeg", "png" }) {
            byte[] bytes = imageBytes(format);
            ValidatedFile result = validator.validateImage(file("image/" + format, bytes));
            assertArrayEquals(bytes, result.bytes());
            assertEquals("image/" + format, result.contentType());
            assertEquals(format.equals("jpeg") ? ".jpg" : "." + format, result.extension());
        }
    }

    @Test
    void usesContentsInsteadOfDeclaredMimeTypeAndFilename() throws IOException {
        for (String declaredType : new String[] { null, "", "text/plain", "image/jpeg" }) {
            byte[] bytes = imageBytes("png");
            MockMultipartFile misleadingFile = new MockMultipartFile("file", "image.jpg", declaredType, bytes);
            ValidatedFile result = validator.validateImage(misleadingFile);
            assertEquals("image/png", result.contentType());
            assertEquals(".png", result.extension());
            assertArrayEquals(bytes, result.bytes());
        }
    }

    @Test
    void rejectsTextLabelledAsAnImage() {
        assertThrows(BadRequestException.class,
                () -> validator.validateImage(file("image/jpeg", "not an image".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsTruncatedImagesEvenWhenTikaRecognizesTheirType() throws IOException {
        for (String format : new String[] { "jpeg", "png" }) {
            byte[] truncated = Arrays.copyOf(imageBytes(format), 32);
            assertEquals("image/" + format, new org.apache.tika.Tika().detect(truncated));
            BadRequestException failure = assertThrows(BadRequestException.class,
                    () -> validator.validateImage(file("image/" + format, truncated)));
            assertEquals("File is not a valid image", failure.getMessage());
        }
    }

    @Test
    void rejectsGifEvenWhenLabelledAsPng() throws IOException {
        byte[] gif = imageBytes("gif");
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/gif", gif)));
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/png", gif)));
    }

    @Test
    void rejectsMissingEmptyAndUnsupportedFiles() {
        assertThrows(BadRequestException.class, () -> validator.validateImage(null));
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/png", new byte[0])));
        for (String type : new String[] { null, "", "text/plain", "image/bmp", "image/webp" }) {
            assertThrows(BadRequestException.class, () -> validator.validateImage(file(type, new byte[] { 1 })));
        }
    }

    @Test
    void rejectsWebpBytesEvenWhenDeclaredAsPng() {
        byte[] webp = java.util.Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/webp", webp)));
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/png", webp)));
    }

    @Test
    void checksBothReportedAndActualSize() throws IOException {
        byte[] large = new byte[(int) properties.getMaxFileSizeBytes() + 1];
        assertThrows(BadRequestException.class, () -> validator.validateImage(file("image/png", large)));
        MultipartFile lyingFile = mock(MultipartFile.class);
        when(lyingFile.getSize()).thenReturn(1L);
        when(lyingFile.getBytes()).thenReturn(large);
        assertThrows(BadRequestException.class, () -> validator.validateImage(lyingFile));
    }

    @Test
    void rejectsEmptyBytesEvenIfMultipartClaimsFileIsNonempty() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[0]);
        assertThrows(BadRequestException.class, () -> validator.validateImage(file));
    }

    @Test
    void readFailureBecomesBadRequest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("failed read"));
        assertThrows(BadRequestException.class, () -> validator.validateImage(file));
    }

    private MockMultipartFile file(String type, byte[] bytes) {
        return new MockMultipartFile("file", "ignored-filename.bin", type, bytes);
    }

    private byte[] imageBytes(String format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }
}
