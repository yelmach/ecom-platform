package ecom.user_service.dto.response;

import ecom.user_service.models.Role;
import ecom.user_service.models.User;

public record UserResponse(
        String userId,
        String email,
        String username,
        Role role,
        String avatarMediaId
        ) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getAvatarMediaId()
        );
    }

}
