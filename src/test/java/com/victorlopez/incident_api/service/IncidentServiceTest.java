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
}