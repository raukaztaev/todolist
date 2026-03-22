package com.example.todolist.dto.todo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Partial todo status update request")
public record UpdateTodoStatusRequest(
        @Schema(example = "true")
        @NotNull(message = "Completed status is required")
        Boolean completed
) {
}
