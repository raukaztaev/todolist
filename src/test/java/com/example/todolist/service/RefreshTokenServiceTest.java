package com.example.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.todolist.config.JwtProperties;
import com.example.todolist.entity.RefreshToken;
import com.example.todolist.entity.User;
import com.example.todolist.exception.InvalidRefreshTokenException;
import com.example.todolist.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .user(User.builder().id(1L).build())
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verify("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token has expired");
    }

    @Test
    void shouldReturnValidRefreshToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-token")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .user(User.builder().id(1L).build())
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));

        RefreshToken verified = refreshTokenService.verify("valid-token");

        assertThat(verified.getToken()).isEqualTo("valid-token");
    }
}
