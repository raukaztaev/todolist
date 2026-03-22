package com.example.todolist.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic API message response")
public record ApiMessageResponse(String message) {
}
