package com.example.todolist.controller;

import com.example.todolist.dto.common.PageResponse;
import com.example.todolist.dto.todo.request.CreateTodoRequest;
import com.example.todolist.dto.todo.request.UpdateTodoRequest;
import com.example.todolist.dto.todo.request.UpdateTodoStatusRequest;
import com.example.todolist.dto.todo.response.TodoResponse;
import com.example.todolist.entity.User;
import com.example.todolist.enums.Priority;
import com.example.todolist.security.UserPrincipal;
import com.example.todolist.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Tag(name = "Todos", description = "CRUD endpoints for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "Create a new todo")
    @ApiResponse(responseCode = "201", description = "Todo created")
    @PostMapping
    public ResponseEntity<TodoResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTodoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.create(currentUser(principal), request));
    }

    @Operation(summary = "Get current user's todos with filters, sorting and pagination")
    @GetMapping
    public ResponseEntity<PageResponse<TodoResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Priority priority,
            @Parameter(description = "ISO date-time, e.g. 2026-03-22T10:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dueDateFrom,
            @Parameter(description = "ISO date-time, e.g. 2026-03-31T18:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dueDateTo
    ) {
        return ResponseEntity.ok(todoService.findAll(
                principal.getId(),
                page,
                size,
                sort,
                direction,
                completed,
                priority,
                dueDateFrom,
                dueDateTo
        ));
    }

    @Operation(summary = "Get one todo by id")
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(todoService.findById(id, principal.getId()));
    }

    @Operation(summary = "Replace a todo")
    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        return ResponseEntity.ok(todoService.update(id, principal.getId(), request));
    }

    @Operation(summary = "Update completed status only")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TodoResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoStatusRequest request
    ) {
        return ResponseEntity.ok(todoService.updateStatus(id, principal.getId(), request.completed()));
    }

    @Operation(summary = "Delete a todo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        todoService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(UserPrincipal principal) {
        return principal.getUser();
    }
}
