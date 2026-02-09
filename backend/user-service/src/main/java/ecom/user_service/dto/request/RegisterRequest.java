package ecom.user_service.dto.request;

import javax.management.relation.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @Email(message = "Invalid Email Format") @NotBlank(message = "Email is required") String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull(message = "You must define your role") Role role) {
}