package com.example.todolist.controller;

import com.example.todolist.dto.common.PageResponse;
import com.example.todolist.dto.todo.request.UpdateTodoStatusRequest;
import com.example.todolist.dto.todo.response.TodoResponse;
import com.example.todolist.enums.Priority;
import com.example.todolist.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/todos")
@RequiredArgsConstructor
@Tag(name = "Admin Todos", description = "Admin-only todo management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminTodoController {

    private final TodoService todoService;

    @Operation(summary = "Get all todos across all users")
    @GetMapping
    public ResponseEntity<PageResponse<TodoResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Priority priority
    ) {
        return ResponseEntity.ok(todoService.findAllForAdmin(page, size, sort, direction, ownerId, completed, priority));
    }

    @Operation(summary = "Get a todo by id regardless of owner")
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.findByIdForAdmin(id));
    }

    @Operation(summary = "Update completed status of any todo")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TodoResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoStatusRequest request
    ) {
        return ResponseEntity.ok(todoService.updateStatusForAdmin(id, request.completed()));
    }

    @Operation(summary = "Delete any todo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.deleteForAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
