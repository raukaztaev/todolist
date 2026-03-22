package com.example.todolist.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with access and refresh tokens")
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserSummaryResponse user
) {
}
