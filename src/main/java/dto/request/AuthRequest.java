package dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(

        @NotBlank(message = "Username or email cannot be empty")
        String nameOrEmail,

        @NotBlank(message = "Password cannot be empty")
        String password
) {}