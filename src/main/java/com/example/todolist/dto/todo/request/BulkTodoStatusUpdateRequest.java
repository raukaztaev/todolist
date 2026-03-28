package com.example.todolist.dto.todo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Bulk todo status update request")
public record BulkTodoStatusUpdateRequest(
        @Schema(example = "[1, 2, 3]")
        @NotEmpty(message = "Todo ids are required")
        List<Long> todoIds,

        @Schema(example = "true")
        @NotNull(message = "Completed status is required")
        Boolean completed
) {
}
