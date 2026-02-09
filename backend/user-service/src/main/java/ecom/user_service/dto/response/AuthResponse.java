package ecom.user_service.dto.response;

import ecom.user_service.models.Role;

public record AuthResponse(
        String token,
        String email,
        Role role) {
}
