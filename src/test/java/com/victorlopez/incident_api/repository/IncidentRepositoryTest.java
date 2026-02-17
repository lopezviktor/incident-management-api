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
}