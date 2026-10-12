package ecom.media_service.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ecom.media_service.model.Media;
import ecom.media_service.model.OwnerType;
import ecom.media_service.repository.MediaRepository;
import ecom.media_service.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaStorageService.class);

    private final ObjectStorageService objectStorageService;
    private final MediaRepository mediaRepository;

    public Media save(OwnerType ownerType, String ownerId, ValidatedFile file) {
        String folder = switch (ownerType) {
            case PRODUCT -> "products";
            case AVATAR -> "avatars";
        };
        String objectKey = folder + "/" + ownerId + "/" + UUID.randomUUID() + file.extension();
        Media metadata = Media.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .objectKey(objectKey)
                .contentType(file.contentType())
                .build();

        try {
            objectStorageService.upload(objectKey, file.bytes(), file.contentType());
            return mediaRepository.save(metadata);
        } catch (RuntimeException originalFailure) {
            cleanup(objectKey, originalFailure);
            LOGGER.error("Failed to upload or save media for object {}", objectKey, originalFailure);
            throw originalFailure;
        }
    }

    private void cleanup(String objectKey, RuntimeException originalFailure) {
        try {
            objectStorageService.delete(objectKey);
        } catch (RuntimeException cleanupFailure) {
            LOGGER.error("Failed to clean up uploaded object {}", objectKey, cleanupFailure);
            if (cleanupFailure != originalFailure) {
                originalFailure.addSuppressed(cleanupFailure);
            }
        }
    }
}
