package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import ecom.media_service.model.*;
import ecom.media_service.repository.*;
import ecom.media_service.storage.ObjectStorageService;

class MediaCleanupServiceTest {
    private final MediaRepository media = mock(MediaRepository.class);
    private final ObjectStorageService storage = mock(ObjectStorageService.class);
    private final MediaCleanupService service = new MediaCleanupService(media, storage);
    private final Media image = Media.builder().id("m").ownerType(OwnerType.PRODUCT)
            .ownerId("p").objectKey("products/p/m.png").build();

    @Test
    void deletesObjectBeforeMetadata() {
        when(media.findByOwnerTypeAndOwnerId(OwnerType.PRODUCT, "p")).thenReturn(List.of(image));
        service.handleProductDeleted("p");
        var order = inOrder(storage, media);
        order.verify(media).findByOwnerTypeAndOwnerId(OwnerType.PRODUCT, "p");
        order.verify(storage).delete(image.getObjectKey());
        order.verify(media).deleteById("m");
        order.verify(storage).deleteByPrefix("products/p/");
    }

    @Test
    void storageFailureKeepsMetadataForEventRetry() {
        when(media.findByOwnerTypeAndOwnerId(OwnerType.PRODUCT, "p")).thenReturn(List.of(image));
        doThrow(new IllegalStateException("storage offline")).doNothing().when(storage).delete(image.getObjectKey());
        assertThrows(IllegalStateException.class, () -> service.handleProductDeleted("p"));
        verify(media, never()).deleteById("m");
        service.handleProductDeleted("p");
        verify(media).deleteById("m");
    }

    @Test
    void duplicateEventWithNoImagesSucceeds() {
        when(media.findByOwnerTypeAndOwnerId(OwnerType.PRODUCT, "p")).thenReturn(List.of());
        service.handleProductDeleted("p");
        service.handleProductDeleted("p");
        verify(storage, times(2)).deleteByPrefix("products/p/");
        verify(storage, never()).delete(anyString());
    }

}
