package dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoteResponseDto(

        Long id,

        String title,

        String content,

        String authorUsername,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {}