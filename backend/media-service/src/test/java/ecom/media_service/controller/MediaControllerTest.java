package ecom.media_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ecom.media_service.dto.response.MediaImageResponse;
import ecom.media_service.dto.response.ProductImagesResponse;
import ecom.media_service.dto.response.ProfileImageResponse;
import ecom.media_service.service.MediaService;

@WebMvcTest(controllers = MediaController.class)
@ContextConfiguration(classes = MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void uploadProductImages_ShouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "img.png",
                "image/png",
                "img".getBytes(StandardCharsets.UTF_8));

        ProductImagesResponse response = new ProductImagesResponse(
                "prod-1",
                List.of(new MediaImageResponse("m1", "http://cdn/products/prod-1/a.png")));

        when(mediaService.uploadProductImages(eq("user-1"), eq("prod-1"), any())).thenReturn(response);

        mockMvc.perform(multipart("/media/images")
                .file(file)
                .param("productId", "prod-1")
                .header("X-User-Id", "user-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("prod-1"))
                .andExpect(jsonPath("$.images[0].id").value("m1"));
    }

    @Test
    void getProductImages_ShouldReturn200() throws Exception {
        ProductImagesResponse response = new ProductImagesResponse(
                "prod-1",
                List.of(new MediaImageResponse("m1", "http://cdn/products/prod-1/a.png")));

        when(mediaService.getProductImages("prod-1")).thenReturn(response);

        mockMvc.perform(get("/media/images/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-1"));
    }

    @Test
    void uploadProfileImage_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "img".getBytes(StandardCharsets.UTF_8));

        ProfileImageResponse response = new ProfileImageResponse(new MediaImageResponse("a1", "http://cdn/avatars/u/a1.png"));
        when(mediaService.uploadProfileImage(eq("user-1"), any())).thenReturn(response);

        mockMvc.perform(multipart("/media/profile")
                .file(file)
                .header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.id").value("a1"));
    }

    @Test
    void getProfileImage_ShouldReturn200() throws Exception {
        ProfileImageResponse response = new ProfileImageResponse(new MediaImageResponse("a1", "http://cdn/avatars/u/a1.png"));
        when(mediaService.getProfileImage("user-1")).thenReturn(response);

        mockMvc.perform(get("/media/profile/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.id").value("a1"));
    }
}
