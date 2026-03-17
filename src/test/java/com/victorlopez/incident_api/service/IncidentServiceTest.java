package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.AIAnalysisResult;
import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentActivityResponse;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.dto.MetricsResponse;
import com.victorlopez.incident_api.dto.UpdateIncidentRequest;
import com.victorlopez.incident_api.dto.UpdateStatusRequest;
import com.victorlopez.incident_api.exception.IncidentNotFoundException;
import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.IncidentActivity;
import com.victorlopez.incident_api.model.IncidentActivityAction;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.repository.IncidentActivityRepository;
import com.victorlopez.incident_api.repository.IncidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private IncidentActivityRepository incidentActivityRepository;

    @Mock
    private AIAnalysisService aiAnalysisService;

    @InjectMocks
    private IncidentService incidentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setAuthenticatedUser(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private Incident buildSavedIncident(UUID id, String title, String description) {
        return Incident.builder()
                .id(id)
                .title(title)
                .description(description)
                .severity(Severity.HIGH)
                .category(Category.BACKEND)
                .status(Status.OPEN)
                .assignedTeam("Backend Team")
                .suggestedSolution("Check server logs")
                .estimatedResolutionHours(4)
                .aiConfidence(0.88)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createIncident ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create incident and return response")
    void shouldCreateIncident() {
        // ARRANGE
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("API endpoint returning 500");
        request.setDescription("Users getting internal server error on checkout endpoint");
        request.setReportedBy("victor.lopez");

        AIAnalysisResult mockAIResult = new AIAnalysisResult(
                Severity.HIGH, Category.BACKEND, "Backend Team",
                "Check server logs and rollback recent deployment if necessary", 4, 0.88);

        when(aiAnalysisService.analyzeIncident(anyString(), anyString())).thenReturn(mockAIResult);

        Incident savedIncident = buildSavedIncident(UUID.randomUUID(),
                "API endpoint returning 500",
                "Users getting internal server error on checkout endpoint");
        savedIncident.setReportedBy("victor.lopez");

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
                "Users getting internal server error on checkout endpoint");
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should log CREATED activity when incident is created")
    void shouldLogCreatedActivityOnCreate() {
        // ARRANGE
        setAuthenticatedUser("alice");
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("New critical incident");
        request.setDescription("Production system is completely down for all users");

        AIAnalysisResult mockAI = new AIAnalysisResult(
                Severity.CRITICAL, Category.BACKEND, "Backend Team", "Restart the service", 2, 0.95);
        when(aiAnalysisService.analyzeIncident(anyString(), anyString())).thenReturn(mockAI);

        Incident saved = buildSavedIncident(UUID.randomUUID(), request.getTitle(), request.getDescription());
        saved.setSeverity(Severity.CRITICAL);
        saved.setCategory(Category.BACKEND);
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        incidentService.createIncident(request);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        IncidentActivity logged = captor.getValue();
        assertThat(logged.getAction()).isEqualTo(IncidentActivityAction.CREATED);
        assertThat(logged.getPerformedBy()).isEqualTo("alice");
        assertThat(logged.getDetails()).contains("CRITICAL");
        assertThat(logged.getDetails()).contains("BACKEND");
    }

    // ── getIncidentById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw exception when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
        UUID randomId = UUID.randomUUID();
        when(incidentRepository.findByIdAndArchivedFalse(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(randomId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incident not found with id");
    }

    // ── getAllIncidents ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated incidents when no filters applied (ADMIN view)")
    void shouldReturnAllIncidents() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Incident> incidents = List.of(
                buildSavedIncident(UUID.randomUUID(), "First incident", "First incident description"),
                buildSavedIncident(UUID.randomUUID(), "Second incident", "Second incident description"));

        when(incidentRepository.findByArchivedFalse(pageable)).thenReturn(new PageImpl<>(incidents));

        Page<IncidentResponse> responses = incidentService.getAllIncidents(null, null, pageable, null);

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should filter incidents by status (ADMIN view)")
    void shouldFilterIncidentsByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Incident> openIncidents = List.of(
                buildSavedIncident(UUID.randomUUID(), "Open incident", "Open incident description"));

        when(incidentRepository.findByStatusAndArchivedFalse(Status.OPEN, pageable))
                .thenReturn(new PageImpl<>(openIncidents));

        Page<IncidentResponse> responses = incidentService.getAllIncidents(Status.OPEN, null, pageable, null);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("Should filter incidents by reportedBy when USER role (Step 5)")
    void shouldFilterIncidentsByReportedByForUser() {
        Pageable pageable = PageRequest.of(0, 20);
        String username = "alice";
        Incident incident = buildSavedIncident(UUID.randomUUID(), "Alice incident", "Alice incident description");
        incident.setReportedBy(username);

        when(incidentRepository.findByReportedByAndArchivedFalse(username, pageable))
                .thenReturn(new PageImpl<>(List.of(incident)));

        Page<IncidentResponse> responses = incidentService.getAllIncidents(null, null, pageable, username);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getReportedBy()).isEqualTo(username);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update incident status to RESOLVED")
    void shouldUpdateIncidentStatus() {
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "API endpoint returning 500",
                "Users getting internal server error on checkout endpoint");

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(Status.RESOLVED);
        request.setActualResolution("Rolled back faulty deployment from 14:30");

        Incident updated = buildSavedIncident(id, existing.getTitle(), existing.getDescription());
        updated.setStatus(Status.RESOLVED);
        updated.setActualResolution("Rolled back faulty deployment from 14:30");
        updated.setResolvedAt(LocalDateTime.now());

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(updated);

        IncidentResponse response = incidentService.updateStatus(id, request);

        assertThat(response.getStatus()).isEqualTo(Status.RESOLVED);
    }

    @Test
    @DisplayName("Should log STATUS_CHANGED activity when status is updated")
    void shouldLogStatusChangedActivityOnUpdateStatus() {
        // ARRANGE
        setAuthenticatedUser("ops-user");
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Some incident", "Some description text here");
        existing.setStatus(Status.OPEN);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(Status.IN_PROGRESS);

        Incident updated = buildSavedIncident(id, existing.getTitle(), existing.getDescription());
        updated.setStatus(Status.IN_PROGRESS);

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(updated);

        // ACT
        incidentService.updateStatus(id, request);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        IncidentActivity logged = captor.getValue();
        assertThat(logged.getAction()).isEqualTo(IncidentActivityAction.STATUS_CHANGED);
        assertThat(logged.getPerformedBy()).isEqualTo("ops-user");
        assertThat(logged.getDetails()).contains("OPEN");
        assertThat(logged.getDetails()).contains("IN_PROGRESS");
    }

    // ── deleteIncident (archive) ──────────────────────────────────────────────

    @Test
    @DisplayName("Should soft-delete (archive) an incident")
    void shouldArchiveIncident() {
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Some incident", "Some incident description");

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(existing);

        incidentService.deleteIncident(id);

        assertThat(existing.isArchived()).isTrue();
        verify(incidentRepository).save(existing);
    }

    @Test
    @DisplayName("Should log ARCHIVED activity when incident is soft-deleted")
    void shouldLogArchivedActivityOnDelete() {
        // ARRANGE
        setAuthenticatedUser("admin");
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Some incident", "Some incident description");

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(existing);

        // ACT
        incidentService.deleteIncident(id);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(IncidentActivityAction.ARCHIVED);
        assertThat(captor.getValue().getPerformedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should throw IncidentNotFoundException when archiving non-existent incident")
    void shouldThrowWhenArchivingNonExistentIncident() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.deleteIncident(id))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    // ── updateIncident ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update incident fields (ADMIN full update)")
    void shouldUpdateIncidentFields() {
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Old title", "Old description that is long enough");
        existing.setSeverity(Severity.LOW);
        existing.setCategory(Category.FRONTEND);

        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("New updated title").severity(Severity.HIGH).build();

        Incident saved = buildSavedIncident(id, "New updated title", existing.getDescription());
        saved.setSeverity(Severity.HIGH);
        saved.setCategory(Category.FRONTEND);

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        IncidentResponse response = incidentService.updateIncident(id, request);

        assertThat(response.getTitle()).isEqualTo("New updated title");
        assertThat(response.getSeverity()).isEqualTo(Severity.HIGH);
        verify(incidentRepository).save(existing);
    }

    @Test
    @DisplayName("Should log UPDATED activity when incident is updated")
    void shouldLogUpdatedActivityOnUpdate() {
        // ARRANGE
        setAuthenticatedUser("admin");
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Old title", "Old description that is long enough");

        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("New title").severity(Severity.CRITICAL).build();

        Incident saved = buildSavedIncident(id, "New title", existing.getDescription());
        saved.setSeverity(Severity.CRITICAL);

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        incidentService.updateIncident(id, request);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        IncidentActivity logged = captor.getValue();
        assertThat(logged.getAction()).isEqualTo(IncidentActivityAction.UPDATED);
        assertThat(logged.getPerformedBy()).isEqualTo("admin");
        assertThat(logged.getDetails()).contains("title");
        assertThat(logged.getDetails()).contains("CRITICAL");
    }

    // ── reanalyzeIncident ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should re-analyze incident and update AI fields (ADMIN)")
    void shouldReanalyzeIncident() {
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Database connection timeout",
                "PostgreSQL pool exhausted under load");
        existing.setSeverity(Severity.LOW);
        existing.setCategory(Category.FRONTEND);

        AIAnalysisResult newAnalysis = new AIAnalysisResult(
                Severity.CRITICAL, Category.DATABASE, "Database Team",
                "Increase connection pool size and add read replicas", 8, 0.95);

        Incident saved = buildSavedIncident(id, existing.getTitle(), existing.getDescription());
        saved.setSeverity(Severity.CRITICAL);
        saved.setCategory(Category.DATABASE);
        saved.setAssignedTeam("Database Team");
        saved.setAiConfidence(0.95);

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(aiAnalysisService.analyzeIncident(existing.getTitle(), existing.getDescription()))
                .thenReturn(newAnalysis);
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        IncidentResponse response = incidentService.reanalyzeIncident(id);

        assertThat(response.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(response.getCategory()).isEqualTo(Category.DATABASE);
        assertThat(response.getAssignedTeam()).isEqualTo("Database Team");
        assertThat(response.getAiConfidence()).isEqualTo(0.95);
        verify(aiAnalysisService).analyzeIncident(existing.getTitle(), existing.getDescription());
    }

    @Test
    @DisplayName("Should log ANALYZED activity when incident is re-analyzed")
    void shouldLogAnalyzedActivityOnReanalyze() {
        // ARRANGE
        setAuthenticatedUser("admin");
        UUID id = UUID.randomUUID();
        Incident existing = buildSavedIncident(id, "Database connection timeout",
                "PostgreSQL pool exhausted under load");

        AIAnalysisResult newAnalysis = new AIAnalysisResult(
                Severity.CRITICAL, Category.DATABASE, "Database Team", "Fix the pool", 4, 0.95);

        Incident saved = buildSavedIncident(id, existing.getTitle(), existing.getDescription());
        saved.setSeverity(Severity.CRITICAL);
        saved.setCategory(Category.DATABASE);

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(existing));
        when(aiAnalysisService.analyzeIncident(anyString(), anyString())).thenReturn(newAnalysis);
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        incidentService.reanalyzeIncident(id);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        IncidentActivity logged = captor.getValue();
        assertThat(logged.getAction()).isEqualTo(IncidentActivityAction.ANALYZED);
        assertThat(logged.getPerformedBy()).isEqualTo("admin");
        assertThat(logged.getDetails()).contains("CRITICAL");
        assertThat(logged.getDetails()).contains("DATABASE");
    }

    // ── getIncidentActivity ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return activity log for an incident")
    void shouldReturnActivityLogForIncident() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Incident incident = buildSavedIncident(id, "Test incident", "Test incident description");

        List<IncidentActivity> activities = List.of(
                IncidentActivity.builder()
                        .id(UUID.randomUUID())
                        .incident(incident)
                        .action(IncidentActivityAction.CREATED)
                        .performedBy("alice")
                        .details("Incident created — severity: HIGH, category: BACKEND")
                        .createdAt(LocalDateTime.now().minusMinutes(10))
                        .build(),
                IncidentActivity.builder()
                        .id(UUID.randomUUID())
                        .incident(incident)
                        .action(IncidentActivityAction.STATUS_CHANGED)
                        .performedBy("bob")
                        .details("Status changed from OPEN to IN_PROGRESS")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.of(incident));
        when(incidentActivityRepository.findByIncidentIdOrderByCreatedAtAsc(id)).thenReturn(activities);

        // ACT
        List<IncidentActivityResponse> responses = incidentService.getIncidentActivity(id);

        // ASSERT
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAction()).isEqualTo(IncidentActivityAction.CREATED);
        assertThat(responses.get(0).getPerformedBy()).isEqualTo("alice");
        assertThat(responses.get(0).getIncidentId()).isEqualTo(id);
        assertThat(responses.get(1).getAction()).isEqualTo(IncidentActivityAction.STATUS_CHANGED);
        assertThat(responses.get(1).getPerformedBy()).isEqualTo("bob");
    }

    @Test
    @DisplayName("Should throw IncidentNotFoundException when getting activity for non-existent incident")
    void shouldThrowWhenGettingActivityForNonExistentIncident() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findByIdAndArchivedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentActivity(id))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    // ── performedBy defaults to "system" without auth ─────────────────────────

    @Test
    @DisplayName("Should use 'system' as performedBy when no authenticated user")
    void shouldUseSystemAsPerformedByWithoutAuth() {
        // ARRANGE — no SecurityContext set
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("System-created incident");
        request.setDescription("Created without an authenticated user in context");

        AIAnalysisResult mockAI = new AIAnalysisResult(
                Severity.MEDIUM, Category.BACKEND, "Backend Team", "Check logs", 2, 0.80);
        when(aiAnalysisService.analyzeIncident(anyString(), anyString())).thenReturn(mockAI);

        Incident saved = buildSavedIncident(UUID.randomUUID(), request.getTitle(), request.getDescription());
        when(incidentRepository.save(any(Incident.class))).thenReturn(saved);

        // ACT
        incidentService.createIncident(request);

        // ASSERT
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(incidentActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getPerformedBy()).isEqualTo("system");
    }

    // ── metrics ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return metrics (excluding archived incidents)")
    void shouldReturnMetrics() {
        when(incidentRepository.countByArchivedFalse()).thenReturn(10L);
        when(incidentRepository.countByStatusAndArchivedFalse(any())).thenReturn(2L);
        when(incidentRepository.countBySeverityAndArchivedFalse(any())).thenReturn(3L);
        when(incidentRepository.countByCategoryAndArchivedFalse(any())).thenReturn(1L);
        when(incidentRepository.findAverageResolutionHours()).thenReturn(4.5);
        when(incidentRepository.countByStatusAndSeverityAndArchivedFalse(Status.OPEN, Severity.CRITICAL)).thenReturn(1L);

        MetricsResponse metrics = incidentService.getMetrics();

        assertThat(metrics.getTotalIncidents()).isEqualTo(10L);
        assertThat(metrics.getAverageResolutionHours()).isEqualTo(4.5);
        assertThat(metrics.getOpenCriticalIncidents()).isEqualTo(1L);
    }

    // ── similarity search ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should find similar incidents and return responses")
    void shouldFindSimilarIncidents() {
        UUID excludeId = UUID.randomUUID();
        String description = "Database connection timeout error in production";

        List<Incident> similarIncidents = List.of(
                buildSavedIncident(UUID.randomUUID(), "Database connection issues",
                        "Connection pool exhausted in production environment"),
                buildSavedIncident(UUID.randomUUID(), "Timeout error on API calls",
                        "API endpoints timing out during peak traffic"));

        when(incidentRepository.findByArchivedFalse()).thenReturn(similarIncidents);

        List<IncidentResponse> responses = incidentService.findSimilarIncidents(description, excludeId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Database connection issues");
        verify(incidentRepository).findByArchivedFalse();
    }

    @Test
    @DisplayName("Should return empty list when no similar incidents found")
    void shouldReturnEmptyListWhenNoSimilarIncidentsFound() {
        UUID excludeId = UUID.randomUUID();
        when(incidentRepository.findByArchivedFalse()).thenReturn(List.of());

        List<IncidentResponse> responses = incidentService.findSimilarIncidents(
                "Very unique error that never happened before", excludeId);

        assertThat(responses).isEmpty();
        verify(incidentRepository).findByArchivedFalse();
    }

    @Test
    @DisplayName("Should extract keywords correctly from description")
    void shouldExtractKeywordsCorrectly() {
        UUID excludeId = UUID.randomUUID();
        when(incidentRepository.findByArchivedFalse()).thenReturn(List.of());

        incidentService.findSimilarIncidents("The database connection is timing out!", excludeId);

        verify(incidentRepository).findByArchivedFalse();
    }
}
