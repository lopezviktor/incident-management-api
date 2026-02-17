package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.dto.MetricsResponse;
import com.victorlopez.incident_api.dto.UpdateStatusRequest;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.repository.IncidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    @DisplayName("Should create incident and return response")
    void shouldCreateIncident() {
        // ARRANGE
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("API endpoint returning 500");
        request.setDescription("Users getting internal server error on checkout endpoint");
        request.setReportedBy("victor.lopez");

        Incident savedIncident = Incident.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
                .severity(Severity.MEDIUM)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // ACT
        IncidentResponse response = incidentService.createIncident(request);

        // ASSERT
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("API endpoint returning 500");
        assertThat(response.getStatus()).isEqualTo(Status.OPEN);
        assertThat(response.getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    @DisplayName("Should throw exception when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
        // ARRANGE
        UUID randomId = UUID.randomUUID();
        when(incidentRepository.findById(randomId)).thenReturn(Optional.empty());

        // ASSERT
        assertThatThrownBy(() -> incidentService.getIncidentById(randomId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incident not found with id");
    }

    @Test
    @DisplayName("Should return all incidents when no filters applied")
    void shouldReturnAllIncidents() {
        // ARRANGE
        List<Incident> incidents = List.of(
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("First incident")
                        .description("First incident description")
                        .severity(Severity.HIGH)
                        .status(Status.OPEN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("Second incident")
                        .description("Second incident description")
                        .severity(Severity.LOW)
                        .status(Status.RESOLVED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentRepository.findAll()).thenReturn(incidents);

        // ACT
        List<IncidentResponse> responses = incidentService.getAllIncidents(null, null);

        // ASSERT
        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("Should filter incidents by status")
    void shouldFilterIncidentsByStatus() {
        // ARRANGE
        List<Incident> openIncidents = List.of(
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("Open incident")
                        .description("Open incident description")
                        .severity(Severity.HIGH)
                        .status(Status.OPEN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentRepository.findByStatus(Status.OPEN)).thenReturn(openIncidents);

        // ACT
        List<IncidentResponse> responses = incidentService.getAllIncidents(Status.OPEN, null);

        // ASSERT
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("Should update incident status to RESOLVED")
    void shouldUpdateIncidentStatus() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Incident existing = Incident.builder()
                .id(id)
                .title("API endpoint returning 500")
                .description("Users getting internal server error on checkout endpoint")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(Status.RESOLVED);
        request.setActualResolution("Rolled back faulty deployment from 14:30");

        Incident updated = Incident.builder()
                .id(id)
                .title(existing.getTitle())
                .description(existing.getDescription())
                .severity(existing.getSeverity())
                .status(Status.RESOLVED)
                .actualResolution("Rolled back faulty deployment from 14:30")
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .resolvedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(updated);

        // ACT
        IncidentResponse response = incidentService.updateStatus(id, request);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(Status.RESOLVED);
    }

    @Test
    @DisplayName("Should return metrics")
    void shouldReturnMetrics() {
        // ARRANGE
        when(incidentRepository.count()).thenReturn(10L);
        when(incidentRepository.countByStatus(any())).thenReturn(2L);
        when(incidentRepository.countBySeverity(any())).thenReturn(3L);
        when(incidentRepository.countByCategory(any())).thenReturn(1L);
        when(incidentRepository.findAverageResolutionHours()).thenReturn(4.5);
        when(incidentRepository.countByStatusAndSeverity(Status.OPEN, Severity.CRITICAL)).thenReturn(1L);

        // ACT
        MetricsResponse metrics = incidentService.getMetrics();

        // ASSERT
        assertThat(metrics.getTotalIncidents()).isEqualTo(10L);
        assertThat(metrics.getAverageResolutionHours()).isEqualTo(4.5);
        assertThat(metrics.getOpenCriticalIncidents()).isEqualTo(1L);
    }
}