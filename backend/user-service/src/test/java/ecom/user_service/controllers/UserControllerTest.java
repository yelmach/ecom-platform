package ecom.user_service.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ecom.user_service.dto.response.UserResponse;
import ecom.user_service.models.Role;
import ecom.user_service.services.UserService;
import ecom.user_service.config.SecurityConfig;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse mockUserResponse;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        mockUserResponse = new UserResponse(userId, "user@test.com", "testuser", Role.CLIENT, null);
    }

    @Test
    void getCurrentUser_ShouldReturn200AndUserResponse() throws Exception {
        when(userService.getCurrentUser(userId)).thenReturn(mockUserResponse);

        mockMvc.perform(get("/users/me")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateProfile_ShouldReturn200AndUpdatedUserResponse() throws Exception {
        String request = """
                {"username":"newuser","email":"newuser@test.com","role":"SELLER"}
                """;

        UserResponse updatedResponse = new UserResponse(userId, "newuser@test.com", "newuser", Role.CLIENT, null);
        when(userService.updateProfile(eq(userId), any())).thenReturn(updatedResponse);

        mockMvc.perform(patch("/users/me")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("CLIENT"));

        verify(userService).updateProfile(eq(userId), any());
    }

    @Test
    void getCurrentUser_ShouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isBadRequest());
    }
}
