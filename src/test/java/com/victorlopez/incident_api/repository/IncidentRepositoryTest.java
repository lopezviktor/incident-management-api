package com.victorlopez.incident_api.repository;

import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("Should save and retrieve an incident")
    void shouldSaveAndRetrieveIncident() {
        // ARRANGE
        Incident incident = Incident.builder()
                .title("Database connection timeout")
                .description("PostgreSQL connection pool exhausted after high traffic spike")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .reportedBy("victor.lopez")
                .build();

        // ACT
        Incident saved = incidentRepository.save(incident);
        Incident found = incidentRepository.findById(saved.getId()).orElseThrow();

        // ASSERT
        assertThat(found.getId()).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Database connection timeout");
        assertThat(found.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(found.getStatus()).isEqualTo(Status.OPEN);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find incidents by status")
    void shouldFindIncidentsByStatus() {
        // ARRANGE
        incidentRepository.save(Incident.builder()
                .title("API endpoint returning 500")
                .description("Users getting internal server error on checkout endpoint")
                .status(Status.OPEN)
                .build());

        incidentRepository.save(Incident.builder()
                .title("Slow database queries")
                .description("Query execution time exceeding 5 seconds on reports page")
                .status(Status.IN_PROGRESS)
                .build());

        // ACT
        List<Incident> openIncidents = incidentRepository.findByStatus(Status.OPEN);

        // ASSERT
        assertThat(openIncidents).hasSize(1);
        assertThat(openIncidents.get(0).getTitle()).isEqualTo("API endpoint returning 500");
    }

    @Test
    @DisplayName("Should find incidents by severity")
    void shouldFindIncidentsBySeverity() {
        // ARRANGE
        incidentRepository.save(Incident.builder()
                .title("Production database down")
                .description("Main PostgreSQL instance is not responding to connections")
                .severity(Severity.CRITICAL)
                .build());

        incidentRepository.save(Incident.builder()
                .title("CSS misalignment on mobile")
                .description("Navigation menu overlaps content on screens smaller than 375px")
                .severity(Severity.LOW)
                .build());

        // ACT
        List<Incident> criticalIncidents = incidentRepository.findBySeverity(Severity.CRITICAL);

        // ASSERT
        assertThat(criticalIncidents).hasSize(1);
        assertThat(criticalIncidents.get(0).getTitle()).isEqualTo("Production database down");
    }

    @Test
    @DisplayName("Should find similar incidents by keywords excluding specified incident")
    void shouldFindSimilarIncidentsByKeywords() {
        // ARRANGE
        Incident incident1 = incidentRepository.save(Incident.builder()
                .title("Database connection timeout")
                .description("PostgreSQL connection pool exhausted after high traffic spike")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .build());

        Incident incident2 = incidentRepository.save(Incident.builder()
                .title("Database query slow")
                .description("SELECT queries taking more than 30 seconds on PostgreSQL")
                .severity(Severity.MEDIUM)
                .status(Status.RESOLVED)
                .build());

        Incident incident3 = incidentRepository.save(Incident.builder()
                .title("API endpoint error")
                .description("REST API returning 500 internal server error")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .build());

        // ACT
        List<Incident> similarIncidents = incidentRepository.findSimilarIncidents("database", incident1.getId());

        // ASSERT
        assertThat(similarIncidents).hasSize(1);
        assertThat(similarIncidents.get(0).getId()).isEqualTo(incident2.getId());
        assertThat(similarIncidents.get(0).getTitle()).contains("Database");
    }

    @Test
    @DisplayName("Should find similar incidents by single keyword")
    void shouldFindSimilarIncidentsBySingleKeyword() {
        // ARRANGE
        Incident incident1 = incidentRepository.save(Incident.builder()
                .title("PostgreSQL connection timeout")
                .description("Database connection pool exhausted")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .build());

        Incident incident2 = incidentRepository.save(Incident.builder()
                .title("MySQL slow queries")
                .description("Database queries taking too long")
                .severity(Severity.MEDIUM)
                .status(Status.RESOLVED)
                .build());

        Incident incident3 = incidentRepository.save(Incident.builder()
                .title("API timeout error")
                .description("Network timeout on user authentication")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .build());

        // ACT
        List<Incident> similarIncidents = incidentRepository.findSimilarIncidents("database", incident1.getId());

        // ASSERT
        assertThat(similarIncidents).hasSize(1);
        assertThat(similarIncidents.get(0).getId()).isEqualTo(incident2.getId());
    }

    @Test
    @DisplayName("Should return empty list when no similar incidents found")
    void shouldReturnEmptyListWhenNoSimilarIncidentsFound() {
        // ARRANGE
        Incident incident1 = incidentRepository.save(Incident.builder()
                .title("Database connection timeout")
                .description("PostgreSQL connection pool exhausted")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .build());

        // ACT
        List<Incident> similarIncidents = incidentRepository.findSimilarIncidents("nonexistent keyword", incident1.getId());

        // ASSERT
        assertThat(similarIncidents).isEmpty();
    }

    // ==================== Soft Delete (archived) ====================

    @Test
    @DisplayName("findByArchivedFalse - Should exclude archived incidents")
    void shouldExcludeArchivedIncidentsFromFindByArchivedFalse() {
        // ARRANGE
        incidentRepository.save(Incident.builder()
                .title("Active incident")
                .description("This incident is still active and visible")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .archived(false)
                .build());

        incidentRepository.save(Incident.builder()
                .title("Archived incident")
                .description("This incident has been soft deleted and should be hidden")
                .severity(Severity.LOW)
                .status(Status.CLOSED)
                .archived(true)
                .build());

        // ACT
        List<Incident> visible = incidentRepository.findByArchivedFalse();

        // ASSERT
        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getTitle()).isEqualTo("Active incident");
    }

    @Test
    @DisplayName("findByIdAndArchivedFalse - Should not find archived incident by ID")
    void shouldNotFindArchivedIncidentById() {
        // ARRANGE
        Incident archived = incidentRepository.save(Incident.builder()
                .title("Archived incident to fetch")
                .description("This was archived and should not be retrievable")
                .severity(Severity.LOW)
                .status(Status.CLOSED)
                .archived(true)
                .build());

        // ACT & ASSERT
        assertThat(incidentRepository.findByIdAndArchivedFalse(archived.getId())).isEmpty();
    }

    @Test
    @DisplayName("findSimilarIncidents - Should exclude archived incidents from similarity search")
    void shouldExcludeArchivedIncidentsFromSimilaritySearch() {
        // ARRANGE
        Incident active = incidentRepository.save(Incident.builder()
                .title("Database timeout active")
                .description("PostgreSQL connection pool exhausted in production")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .archived(false)
                .build());

        incidentRepository.save(Incident.builder()
                .title("Database timeout archived")
                .description("PostgreSQL connection archived and hidden from results")
                .severity(Severity.MEDIUM)
                .status(Status.CLOSED)
                .archived(true)
                .build());

        // ACT — search with a different excludeId so both would otherwise match
        List<Incident> similar = incidentRepository.findSimilarIncidents("database", UUID.randomUUID());

        // ASSERT — only the active one is returned
        assertThat(similar).hasSize(1);
        assertThat(similar.get(0).getId()).isEqualTo(active.getId());
    }
}