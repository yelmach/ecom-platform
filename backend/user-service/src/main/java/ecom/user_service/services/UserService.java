package ecom.user_service.services;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ecom.user_service.dto.request.UpdateRequest;
import ecom.user_service.dto.response.UserResponse;
import ecom.user_service.event.UserEventProducer;
import ecom.user_service.exceptions.EmailAlreadyExistsException;
import ecom.user_service.exceptions.UserNotFoundException;
import ecom.user_service.exceptions.UsernameAlreadyExistsException;
import ecom.user_service.models.User;
import ecom.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaValidationService mediaValidationService;
    private final UserEventProducer userEventProducer;

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User Not Found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse updateProfile(String userId, UpdateRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String previousMediaId = user.getAvatarMediaId();

        if (updateRequest.getUsername() != null && !updateRequest.getUsername().trim().isEmpty()) {
            String newUsername = updateRequest.getUsername().trim();
            if (!user.getUsername().equals(newUsername)) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new UsernameAlreadyExistsException("Username already exists");
                }
                user.setUsername(newUsername);
            }
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            String newEmail = updateRequest.getEmail().trim();
            if (!user.getEmail().equals(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new EmailAlreadyExistsException("Email already exists");
                }
                user.setEmail(newEmail);
            }
        }

        if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        if (updateRequest.isAvatarMediaIdProvided()) {
            String avatarMediaId = updateRequest.getAvatarMediaId();

            if (avatarMediaId == null || avatarMediaId.trim().isEmpty()) {
                user.setAvatarMediaId(null);
            } else {
                mediaValidationService.validateAvatarOwnership(userId, avatarMediaId.trim());
                user.setAvatarMediaId(avatarMediaId.trim());
            }
        }

        User savedUser = userRepository.save(user);
        if (updateRequest.isAvatarMediaIdProvided() && previousMediaId != null
                && !Objects.equals(previousMediaId, savedUser.getAvatarMediaId())) {
            userEventProducer.publishUserAvatarChanged(userId, previousMediaId, savedUser.getAvatarMediaId());
        }
        return UserResponse.fromEntity(savedUser);
    }
}
