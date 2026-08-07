package dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record RegisterResponse(

        String token,

        Long id,

        String username,

        String email,

        ZonedDateTime createdAt
) {}