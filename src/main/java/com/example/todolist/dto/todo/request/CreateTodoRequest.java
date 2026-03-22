package com.example.todolist.dto.todo.request;

import com.example.todolist.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Create todo request")
public record CreateTodoRequest(
        @Schema(example = "Prepare project portfolio")
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Schema(example = "Finalize README and Swagger examples")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Schema(example = "MEDIUM")
        @NotNull(message = "Priority is required")
        Priority priority,

        @Schema(example = "2026-03-30T18:00:00")
        LocalDateTime dueDate
) {
}
