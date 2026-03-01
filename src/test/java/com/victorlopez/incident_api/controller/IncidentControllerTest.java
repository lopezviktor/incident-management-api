package com.victorlopez.incident_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.config.SecurityConfig;
import com.victorlopez.incident_api.service.IncidentService;
import com.victorlopez.incident_api.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
@Import(SecurityConfig.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return similar incidents when description parameter provided")
    void shouldReturnSimilarIncidents() throws Exception {
        // ARRANGE
        String description = "Database connection timeout error";
        
        List<IncidentResponse> similarIncidents = List.of(
                IncidentResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Database connection issues")
                        .description("Connection pool exhausted in production")
                        .severity(Severity.HIGH)
                        .status(Status.RESOLVED)
                        .category(Category.DATABASE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                IncidentResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Timeout error on API")
                        .description("API endpoints timing out during peak traffic")
                        .severity(Severity.MEDIUM)
                        .status(Status.OPEN)
                        .category(Category.BACKEND)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentService.findSimilarIncidents(eq(description), any(UUID.class)))
                .thenReturn(similarIncidents);

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar")
                        .param("description", description))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Database connection issues"))
                .andExpect(jsonPath("$[1].title").value("Timeout error on API"));
    }

    @Test
    @DisplayName("Should return empty list when no similar incidents found")
    void shouldReturnEmptyListWhenNoSimilarIncidentsFound() throws Exception {
        // ARRANGE
        String description = "Very unique error that never happened before";

        when(incidentService.findSimilarIncidents(eq(description), any(UUID.class)))
                .thenReturn(List.of());

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar")
                        .param("description", description))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should return server error when description parameter is missing")
    void shouldReturnServerErrorWhenDescriptionMissing() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return bad request when description parameter is empty")
    void shouldReturnBadRequestWhenDescriptionEmpty() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar")
                        .param("description", ""))
                .andExpect(status().isBadRequest());
    }
}