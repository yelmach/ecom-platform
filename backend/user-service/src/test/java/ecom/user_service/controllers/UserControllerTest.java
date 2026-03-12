package ecom.user_service.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ecom.user_service.dto.request.UpdateRequest;
import ecom.user_service.dto.response.UserResponse;
import ecom.user_service.models.Role;
import ecom.user_service.services.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        UpdateRequest request = new UpdateRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@test.com");
        request.setRole(Role.SELLER);

        UserResponse updatedResponse = new UserResponse(userId, "newuser@test.com", "newuser", Role.SELLER, null);
        when(userService.UpdateProfile(eq(userId), any(UpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/users/me")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("SELLER"));

        verify(userService).UpdateProfile(eq(userId), any(UpdateRequest.class));
    }

    @Test
    void getCurrentUser_ShouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isBadRequest());
    }
}
