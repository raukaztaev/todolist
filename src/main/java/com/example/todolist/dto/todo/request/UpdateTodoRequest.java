package com.example.todolist.dto.todo.request;

import com.example.todolist.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Full todo update request")
public record UpdateTodoRequest(
        @Schema(example = "Prepare project portfolio v2")
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Schema(example = "Extend docs and add more tests")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Schema(example = "true")
        boolean completed,

        @Schema(example = "HIGH")
        @NotNull(message = "Priority is required")
        Priority priority,

        @Schema(example = "2026-03-31T20:30:00")
        LocalDateTime dueDate
) {
}
