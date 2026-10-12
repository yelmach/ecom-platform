package ecom.media_service.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.config.MediaProperties;
import ecom.media_service.exception.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final MediaProperties mediaProperties;
    private final Tika tika = new Tika();

    public List<ValidatedFile> validateImages(MultipartFile[] files) {
        List<ValidatedFile> validatedFiles = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            validatedFiles.add(validateImage(file));
        }
        return List.copyOf(validatedFiles);
    }

    public ValidatedFile validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > mediaProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("image must be <= 2 MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded file");
        }
        if (bytes.length == 0) {
            throw new BadRequestException("Image file is empty");
        }
        if (bytes.length > mediaProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("image must be <= 2 MB");
        }

        String contentType = tika.detect(bytes);
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new BadRequestException("Only JPEG and PNG images are allowed");
        };

        BufferedImage image;
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            image = ImageIO.read(input);
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("File is not a valid image");
        }
        if (image == null) {
            throw new BadRequestException("File is not a valid image");
        }
        image.flush();
        return new ValidatedFile(bytes, contentType, extension);
    }
}
