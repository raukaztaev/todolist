package com.example.todolist.dto.auth.response;

import com.example.todolist.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public user payload")
public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
