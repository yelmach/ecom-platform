package ecom.media_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ecom.media_service.model.OwnerType;
import ecom.media_service.repository.MediaRepository;
import ecom.media_service.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaCleanupService {

    private final MediaRepository mediaRepository;
    private final ObjectStorageService objectStorageService;

    public void handleProductDeleted(String productId) {
        for (var media : mediaRepository.findByOwnerTypeAndOwnerId(OwnerType.PRODUCT, productId)) {
            objectStorageService.delete(media.getObjectKey());
            mediaRepository.deleteById(media.getId());
        }
        objectStorageService.deleteByPrefix("products/" + productId + "/");
    }

    public void handleProductImagesDetached(String productId, List<String> mediaIds) {
        for (String mediaId : mediaIds) {
            var media = mediaRepository.findById(mediaId).orElse(null);
            if (media == null || media.getOwnerType() != OwnerType.PRODUCT
                    || !productId.equals(media.getOwnerId())) {
                continue;
            }
            objectStorageService.delete(media.getObjectKey());
            mediaRepository.deleteById(media.getId());
        }
    }

    public void handleUserAvatarChanged(String userId, String previousMediaId) {
        var media = mediaRepository.findById(previousMediaId).orElse(null);
        if (media == null || media.getOwnerType() != OwnerType.AVATAR
                || !userId.equals(media.getOwnerId())) {
            return;
        }
        objectStorageService.delete(media.getObjectKey());
        mediaRepository.deleteById(media.getId());
    }
}
