package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ecom.user_service.models.Role;
import ecom.user_service.models.User;
import ecom.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnUserDetails() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@test.com");
        user.setUsername("testuser");
        user.setPassword("encoded-pass");
        user.setRole(Role.CLIENT);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("test@test.com");

        assertSame(user, result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void loadUserByUsername_ShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@test.com"));

        assertEquals("User not found with email: missing@test.com", ex.getMessage());
    }
}
