package com.example.todolist.dto.todo.request;

import com.example.todolist.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Partial todo priority update request")
public record UpdateTodoPriorityRequest(
        @Schema(example = "HIGH")
        @NotNull(message = "Priority is required")
        Priority priority
) {
}
