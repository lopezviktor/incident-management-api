package com.victorlopez.incident_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.dto.MetricsResponse;
import com.victorlopez.incident_api.dto.UpdateStatusRequest;
import com.victorlopez.incident_api.exception.IncidentNotFoundException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // ==================== POST /api/incidents ====================

    @Test
    @DisplayName("POST /api/incidents - Should create incident and return 201 with body")
    void shouldCreateIncidentAndReturn201() throws Exception {
        // ARRANGE
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Production database is down");
        request.setDescription("Main PostgreSQL instance is not responding to connections");
        request.setReportedBy("ops-team");

        IncidentResponse response = IncidentResponse.builder()
                .id(UUID.randomUUID())
                .title("Production database is down")
                .description("Main PostgreSQL instance is not responding to connections")
                .severity(Severity.CRITICAL)
                .category(Category.DATABASE)
                .status(Status.OPEN)
                .reportedBy("ops-team")
                .assignedTeam("Database Team")
                .suggestedSolution("Restart the database service and check disk space")
                .estimatedResolutionHours(2)
                .aiConfidence(0.92)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/incidents")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Production database is down"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.category").value("DATABASE"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTeam").value("Database Team"))
                .andExpect(jsonPath("$.aiConfidence").value(0.92));
    }

    @Test
    @DisplayName("POST /api/incidents - Should return 400 when title is blank")
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        // ARRANGE
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("");
        request.setDescription("Some description with enough characters");

        // ACT & ASSERT
        mockMvc.perform(post("/api/incidents")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("POST /api/incidents - Should return 400 when description is too short")
    void shouldReturn400WhenDescriptionIsTooShort() throws Exception {
        // ARRANGE
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Valid title here");
        request.setDescription("Too short");

        // ACT & ASSERT
        mockMvc.perform(post("/api/incidents")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.description").exists());
    }

    // ==================== GET /api/incidents ====================

    @Test
    @DisplayName("GET /api/incidents - Should return paginated list of incidents")
    void shouldReturnPaginatedIncidents() throws Exception {
        // ARRANGE
        List<IncidentResponse> incidentList = List.of(
                IncidentResponse.builder()
                        .id(UUID.randomUUID())
                        .title("API gateway timeout")
                        .description("Load balancer health checks failing")
                        .severity(Severity.HIGH)
                        .status(Status.OPEN)
                        .category(Category.NETWORK)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                IncidentResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Login page CSS broken")
                        .description("Styles missing after latest deployment")
                        .severity(Severity.LOW)
                        .status(Status.RESOLVED)
                        .category(Category.FRONTEND)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentService.getAllIncidents(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(incidentList));

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("API gateway timeout"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/incidents - Should filter by status parameter")
    void shouldFilterIncidentsByStatus() throws Exception {
        // ARRANGE
        List<IncidentResponse> openIncidents = List.of(
                IncidentResponse.builder()
                        .id(UUID.randomUUID())
                        .title("Open incident")
                        .description("An open incident in production")
                        .severity(Severity.HIGH)
                        .status(Status.OPEN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentService.getAllIncidents(eq(Status.OPEN), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(openIncidents));

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    // ==================== GET /api/incidents/{id} ====================

    @Test
    @DisplayName("GET /api/incidents/{id} - Should return incident by ID")
    void shouldReturnIncidentById() throws Exception {
        // ARRANGE
        UUID id = UUID.randomUUID();
        IncidentResponse response = IncidentResponse.builder()
                .id(id)
                .title("Memory leak in backend service")
                .description("Node.js service consuming 100% memory after 2 hours of uptime")
                .severity(Severity.HIGH)
                .category(Category.BACKEND)
                .status(Status.IN_PROGRESS)
                .assignedTeam("Backend Team")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentService.getIncidentById(id)).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Memory leak in backend service"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /api/incidents/{id} - Should return 404 when incident not found")
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        // ARRANGE
        UUID id = UUID.randomUUID();
        when(incidentService.getIncidentById(id))
                .thenThrow(new IncidentNotFoundException(id));

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incident not found with id: " + id));
    }

    // ==================== PATCH /api/incidents/{id}/status ====================

    @Test
    @DisplayName("PATCH /api/incidents/{id}/status - Should update status to RESOLVED")
    void shouldUpdateIncidentStatusToResolved() throws Exception {
        // ARRANGE
        UUID id = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(Status.RESOLVED);
        request.setActualResolution("Rolled back to the previous stable deployment");

        IncidentResponse response = IncidentResponse.builder()
                .id(id)
                .title("API endpoint returning 500")
                .description("Users getting internal server error on checkout endpoint")
                .severity(Severity.HIGH)
                .status(Status.RESOLVED)
                .actualResolution("Rolled back to the previous stable deployment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentService.updateStatus(eq(id), any(UpdateStatusRequest.class))).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(patch("/api/incidents/{id}/status", id)
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.actualResolution").value("Rolled back to the previous stable deployment"));
    }

    @Test
    @DisplayName("PATCH /api/incidents/{id}/status - Should return 404 when incident not found")
    void shouldReturn404WhenUpdatingNonExistentIncident() throws Exception {
        // ARRANGE
        UUID id = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(Status.IN_PROGRESS);

        when(incidentService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
                .thenThrow(new IncidentNotFoundException(id));

        // ACT & ASSERT
        mockMvc.perform(patch("/api/incidents/{id}/status", id)
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/incidents/metrics ====================

    @Test
    @DisplayName("GET /api/incidents/metrics - Should return incident metrics")
    void shouldReturnIncidentMetrics() throws Exception {
        // ARRANGE
        MetricsResponse metrics = MetricsResponse.builder()
                .totalIncidents(42L)
                .byStatus(Map.of("OPEN", 15L, "IN_PROGRESS", 8L, "RESOLVED", 17L, "CLOSED", 2L))
                .bySeverity(Map.of("CRITICAL", 3L, "HIGH", 12L, "MEDIUM", 20L, "LOW", 7L))
                .byCategory(Map.of("BACKEND", 18L, "DATABASE", 10L, "FRONTEND", 8L, "NETWORK", 4L, "SECURITY", 2L))
                .averageResolutionHours(6.5)
                .openCriticalIncidents(3L)
                .build();

        when(incidentService.getMetrics()).thenReturn(metrics);

        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalIncidents").value(42))
                .andExpect(jsonPath("$.openCriticalIncidents").value(3))
                .andExpect(jsonPath("$.averageResolutionHours").value(6.5))
                .andExpect(jsonPath("$.byStatus.OPEN").value(15))
                .andExpect(jsonPath("$.bySeverity.CRITICAL").value(3));
    }

    // ==================== GET /api/incidents/similar ====================

    @Test
    @DisplayName("GET /api/incidents/similar - Should return similar incidents when description provided")
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
    @DisplayName("GET /api/incidents/similar - Should return empty list when no similar incidents found")
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
    @DisplayName("GET /api/incidents/similar - Should return 400 when description parameter is missing")
    void shouldReturnBadRequestWhenDescriptionMissing() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Required parameter 'description' is missing"));
    }

    @Test
    @DisplayName("GET /api/incidents/similar - Should return 400 when description parameter is empty")
    void shouldReturnBadRequestWhenDescriptionEmpty() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/incidents/similar")
                        .param("description", ""))
                .andExpect(status().isBadRequest());
    }
}
