package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import ecom.media_service.dto.response.ProductImagesResponse;
import ecom.media_service.config.MediaProperties;
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

    @Spy
    private FileValidationService fileValidationService = new FileValidationService(new MediaProperties());

    @Mock
    private MediaUrlBuilder mediaUrlBuilder;

    @Mock
    private ProductOwnershipService productOwnershipService;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        MediaStorageService storage = new MediaStorageService(objectStorageService, mediaRepository);
        mediaService = new MediaService(mediaRepository, storage, fileValidationService,
                mediaUrlBuilder, productOwnershipService);
    }

    @Mock
    private MultipartFile file;

    @Test
    void uploadProductImages_ShouldThrowWhenInputInvalid() {
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("", "prod-1", new MultipartFile[] { file }));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", " ", new MultipartFile[] { file }));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", null));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[0]));
        assertThrows(BadRequestException.class, () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file, file, file, file, file, file }));
        verifyNoInteractions(productOwnershipService, fileValidationService, objectStorageService, mediaRepository);
    }

    @Test
    void uploadProductImages_ShouldUploadAndPersist() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/png", ".png");
        doReturn(validatedFile).when(fileValidationService).validateImage(file);
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
    void uploadProfileImage_ShouldRetainExistingAvatarUntilProfileIsLinked() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/jpeg", ".jpg");
        doReturn(validatedFile).when(fileValidationService).validateImage(file);
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("new");
            return media;
        });

        ProfileImageResponse response = mediaService.uploadProfileImage("user-1", file);

        assertEquals("new", response.avatar().id());
        verify(objectStorageService, never()).delete("avatars/user-1/old.jpg");
        verify(mediaRepository, never()).deleteAll(any());
        verify(objectStorageService).upload(anyString(), any(byte[].class), eq("image/jpeg"));
    }

    @Test
    void uploadProfileImage_ShouldNotDeleteWhenNoExistingAvatar() {
        ValidatedFile validatedFile = new ValidatedFile("abc".getBytes(), "image/jpeg", ".jpg");
        doReturn(validatedFile).when(fileValidationService).validateImage(file);
        when(mediaUrlBuilder.build(anyString())).thenAnswer(invocation -> "http://cdn/" + invocation.getArgument(0));
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

    @Test
    void ownershipRejectionDoesNotValidateOrStoreFiles() {
        for (HttpStatus status : new HttpStatus[] { HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND,
                HttpStatus.BAD_REQUEST, HttpStatus.SERVICE_UNAVAILABLE }) {
            ResponseStatusException failure = new ResponseStatusException(status);
            doThrow(failure).when(productOwnershipService).verifyOwnership("prod-1", "user-1");
            assertSame(failure, assertThrows(ResponseStatusException.class,
                    () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file })));
        }
        verifyNoInteractions(fileValidationService, objectStorageService, mediaRepository);
    }

    @Test
    void invalidLaterFileLeavesEntireBatchUnstored() {
        MultipartFile invalid = org.mockito.Mockito.mock(MultipartFile.class);
        doReturn(new ValidatedFile(new byte[] { 1 }, "image/png", ".png")).when(fileValidationService).validateImage(file);
        doThrow(new BadRequestException("invalid image")).when(fileValidationService).validateImage(invalid);
        assertThrows(BadRequestException.class,
                () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file, invalid }));
        verify(productOwnershipService).verifyOwnership("prod-1", "user-1");
        verifyNoInteractions(objectStorageService, mediaRepository);
    }

    @Test
    void checksOwnershipAndAllFilesBeforeFirstUpload() {
        MultipartFile second = org.mockito.Mockito.mock(MultipartFile.class);
        ValidatedFile valid = new ValidatedFile(new byte[] { 1 }, "image/png", ".png");
        doReturn(valid).when(fileValidationService).validateImage(file);
        doReturn(valid).when(fileValidationService).validateImage(second);
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file, second });
        var order = inOrder(productOwnershipService, fileValidationService, objectStorageService);
        order.verify(productOwnershipService).verifyOwnership("prod-1", "user-1");
        order.verify(fileValidationService).validateImage(file);
        order.verify(fileValidationService).validateImage(second);
        order.verify(objectStorageService, times(2)).upload(anyString(), any(byte[].class), eq("image/png"));
    }

    @Test
    void laterMetadataFailureCleansOnlyCurrentObjectAndStopsBatch() {
        doReturn(new ValidatedFile(new byte[] { 1 }, "image/png", ".png")).when(fileValidationService).validateImage(file);
        RuntimeException failure = new IllegalStateException("Mongo unavailable");
        when(mediaRepository.save(any(Media.class))).thenReturn(Media.builder().id("first").objectKey("first-key").build())
                .thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file, file, file })));
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService, times(2)).upload(keys.capture(), any(byte[].class), eq("image/png"));
        verify(objectStorageService).delete(keys.getAllValues().get(1));
        verify(objectStorageService, never()).delete(keys.getAllValues().get(0));
        verify(mediaRepository, times(2)).save(any(Media.class));
    }

    @Test
    void uploadFailureAttemptsCleanupAndDoesNotSaveMetadata() {
        doReturn(new ValidatedFile(new byte[] { 1 }, "image/png", ".png")).when(fileValidationService).validateImage(file);
        RuntimeException failure = new IllegalStateException("MinIO unavailable");
        doThrow(failure).when(objectStorageService).upload(anyString(), any(byte[].class), anyString());
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file })));
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).upload(key.capture(), any(byte[].class), anyString());
        verify(objectStorageService).delete(key.getValue());
        verifyNoInteractions(mediaRepository);
    }

    @Test
    void avatarMetadataFailureUsesSameCleanup() {
        doReturn(new ValidatedFile(new byte[] { 1 }, "image/png", ".png")).when(fileValidationService).validateImage(file);
        RuntimeException failure = new IllegalStateException("Mongo unavailable");
        when(mediaRepository.save(any(Media.class))).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, () -> mediaService.uploadProfileImage("user-1", file)));
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).upload(key.capture(), any(byte[].class), anyString());
        verify(objectStorageService).delete(key.getValue());
        verifyNoInteractions(productOwnershipService);
    }

    @Test
    void cleanupFailureIsLoggedAndOriginalFailureIsPreserved() {
        doReturn(new ValidatedFile(new byte[] { 1 }, "image/png", ".png")).when(fileValidationService).validateImage(file);
        RuntimeException original = new IllegalStateException("save failed");
        RuntimeException cleanup = new IllegalStateException("delete failed");
        when(mediaRepository.save(any(Media.class))).thenThrow(original);
        doThrow(cleanup).when(objectStorageService).delete(anyString());
        Logger logger = (Logger) LoggerFactory.getLogger(MediaStorageService.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            assertSame(original, assertThrows(RuntimeException.class,
                    () -> mediaService.uploadProductImages("user-1", "prod-1", new MultipartFile[] { file })));
            assertSame(cleanup, original.getSuppressed()[0]);
            ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
            verify(objectStorageService).delete(key.capture());
            assertTrue(logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains(key.getValue())
                    && event.getFormattedMessage().contains("clean up") && event.getThrowableProxy() != null));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
