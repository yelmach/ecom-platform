package ecom.media_service.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.config.MediaProperties;
import ecom.media_service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    private final MediaProperties mediaProperties;

    public ValidatedFile validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > mediaProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("image must be <= 2 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Image content type is required");
        }
        contentType = contentType.trim().toLowerCase(Locale.ROOT);

        if (!isImageContentType(contentType)) {
            throw new BadRequestException("Only image files are allowed");
        }

        byte[] bytes = readBytes(file);
        if (bytes.length == 0) {
            throw new BadRequestException("Image file is empty");
        }

        String extension = resolveExtension(contentType);

        return new ValidatedFile(bytes, contentType, extension);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded file");
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private String resolveExtension(String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension != null) {
            return extension;
        }
        throw new BadRequestException("Unsupported image type: " + contentType);
    }
}
