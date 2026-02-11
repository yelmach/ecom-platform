package ecom.user_service.dto.response;

public record AuthResponse(
                String token,
                UserResponse user) {
}
