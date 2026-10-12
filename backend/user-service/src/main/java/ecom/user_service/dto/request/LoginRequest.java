package ecom.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required") @Email String email,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 72) String password) {
}
