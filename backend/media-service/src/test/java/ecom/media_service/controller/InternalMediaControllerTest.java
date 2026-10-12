package ecom.media_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ecom.media_service.model.Media;
import ecom.media_service.model.OwnerType;
import ecom.media_service.repository.MediaRepository;
import ecom.media_service.storage.ObjectStorageService;
import ecom.media_service.service.*;
import ecom.media_service.exception.GlobalExceptionHandler;

class InternalMediaControllerTest {
    private final MediaRepository repository = mock(MediaRepository.class);
    private final ObjectStorageService storage = mock(ObjectStorageService.class);
    private final MediaService service = new MediaService(repository, new MediaStorageService(storage, repository),
            mock(FileValidationService.class), mock(MediaUrlBuilder.class), mock(ProductOwnershipService.class));
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new InternalMediaController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void ownershipChecksIdentityOwnerTypeAndExistenceWithoutStorageWrites() throws Exception {
        Media avatar = Media.builder().id("new").ownerId("user-1").ownerType(OwnerType.AVATAR).build();
        when(repository.findById("new")).thenReturn(Optional.of(avatar));
        when(repository.findById("missing")).thenReturn(Optional.empty());
        mvc.perform(get("/internal/media/avatars/new/ownership").header("X-User-Id", "user-1")).andExpect(status().isNoContent());
        mvc.perform(get("/internal/media/avatars/new/ownership").header("X-User-Id", "other")).andExpect(status().isForbidden());
        mvc.perform(get("/internal/media/avatars/missing/ownership").header("X-User-Id", "user-1")).andExpect(status().isNotFound());
        mvc.perform(get("/internal/media/avatars/new/ownership")).andExpect(status().isBadRequest());
        mvc.perform(get("/internal/media/avatars/new/ownership").header("X-User-Id", " ")).andExpect(status().isBadRequest());
        avatar.setOwnerType(OwnerType.PRODUCT);
        mvc.perform(get("/internal/media/avatars/new/ownership").header("X-User-Id", "user-1")).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }

}
