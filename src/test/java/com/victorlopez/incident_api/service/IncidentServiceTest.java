package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
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
}