package dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record NoteResponse(

        Long id,

        String title,

        String content,

        ZonedDateTime createdAt,

        ZonedDateTime updatedAt
) {}