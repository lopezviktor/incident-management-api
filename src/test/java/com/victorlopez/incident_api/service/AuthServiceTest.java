package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.AuthResponse;
import com.victorlopez.incident_api.dto.LoginRequest;
import com.victorlopez.incident_api.dto.RegisterRequest;
import com.victorlopez.incident_api.exception.UserAlreadyExistsException;
import com.victorlopez.incident_api.model.Role;
import com.victorlopez.incident_api.model.User;
import com.victorlopez.incident_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should register a new user and return auth response with token")
    void shouldRegisterUser() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setUsername("victor");
        request.setEmail("victor@example.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("victor")
                .email("victor@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByUsername("victor")).thenReturn(false);
        when(userRepository.existsByEmail("victor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token-123");

        // ACT
        AuthResponse response = authService.register(request);

        // ASSERT
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getUsername()).isEqualTo("victor");
        assertThat(response.getRole()).isEqualTo(Role.USER);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username is taken")
    void shouldThrowWhenUsernameAlreadyExists() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setUsername("victor");
        request.setEmail("victor@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("victor")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("victor");
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email is already registered")
    void shouldThrowWhenEmailAlreadyExists() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("victor@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("victor@example.com")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("victor@example.com");
    }

    @Test
    @DisplayName("Should login with valid credentials and return auth response")
    void shouldLoginWithValidCredentials() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setUsername("victor");
        request.setPassword("password123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("victor")
                .email("victor@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token-456");

        // ACT
        AuthResponse response = authService.login(request);

        // ASSERT
        assertThat(response.getToken()).isEqualTo("jwt-token-456");
        assertThat(response.getUsername()).isEqualTo("victor");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found")
    void shouldThrowWhenUserNotFound() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("password123");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is wrong")
    void shouldThrowWhenPasswordIsWrong() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setUsername("victor");
        request.setPassword("wrongpassword");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("victor")
                .email("victor@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
