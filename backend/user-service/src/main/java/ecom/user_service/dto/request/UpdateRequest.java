package ecom.user_service.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import ecom.user_service.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequest {

    @Pattern(regexp = "^[a-zA-Z0-9]{4,15}$", message = "username must be between 4 and 15 characters and contain only characters and numbers")
    private String username;

    @Email(message = "Invalid Email Format")
    private String email;

    @Size(min = 6, max = 100)
    private String password;

    private Role role;

    private String avatarMediaId;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private boolean avatarMediaIdProvided;

    @JsonSetter("avatarMediaId")
    public void setAvatarMediaId(String avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
        this.avatarMediaIdProvided = true;
    }
}
