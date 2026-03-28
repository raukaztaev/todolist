package com.example.todolist.dto.todo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Partial todo due date update request")
public record UpdateTodoDueDateRequest(
        @Schema(example = "2026-04-02T10:00:00")
        @NotNull(message = "Due date is required")
        LocalDateTime dueDate
) {
}
