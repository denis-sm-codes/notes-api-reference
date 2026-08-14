package dto.response;

import entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponseDto(

        Long id,

        String username,

        String email,

        Role role,

        Integer noteCount
) {}