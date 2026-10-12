package ecom.media_service.service;

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

}
