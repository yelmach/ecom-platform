package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.dto.response.ProductImagesResponse;
import ecom.media_service.dto.response.ProfileImageResponse;
import ecom.media_service.exception.BadRequestException;
import ecom.media_service.exception.MediaNotFoundException;
import ecom.media_service.model.Media;
import ecom.media_service.model.OwnerType;
import ecom.media_service.repository.MediaRepository;
import ecom.media_service.storage.ObjectStorageService;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private MediaUrlBuilder mediaUrlBuilder;

    @InjectMocks
    private MediaService mediaService;

    @Mock
    private MultipartFile file;

    @Test
    void uploadProductImages_ShouldThrowWhenInputInvalid() {
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("", "prod-1", new MultipartFile[] { file }));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", " ", new MultipartFile[] { file }));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", null));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[0]));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file, file, file, file, file, file }));
    }

    @Test
    void uploadProductImages_ShouldUploadAndPersist() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/png", ".png");
        when(fileValidationService.validateImage(file)).thenReturn(validatedFile);
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("m1");
            return media;
        });

        ProductImagesResponse response = mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file });

        assertEquals("prod-1", response.productId());
        assertEquals(1, response.images().size());
        assertEquals("m1", response.images().get(0).id());
        verify(objectStorageService).upload(anyString(), any(byte[].class), eq("image/png"));

        ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(captor.capture());
        assertEquals(OwnerType.PRODUCT, captor.getValue().getOwnerType());
        assertEquals("prod-1", captor.getValue().getOwnerId());
    }

    @Test
    void getProductImages_ShouldReturnMappedImages() {
        Media first = Media.builder().id("m1").ownerId("prod-1").ownerType(OwnerType.PRODUCT).objectKey("products/prod-1/a.png").build();
        Media second = Media.builder().id("m2").ownerId("prod-1").ownerType(OwnerType.PRODUCT).objectKey("products/prod-1/b.png").build();
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));
        when(mediaRepository.findByOwnerTypeAndOwnerIdOrderByIdAsc(OwnerType.PRODUCT, "prod-1")).thenReturn(List.of(first, second));

        ProductImagesResponse response = mediaService.getProductImages("prod-1");

        assertEquals("prod-1", response.productId());
        assertEquals(2, response.images().size());
        assertEquals("m1", response.images().get(0).id());
    }

    @Test
    void uploadProfileImage_ShouldReplaceExistingAvatar() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/jpeg", ".jpg");
        when(fileValidationService.validateImage(file)).thenReturn(validatedFile);
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));

        Media oldAvatar = Media.builder().id("old").ownerId("user-1").ownerType(OwnerType.AVATAR).objectKey("avatars/user-1/old.jpg").build();
        when(mediaRepository.findByOwnerTypeAndOwnerId(OwnerType.AVATAR, "user-1")).thenReturn(List.of(oldAvatar));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("new");
            return media;
        });

        ProfileImageResponse response = mediaService.uploadProfileImage("user-1", file);

        assertEquals("new", response.avatar().id());
        verify(objectStorageService).delete("avatars/user-1/old.jpg");
        verify(mediaRepository).deleteAll(List.of(oldAvatar));
        verify(objectStorageService).upload(anyString(), any(byte[].class), eq("image/jpeg"));
    }

    @Test
    void uploadProfileImage_ShouldNotDeleteWhenNoExistingAvatar() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/jpeg", ".jpg");
        when(fileValidationService.validateImage(file)).thenReturn(validatedFile);
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));
        when(mediaRepository.findByOwnerTypeAndOwnerId(OwnerType.AVATAR, "user-1")).thenReturn(List.of());
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("new");
            return media;
        });

        mediaService.uploadProfileImage("user-1", file);

        verify(mediaRepository, never()).deleteAll(any());
    }

    @Test
    void getProfileImage_ShouldThrowWhenMissing() {
        when(mediaRepository.findFirstByOwnerTypeAndOwnerIdOrderByIdDesc(OwnerType.AVATAR, "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(MediaNotFoundException.class, () -> mediaService.getProfileImage("user-1"));
    }

    @Test
    void getProfileImage_ShouldReturnMappedAvatar() {
        Media avatar = Media.builder().id("av1").ownerId("user-1").ownerType(OwnerType.AVATAR).objectKey("avatars/user-1/av1.jpg").build();
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));
        when(mediaRepository.findFirstByOwnerTypeAndOwnerIdOrderByIdDesc(OwnerType.AVATAR, "user-1"))
                .thenReturn(Optional.of(avatar));

        ProfileImageResponse response = mediaService.getProfileImage("user-1");

        assertEquals("av1", response.avatar().id());
    }
}
