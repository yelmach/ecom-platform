package ecom.user_service.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ecom.user_service.dto.request.UpdateRequest;
import ecom.user_service.dto.response.UserResponse;
import ecom.user_service.exceptions.UserNotFoundException;
import ecom.user_service.models.User;
import ecom.user_service.repository.UserRepository;
import ecom.user_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return ResponseEntity.status(HttpStatus.OK).body(UserResponse.fromEntity(user));
    }

    @PatchMapping
    public ResponseEntity<User> UpdateProfile(@Valid @RequestBody UpdateRequest request, @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.status(HttpStatus.OK).body(userService.UpdateProfile(userId, request));
    }

}
