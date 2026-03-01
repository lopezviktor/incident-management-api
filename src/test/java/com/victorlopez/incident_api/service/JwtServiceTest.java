package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.model.Role;
import com.victorlopez.incident_api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "testSecretKeyForJWTTokenGenerationThatIsAtLeast32Bytes");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("victor")
                .email("victor@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Should generate a non-blank token for a user")
    void shouldGenerateToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from generated token")
    void shouldExtractUsername() {
        String token = jwtService.generateToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("victor");
    }

    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // already expired
        String expiredToken = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject a tampered token")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        boolean isValid = jwtService.isTokenValid(tampered);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract USER role as authority from token")
    void shouldExtractUserAuthority() {
        String token = jwtService.generateToken(testUser);

        List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should extract ADMIN role as authority from token")
    void shouldExtractAdminAuthority() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateToken(adminUser);
        List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
