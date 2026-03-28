package com.example.todolist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todolist.config.JwtProperties;
import com.example.todolist.entity.RefreshToken;
import com.example.todolist.entity.User;
import com.example.todolist.exception.InvalidRefreshTokenException;
import com.example.todolist.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
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

    @Test
    void shouldRejectUnknownRefreshToken() {
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verify("missing-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid");
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("revoked-token")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(true)
                .user(User.builder().id(1L).build())
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.verify("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void shouldRevokeRefreshToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-token")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .user(User.builder().id(1L).build())
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revoke("valid-token");

        assertThat(refreshToken.isRevoked()).isTrue();
    }

    @Test
    void shouldRevokeAllActiveUserTokensWhenCreatingNewOne() {
        User user = User.builder().id(1L).build();
        RefreshToken existingToken = RefreshToken.builder()
                .token("old-token")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .user(user)
                .build();
        RefreshToken savedToken = RefreshToken.builder()
                .token("new-token")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .user(user)
                .build();

        when(jwtProperties.refreshTokenExpirationDays()).thenReturn(7L);
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(List.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        RefreshToken created = refreshTokenService.createToken(user);

        assertThat(existingToken.isRevoked()).isTrue();
        assertThat(created.getToken()).isEqualTo("new-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
