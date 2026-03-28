package com.example.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todolist.entity.RefreshToken;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.User;
import com.example.todolist.enums.Priority;
import com.example.todolist.enums.Role;
import com.example.todolist.repository.RefreshTokenRepository;
import com.example.todolist.repository.TodoRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User anotherUser;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        todoRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .username("owner")
                .email("owner@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        anotherUser = userRepository.save(User.builder()
                .username("another")
                .email("another@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        ownerToken = jwtService.generateAccessToken(
                owner.getEmail(),
                Map.of("userId", owner.getId(), "role", owner.getRole().name())
        );
    }

    @Test
    void shouldCreateTodoForAuthenticatedUser() throws Exception {
        Map<String, Object> payload = Map.of(
                "title", "Write integration tests",
                "description", "Cover auth and todo endpoints",
                "priority", "HIGH",
                "dueDate", "2026-03-30T18:00:00"
        );

        mockMvc.perform(post("/api/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Write integration tests"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void shouldReturnOnlyCurrentUsersTodos() throws Exception {
        todoRepository.save(Todo.builder()
                .title("Owner todo")
                .description("visible")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 25, 12, 0))
                .owner(owner)
                .build());

        todoRepository.save(Todo.builder()
                .title("Another todo")
                .description("hidden")
                .completed(true)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.of(2026, 3, 26, 12, 0))
                .owner(anotherUser)
                .build());

        mockMvc.perform(get("/api/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Owner todo"));
    }

    @Test
    void shouldReturnNotFoundForForeignTodoAccess() throws Exception {
        Todo foreignTodo = todoRepository.save(Todo.builder()
                .title("Foreign todo")
                .description("must stay hidden")
                .completed(false)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.of(2026, 3, 28, 9, 0))
                .owner(anotherUser)
                .build());

        mockMvc.perform(get("/api/todos/{id}", foreignTodo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void shouldUpdateTodoStatus() throws Exception {
        Todo todo = todoRepository.save(Todo.builder()
                .title("Owner todo")
                .description("status update")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 29, 12, 0))
                .owner(owner)
                .build());

        mockMvc.perform(patch("/api/todos/{id}/status", todo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("completed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void shouldDeleteOwnedTodo() throws Exception {
        Todo todo = todoRepository.save(Todo.builder()
                .title("Delete me")
                .description("remove")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 29, 12, 0))
                .owner(owner)
                .build());

        mockMvc.perform(delete("/api/todos/{id}", todo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateOwnedTodo() throws Exception {
        Todo todo = todoRepository.save(Todo.builder()
                .title("Old title")
                .description("old description")
                .completed(false)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.of(2026, 3, 29, 12, 0))
                .owner(owner)
                .build());

        Map<String, Object> payload = Map.of(
                "title", "Updated title",
                "description", "updated description",
                "completed", true,
                "priority", "HIGH",
                "dueDate", "2026-04-01T10:30:00"
        );

        mockMvc.perform(put("/api/todos/{id}", todo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("updated description"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-04-01T10:30:00"));
    }

    @Test
    void shouldFilterTodosByStatusPriorityAndDueDateRange() throws Exception {
        todoRepository.save(Todo.builder()
                .title("Matching todo")
                .description("visible")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 3, 30, 10, 0))
                .owner(owner)
                .build());

        todoRepository.save(Todo.builder()
                .title("Completed todo")
                .description("filtered out")
                .completed(true)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 3, 30, 10, 0))
                .owner(owner)
                .build());

        todoRepository.save(Todo.builder()
                .title("Wrong priority")
                .description("filtered out")
                .completed(false)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.of(2026, 3, 30, 10, 0))
                .owner(owner)
                .build());

        todoRepository.save(Todo.builder()
                .title("Outside range")
                .description("filtered out")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 4, 5, 10, 0))
                .owner(owner)
                .build());

        mockMvc.perform(get("/api/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("completed", "false")
                        .param("priority", "HIGH")
                        .param("dueDateFrom", "2026-03-29T00:00:00")
                        .param("dueDateTo", "2026-03-31T23:59:59")
                        .param("sort", "dueDate")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Matching todo"))
                .andExpect(jsonPath("$.sort").value("dueDate"))
                .andExpect(jsonPath("$.direction").value("ASC"));
    }

    @Test
    void shouldFallbackToDefaultSortingForInvalidSortAndDirection() throws Exception {
        todoRepository.save(Todo.builder()
                .title("Later created")
                .description("second")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 31, 12, 0))
                .owner(owner)
                .build());

        todoRepository.save(Todo.builder()
                .title("Earlier created")
                .description("first")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 28, 12, 0))
                .owner(owner)
                .build());

        mockMvc.perform(get("/api/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("sort", "title")
                        .param("direction", "SIDEWAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort").value("createdAt"))
                .andExpect(jsonPath("$.direction").value("DESC"));
    }

    @Test
    void shouldRejectInvalidCreateTodoPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "title", " ",
                "description", "valid description"
        );

        mockMvc.perform(post("/api/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.priority").value("Priority is required"));
    }

    @Test
    void shouldRejectInvalidUpdateTodoPayload() throws Exception {
        Todo todo = todoRepository.save(Todo.builder()
                .title("Update me")
                .description("description")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 29, 12, 0))
                .owner(owner)
                .build());

        Map<String, Object> payload = Map.of(
                "title", "",
                "description", "still valid",
                "completed", false,
                "dueDate", "2026-04-01T10:30:00"
        );

        mockMvc.perform(put("/api/todos/{id}", todo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.priority").value("Priority is required"));
    }

    @Test
    void shouldRejectMissingCompletedStatus() throws Exception {
        Todo todo = todoRepository.save(Todo.builder()
                .title("Owner todo")
                .description("status update")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 3, 29, 12, 0))
                .owner(owner)
                .build());

        mockMvc.perform(patch("/api/todos/{id}/status", todo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.completed").value("Completed status is required"));
    }

    @Test
    void shouldRequireAuthenticationForTodoEndpoints() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
