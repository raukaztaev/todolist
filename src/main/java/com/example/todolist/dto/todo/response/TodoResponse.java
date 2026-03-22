package com.example.todolist.dto.todo.response;

import com.example.todolist.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;

@Schema(description = "Todo response")
public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        Priority priority,
        LocalDateTime dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}
