package dto.response;

import entity.Role;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record UserResponseDto(

        Long id,

        String username,

        String email,

        Role role,

        Long noteCount,

        ZonedDateTime createdAt
) {}