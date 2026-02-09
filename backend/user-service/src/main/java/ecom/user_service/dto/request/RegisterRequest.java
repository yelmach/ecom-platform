package ecom.user_service.dto.request;

import ecom.user_service.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-zA-Z0-9]{4,15}$", message = "username must be between 4 and 15 characters and contain only characters and numbers")
        String username,
        @Email(message = "Invalid Email Format")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank
        @Size(min = 6, max = 100)
        String password,
        @NotNull(message = "You must define your role")
        Role role
        ) {

}
