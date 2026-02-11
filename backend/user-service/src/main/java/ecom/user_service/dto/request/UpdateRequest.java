package ecom.user_service.dto.request;

import ecom.user_service.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @Pattern(regexp = "^[a-zA-Z0-9]{4,15}$", message = "username must be between 4 and 15 characters and contain only characters and numbers")
        String username,
        @Email(message = "Invalid Email Format")
        String email,
        @Size(min = 6, max = 100)
        String password,
        Role role,
        String avatarUrl
        ) {

}
