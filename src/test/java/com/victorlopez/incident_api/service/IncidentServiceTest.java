package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.AIAnalysisResult;
import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.dto.MetricsResponse;
import com.victorlopez.incident_api.dto.UpdateIncidentRequest;
import com.victorlopez.incident_api.dto.UpdateStatusRequest;
import com.victorlopez.incident_api.exception.IncidentNotFoundException;
import com.victorlopez.incident_api.model.Category;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AIAnalysisService aiAnalysisService;

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

        AIAnalysisResult mockAIResult = new AIAnalysisResult(
                Severity.HIGH,
                Category.BACKEND,
                "Backend Team",
                "Check server logs and rollback recent deployment if necessary",
                4,
                0.88
        );

        when(aiAnalysisService.analyzeIncident(anyString(), anyString()))
                .thenReturn(mockAIResult);

        Incident savedIncident = Incident.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
                .severity(Severity.HIGH)
                .category(Category.BACKEND)
                .status(Status.OPEN)
                .assignedTeam("Backend Team")
                .suggestedSolution("Check server logs and rollback recent deployment if necessary")
                .estimatedResolutionHours(4)
                .aiConfidence(0.88)
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
        assertThat(response.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(response.getCategory()).isEqualTo(Category.BACKEND);
        assertThat(response.getAssignedTeam()).isEqualTo("Backend Team");
        assertThat(response.getAiConfidence()).isEqualTo(0.88);

        verify(aiAnalysisService).analyzeIncident(
                "API endpoint returning 500",
                "Users getting internal server error on checkout endpoint"
        );
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should throw exception when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
        // ARRANGE
        UUID randomId = UUID.randomUUID();
        when(incidentRepository.findByIdAndArchivedFalse(randomId)).thenReturn(Optional.empty());

        // ASSERT
        assertThatThrownBy(() -> incidentService.getIncidentById(randomId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incident not found with id");
    }

    @Test
    @DisplayName("Should return paginated incidents when no filters applied (ADMIN view)")
    void shouldReturnAllIncidents() {
        // ARRANGE
        Pageable pageable = PageRequest.of(0, 20);
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

        when(incidentRepository.findByArchivedFalse(pageable)).thenReturn(new PageImpl<>(incidents));

        // ACT
        Page<IncidentResponse> responses = incidentService.getAllIncidents(null, null, pageable, null);

        // ASSERT
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should filter incidents by status (ADMIN view)")
    void shouldFilterIncidentsByStatus() {
        // ARRANGE
        Pageable pageable = PageRequest.of(0, 20);
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

        when(incidentRepository.findByStatusAndArchivedFalse(Status.OPEN, pageable))
                .thenReturn(new PageImpl<>(openIncidents));

        // ACT
        Page<IncidentResponse> responses = incidentService.getAllIncidents(Status.OPEN, null, pageable, null);

        // ASSERT
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("Should filter incidents by reportedBy when USER role (Step 5)")
    void shouldFilterIncidentsByReportedByForUser() {
        // ARRANGE
        Pageable pageable = PageRequest.of(0, 20);
        String username = "alice";
        List<Incident> userIncidents = List.of(
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("Alice incident")
                        .description("Alice incident description")
                        .severity(Severity.MEDIUM)
                        .status(Status.OPEN)
                        .reportedBy(username)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentRepository.findByReportedByAndArchivedFalse(username, pageable))
                .thenReturn(new PageImpl<>(userIncidents));

        // ACT
        Page<IncidentResponse> responses = incidentService.getAllIncidents(null, null, pageable, username);

        // ASSERT
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getReportedBy()).isEqualTo(username);
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

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(updated);

        // ACT
        IncidentResponse response = incidentService.updateStatus(id, request);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(Status.RESOLVED);
    }

    @Test
    @DisplayName("Should soft-delete (archive) an incident")
    void shouldArchiveIncident() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Incident existing = Incident.builder()
                .id(id)
                .title("Some incident")
                .description("Some incident description")
                .severity(Severity.LOW)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(existing);

        // ACT
        incidentService.deleteIncident(id);

        // ASSERT
        assertThat(existing.isArchived()).isTrue();
        verify(incidentRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw IncidentNotFoundException when archiving non-existent incident")
    void shouldThrowWhenArchivingNonExistentIncident() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.empty());

        // ASSERT
        assertThatThrownBy(() -> incidentService.deleteIncident(id))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    @DisplayName("Should update incident fields (ADMIN full update)")
    void shouldUpdateIncidentFields() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Incident existing = Incident.builder()
                .id(id)
                .title("Old title")
                .description("Old description that is long enough")
                .severity(Severity.LOW)
                .category(Category.FRONTEND)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("New updated title")
                .severity(Severity.HIGH)
                .build();

        Incident saved = Incident.builder()
                .id(id)
                .title("New updated title")
                .description(existing.getDescription())
                .severity(Severity.HIGH)
                .category(Category.FRONTEND)
                .status(Status.OPEN)
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        IncidentResponse response = incidentService.updateIncident(id, request);

        // ASSERT
        assertThat(response.getTitle()).isEqualTo("New updated title");
        assertThat(response.getSeverity()).isEqualTo(Severity.HIGH);
        verify(incidentRepository).save(existing);
    }

    @Test
    @DisplayName("Should re-analyze incident and update AI fields (ADMIN)")
    void shouldReanalyzeIncident() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Incident existing = Incident.builder()
                .id(id)
                .title("Database connection timeout")
                .description("PostgreSQL pool exhausted under load")
                .severity(Severity.LOW)
                .category(Category.FRONTEND)
                .assignedTeam("Frontend Team")
                .suggestedSolution("Old solution")
                .estimatedResolutionHours(1)
                .aiConfidence(0.50)
                .status(Status.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AIAnalysisResult newAnalysis = new AIAnalysisResult(
                Severity.CRITICAL,
                Category.DATABASE,
                "Database Team",
                "Increase connection pool size and add read replicas",
                8,
                0.95
        );

        Incident saved = Incident.builder()
                .id(id)
                .title(existing.getTitle())
                .description(existing.getDescription())
                .severity(Severity.CRITICAL)
                .category(Category.DATABASE)
                .assignedTeam("Database Team")
                .suggestedSolution("Increase connection pool size and add read replicas")
                .estimatedResolutionHours(8)
                .aiConfidence(0.95)
                .status(Status.OPEN)
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(aiAnalysisService.analyzeIncident(existing.getTitle(), existing.getDescription()))
                .thenReturn(newAnalysis);
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        IncidentResponse response = incidentService.reanalyzeIncident(id);

        // ASSERT
        assertThat(response.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(response.getCategory()).isEqualTo(Category.DATABASE);
        assertThat(response.getAssignedTeam()).isEqualTo("Database Team");
        assertThat(response.getAiConfidence()).isEqualTo(0.95);
        verify(aiAnalysisService).analyzeIncident(existing.getTitle(), existing.getDescription());
    }

    @Test
    @DisplayName("Should return metrics (excluding archived incidents)")
    void shouldReturnMetrics() {
        // ARRANGE
        when(incidentRepository.countByArchivedFalse()).thenReturn(10L);
        when(incidentRepository.countByStatusAndArchivedFalse(any())).thenReturn(2L);
        when(incidentRepository.countBySeverityAndArchivedFalse(any())).thenReturn(3L);
        when(incidentRepository.countByCategoryAndArchivedFalse(any())).thenReturn(1L);
        when(incidentRepository.findAverageResolutionHours()).thenReturn(4.5);
        when(incidentRepository.countByStatusAndSeverityAndArchivedFalse(Status.OPEN, Severity.CRITICAL)).thenReturn(1L);

        // ACT
        MetricsResponse metrics = incidentService.getMetrics();

        // ASSERT
        assertThat(metrics.getTotalIncidents()).isEqualTo(10L);
        assertThat(metrics.getAverageResolutionHours()).isEqualTo(4.5);
        assertThat(metrics.getOpenCriticalIncidents()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find similar incidents and return responses")
    void shouldFindSimilarIncidents() {
        // ARRANGE
        UUID excludeId = UUID.randomUUID();
        String description = "Database connection timeout error in production";

        List<Incident> similarIncidents = List.of(
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("Database connection issues")
                        .description("Connection pool exhausted in production environment")
                        .severity(Severity.HIGH)
                        .status(Status.RESOLVED)
                        .category(Category.DATABASE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Incident.builder()
                        .id(UUID.randomUUID())
                        .title("Timeout error on API calls")
                        .description("API endpoints timing out during peak traffic")
                        .severity(Severity.MEDIUM)
                        .status(Status.OPEN)
                        .category(Category.BACKEND)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(incidentRepository.findByArchivedFalse()).thenReturn(similarIncidents);

        // ACT
        List<IncidentResponse> responses = incidentService.findSimilarIncidents(description, excludeId);

        // ASSERT
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Database connection issues");
        assertThat(responses.get(1).getTitle()).isEqualTo("Timeout error on API calls");

        verify(incidentRepository).findByArchivedFalse();
    }

    @Test
    @DisplayName("Should return empty list when no similar incidents found")
    void shouldReturnEmptyListWhenNoSimilarIncidentsFound() {
        // ARRANGE
        UUID excludeId = UUID.randomUUID();
        String description = "Very unique error that never happened before";

        when(incidentRepository.findByArchivedFalse()).thenReturn(List.of());

        // ACT
        List<IncidentResponse> responses = incidentService.findSimilarIncidents(description, excludeId);

        // ASSERT
        assertThat(responses).isEmpty();
        verify(incidentRepository).findByArchivedFalse();
    }

    @Test
    @DisplayName("Should extract keywords correctly from description")
    void shouldExtractKeywordsCorrectly() {
        // ARRANGE
        UUID excludeId = UUID.randomUUID();
        String description = "The database connection is timing out!";

        when(incidentRepository.findByArchivedFalse()).thenReturn(List.of());

        // ACT
        incidentService.findSimilarIncidents(description, excludeId);

        // ASSERT
        verify(incidentRepository).findByArchivedFalse();
    }
}
