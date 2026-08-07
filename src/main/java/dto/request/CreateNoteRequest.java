package dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(

        @NotBlank(message = "Title cannot be empty")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @Size(max = 5000, message = "Content length must not exceed 5000 characters")
        @NotNull(message = "Content cannot be null")
        String content
) {}