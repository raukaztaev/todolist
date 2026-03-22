package com.example.todolist.service;

import com.example.todolist.dto.auth.request.LoginRequest;
import com.example.todolist.dto.auth.request.LogoutRequest;
import com.example.todolist.dto.auth.request.RefreshTokenRequest;
import com.example.todolist.dto.auth.request.RegisterRequest;
import com.example.todolist.dto.auth.response.AuthResponse;
import com.example.todolist.entity.RefreshToken;
import com.example.todolist.entity.User;
import com.example.todolist.enums.Role;
import com.example.todolist.exception.UserAlreadyExistsException;
import com.example.todolist.mapper.UserMapper;
import com.example.todolist.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);
        RefreshToken refreshToken = refreshTokenService.createToken(savedUser);
        String accessToken = generateAccessToken(savedUser);
        return buildAuthResponse(savedUser, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );

        User user = ((com.example.todolist.security.UserPrincipal) authentication.getPrincipal()).getUser();
        RefreshToken refreshToken = refreshTokenService.createToken(user);
        String accessToken = generateAccessToken(user);
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        String accessToken = generateAccessToken(refreshToken.getUser());
        return buildAuthResponse(refreshToken.getUser(), accessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private String generateAccessToken(User user) {
        return jwtService.generateAccessToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "role", user.getRole().name()
                )
        );
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toSummary(user)
        );
    }
}
