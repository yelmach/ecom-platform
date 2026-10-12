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

    @Test
    void detachedCleanupDeletesOnlyListedImagesOwnedByProduct() {
        when(media.findById("m")).thenReturn(java.util.Optional.of(image));
        when(media.findById("avatar")).thenReturn(java.util.Optional.of(
                Media.builder().id("avatar").ownerType(OwnerType.AVATAR).ownerId("p").build()));
        when(media.findById("other")).thenReturn(java.util.Optional.of(
                Media.builder().id("other").ownerType(OwnerType.PRODUCT).ownerId("other-product").build()));
        when(media.findById("missing")).thenReturn(java.util.Optional.empty());
        service.handleProductImagesDetached("p", List.of("m", "avatar", "other", "missing"));
        var order = inOrder(storage, media);
        order.verify(storage).delete(image.getObjectKey());
        order.verify(media).deleteById("m");
        verify(media, never()).deleteById("avatar");
        verify(media, never()).deleteById("other");
        verify(storage, never()).deleteByPrefix(anyString());
    }

    @Test
    void detachedStorageFailureKeepsMetadataAndCanBeRetried() {
        when(media.findById("m")).thenReturn(java.util.Optional.of(image));
        doThrow(new IllegalStateException("offline")).doNothing().when(storage).delete(image.getObjectKey());
        assertThrows(IllegalStateException.class, () -> service.handleProductImagesDetached("p", List.of("m")));
        verify(media, never()).deleteById("m");
        service.handleProductImagesDetached("p", List.of("m"));
        verify(media).deleteById("m");
    }


    @Test
    void avatarCleanupDeletesOwnedAvatarBeforeMetadata() {
        var avatar = Media.builder().id("old").ownerType(OwnerType.AVATAR).ownerId("u")
                .objectKey("avatars/u/old.png").build();
        when(media.findById("old")).thenReturn(java.util.Optional.of(avatar));
        doThrow(new IllegalStateException("offline")).doNothing().when(storage).delete("avatars/u/old.png");
        assertThrows(IllegalStateException.class, () -> service.handleUserAvatarChanged("u", "old"));
        verify(media, never()).deleteById("old");
        service.handleUserAvatarChanged("u", "old");
        var order = inOrder(storage, media);
        order.verify(storage, times(2)).delete("avatars/u/old.png");
        order.verify(media).deleteById("old");
        when(media.findById("old")).thenReturn(java.util.Optional.empty());
        service.handleUserAvatarChanged("u", "old");
        verify(media).deleteById("old");
        verify(storage, never()).deleteByPrefix(anyString());
    }

    @Test
    void avatarCleanupSkipsWrongOwnerAndProductImages() {
        when(media.findById("other")).thenReturn(java.util.Optional.of(
                Media.builder().id("other").ownerType(OwnerType.AVATAR).ownerId("other-user").build()));
        when(media.findById("m")).thenReturn(java.util.Optional.of(image));
        service.handleUserAvatarChanged("u", "other");
        service.handleUserAvatarChanged("u", "m");
        verifyNoInteractions(storage);
        verify(media, never()).deleteById(anyString());
    }
}
