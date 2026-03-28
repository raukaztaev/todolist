package com.example.todolist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminTodoControllerIntegrationTest {

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

    private User admin;
    private User firstUser;
    private User secondUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        todoRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        firstUser = userRepository.save(User.builder()
                .username("first_user")
                .email("first@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        secondUser = userRepository.save(User.builder()
                .username("second_user")
                .email("second@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        adminToken = jwtService.generateAccessToken(
                admin.getEmail(),
                Map.of("userId", admin.getId(), "role", admin.getRole().name())
        );
        userToken = jwtService.generateAccessToken(
                firstUser.getEmail(),
                Map.of("userId", firstUser.getId(), "role", firstUser.getRole().name())
        );
    }

    @Test
    void shouldAllowAdminToListTodosAcrossUsers() throws Exception {
        todoRepository.save(Todo.builder()
                .title("First user todo")
                .description("visible for admin")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(firstUser)
                .build());
        todoRepository.save(Todo.builder()
                .title("Second user todo")
                .description("also visible")
                .completed(true)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.of(2026, 4, 3, 10, 0))
                .owner(secondUser)
                .build());

        mockMvc.perform(get("/api/admin/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldAllowAdminToFilterTodosByOwner() throws Exception {
        todoRepository.save(Todo.builder()
                .title("Owned by first")
                .description("match")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(firstUser)
                .build());
        todoRepository.save(Todo.builder()
                .title("Owned by second")
                .description("no match")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(secondUser)
                .build());

        mockMvc.perform(get("/api/admin/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("ownerId", firstUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Owned by first"));
    }

    @Test
    void shouldForbidRegularUserFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/todos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void shouldAllowAdminToReadForeignTodo() throws Exception {
        Todo foreignTodo = todoRepository.save(Todo.builder()
                .title("Foreign todo")
                .description("admin can read")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(secondUser)
                .build());

        mockMvc.perform(get("/api/admin/todos/{id}", foreignTodo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Foreign todo"));
    }

    @Test
    void shouldAllowAdminToUpdateForeignTodoStatus() throws Exception {
        Todo foreignTodo = todoRepository.save(Todo.builder()
                .title("Foreign todo")
                .description("admin updates")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(secondUser)
                .build());

        mockMvc.perform(patch("/api/admin/todos/{id}/status", foreignTodo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("completed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        assertThat(todoRepository.findById(foreignTodo.getId())).get()
                .extracting(Todo::isCompleted)
                .isEqualTo(true);
    }

    @Test
    void shouldAllowAdminToDeleteForeignTodo() throws Exception {
        Todo foreignTodo = todoRepository.save(Todo.builder()
                .title("Foreign todo")
                .description("admin deletes")
                .completed(false)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.of(2026, 4, 2, 10, 0))
                .owner(secondUser)
                .build());

        mockMvc.perform(delete("/api/admin/todos/{id}", foreignTodo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertThat(todoRepository.findById(foreignTodo.getId())).isEmpty();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
