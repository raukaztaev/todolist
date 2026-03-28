package com.example.todolist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todolist.entity.User;
import com.example.todolist.enums.Role;
import com.example.todolist.repository.RefreshTokenRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndReturnTokens() throws Exception {
        Map<String, Object> payload = Map.of(
                "username", "john_doe",
                "email", "john.doe@example.com",
                "password", "StrongPassword123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    void shouldLoginUserAndReturnTokens() throws Exception {
        userRepository.save(User.builder()
                .username("john_doe")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        Map<String, Object> payload = Map.of(
                "email", "john.doe@example.com",
                "password", "StrongPassword123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectDuplicateEmailOnRegister() throws Exception {
        userRepository.save(User.builder()
                .username("existing_user")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        Map<String, Object> payload = Map.of(
                "username", "new_user",
                "email", "john.doe@example.com",
                "password", "StrongPassword123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with this email already exists"));
    }

    @Test
    void shouldRejectDuplicateUsernameOnRegister() throws Exception {
        userRepository.save(User.builder()
                .username("existing_user")
                .email("existing@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        Map<String, Object> payload = Map.of(
                "username", "existing_user",
                "email", "new@example.com",
                "password", "StrongPassword123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with this username already exists"));
    }

    @Test
    void shouldRefreshAccessTokenForValidRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
                .username("john_doe")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        String refreshToken = refreshTokenService.createToken(user).getToken();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    void shouldLogoutAndRevokeRefreshToken() throws Exception {
        User user = userRepository.save(User.builder()
                .username("john_doe")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("StrongPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        String refreshToken = refreshTokenService.createToken(user).getToken();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    void shouldReturnValidationErrorsForInvalidRegisterPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "username", "  ",
                "email", "invalid-email",
                "password", "short"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.username").value(
                        Matchers.anyOf(
                                Matchers.is("Username is required"),
                                Matchers.is("Username must be between 3 and 50 characters")
                        )))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.validationErrors.password")
                        .value("Password must be between 8 and 100 characters"));
    }

    @Test
    void shouldReturnValidationErrorsForInvalidLoginPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "not-an-email",
                "password", ""
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.validationErrors.password").value("Password is required"));
    }

    @Test
    void shouldReturnValidationErrorForBlankRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.refreshToken").value("Refresh token is required"));
    }

    @Test
    void shouldReturnValidationErrorForBlankLogoutRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.refreshToken").value("Refresh token is required"));
    }
}
