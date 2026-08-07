package dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record UpdateNoteResponse(

        Long id,

        String title,

        String content,

        ZonedDateTime updatedAt
) {}