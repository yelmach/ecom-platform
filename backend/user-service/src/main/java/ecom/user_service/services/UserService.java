package ecom.user_service.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ecom.user_service.dto.request.UpdateRequest;
import ecom.user_service.models.User;
import ecom.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User UpdateProfile(String userId, UpdateRequest updateRequest) {
        User user = userRepository.findById(userId).orElseThrow(null);

        // Update lastName
        if (updateRequest.username() != null && !updateRequest.username().trim().isEmpty()) {
            user.setUsername(updateRequest.username().trim());
        }

        // Update email
        if (updateRequest.email() != null && !updateRequest.email().trim().isEmpty()) {
            String newEmail = updateRequest.email().trim();
            if (!user.getEmail().equals(newEmail)) {
                if (userRepository.existByEmail(newEmail)) {
                    return null;//handle exception
                }
                user.setEmail(newEmail);
            }
        }

        // Update password
        if (updateRequest.password() != null && !updateRequest.password().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateRequest.password()));
        }

        return userRepository.save(user);
    }
}
