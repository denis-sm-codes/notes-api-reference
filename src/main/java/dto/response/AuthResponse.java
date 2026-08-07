package dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AuthResponse(

        Long id,

        String username,

        String email,

        String accessToken,

        String refreshToken,

        List<String> roles
) {}