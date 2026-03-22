package com.example.todolist.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Logout request")
public record LogoutRequest(
        @Schema(example = "24ef8fb0-08fc-4435-a3ea-3d4a6a36218b")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
