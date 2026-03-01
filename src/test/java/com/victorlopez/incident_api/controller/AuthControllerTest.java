package com.victorlopez.incident_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victorlopez.incident_api.dto.AuthResponse;
import com.victorlopez.incident_api.dto.LoginRequest;
import com.victorlopez.incident_api.dto.RegisterRequest;
import com.victorlopez.incident_api.exception.UserAlreadyExistsException;
import com.victorlopez.incident_api.model.Role;
import com.victorlopez.incident_api.config.SecurityConfig;
import com.victorlopez.incident_api.service.AuthService;
import com.victorlopez.incident_api.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should register user and return 201 with token")
    void shouldRegisterUserAndReturn201() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setUsername("victor");
        request.setEmail("victor@example.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-123")
                .username("victor")
                .role(Role.USER)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.username").value("victor"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void shouldReturnConflictWhenUsernameExists() throws Exception {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setUsername("victor");
        request.setEmail("victor@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username already taken: victor"));

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when register request has invalid fields")
    void shouldReturnBadRequestWhenRegisterRequestInvalid() throws Exception {
        // ARRANGE - missing required fields
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab"); // too short (min 3)
        request.setEmail("not-an-email");
        request.setPassword("short"); // too short (min 8)

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login and return 200 with token")
    void shouldLoginAndReturn200() throws Exception {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setUsername("victor");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-456")
                .username("victor")
                .role(Role.USER)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-456"))
                .andExpect(jsonPath("$.username").value("victor"));
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void shouldReturnUnauthorizedWhenInvalidCredentials() throws Exception {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setUsername("victor");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when login request has blank fields")
    void shouldReturnBadRequestWhenLoginRequestInvalid() throws Exception {
        // ARRANGE - blank fields
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("");

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
