package com.example.todolist.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request using email and password")
public record LoginRequest(
        @Schema(example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(example = "StrongPassword123")
        @NotBlank(message = "Password is required")
        String password
) {
}
