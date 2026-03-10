package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ecom.user_service.dto.request.UpdateRequest;
import ecom.user_service.dto.response.UserResponse;
import ecom.user_service.exceptions.EmailAlreadyExistsException;
import ecom.user_service.exceptions.UserNotFoundException;
import ecom.user_service.models.Role;
import ecom.user_service.models.User;
import ecom.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MediaValidationService mediaValidationService;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("mockUser");
        mockUser.setEmail("mockUser@test.com");
        mockUser.setPassword("mockUser123");
        mockUser.setRole(Role.CLIENT);
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserResponse response = userService.getCurrentUser(userId);

        assertNotNull(response);
        assertEquals("mockUser", response.username());
    }

    @Test
    void getCurrentUser_ThrowsUserNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getCurrentUser(userId));
    }

    @Test
    void UpdateProfileTest_Success() {
        UpdateRequest request = new UpdateRequest();
        request.setUsername("newName");
        request.setEmail("newName@test.com");
        request.setPassword("newName123");
        request.setRole(Role.SELLER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail("newName@test.com")).thenReturn(false);
        when(passwordEncoder.encode("newName123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse response = userService.UpdateProfile(userId, request);

        assertNotNull(response);
        verify(userRepository).save(mockUser);
        assertEquals("newName", mockUser.getUsername());
        assertEquals("newName@test.com", mockUser.getEmail());
        assertEquals("encodedNewPassword", mockUser.getPassword());
        assertEquals(Role.SELLER, mockUser.getRole());
    }

    @Test
    void updateProfile_ThrowsEmailAlreadyExistsException() {

        UpdateRequest request = new UpdateRequest();
        request.setEmail("taken@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.UpdateProfile(userId, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_UpdatesAvatarSuccessfully() {
        UpdateRequest request = mock(UpdateRequest.class);
        when(request.isAvatarMediaIdProvided()).thenReturn(true);
        when(request.getAvatarMediaId()).thenReturn("media-123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(mediaValidationService).validateAvatarOwnership(userId, "media-123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.UpdateProfile(userId, request);

        assertEquals("media-123", mockUser.getAvatarMediaId());
        verify(mediaValidationService).validateAvatarOwnership(userId, "media-123");
        verify(userRepository).save(mockUser);
    }
}
