package com.victorlopez.incident_api.repository;

import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.IncidentActivity;
import com.victorlopez.incident_api.model.IncidentActivityAction;
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
class IncidentActivityRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentActivityRepository activityRepository;

    private Incident savedIncident() {
        return incidentRepository.save(Incident.builder()
                .title("Test incident title")
                .description("Test incident description for activity tests")
                .severity(Severity.HIGH)
                .status(Status.OPEN)
                .reportedBy("testuser")
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve activity for an incident")
    void shouldPersistAndRetrieveActivity() {
        // ARRANGE
        Incident incident = savedIncident();

        IncidentActivity activity = IncidentActivity.builder()
                .incident(incident)
                .action(IncidentActivityAction.CREATED)
                .performedBy("alice")
                .details("Incident created — severity: HIGH, category: null")
                .build();

        // ACT
        activityRepository.save(activity);
        List<IncidentActivity> results = activityRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

        // ASSERT
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo(IncidentActivityAction.CREATED);
        assertThat(results.get(0).getPerformedBy()).isEqualTo("alice");
        assertThat(results.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return activities in chronological order")
    void shouldReturnActivitiesInChronologicalOrder() throws InterruptedException {
        // ARRANGE
        Incident incident = savedIncident();

        activityRepository.save(IncidentActivity.builder()
                .incident(incident)
                .action(IncidentActivityAction.CREATED)
                .performedBy("alice")
                .details("Incident created")
                .build());

        // Small delay to ensure distinct createdAt timestamps
        Thread.sleep(10);

        activityRepository.save(IncidentActivity.builder()
                .incident(incident)
                .action(IncidentActivityAction.STATUS_CHANGED)
                .performedBy("bob")
                .details("Status changed from OPEN to IN_PROGRESS")
                .build());

        Thread.sleep(10);

        activityRepository.save(IncidentActivity.builder()
                .incident(incident)
                .action(IncidentActivityAction.UPDATED)
                .performedBy("admin")
                .details("Fields updated: severity → CRITICAL")
                .build());

        // ACT
        List<IncidentActivity> results = activityRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

        // ASSERT
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getAction()).isEqualTo(IncidentActivityAction.CREATED);
        assertThat(results.get(1).getAction()).isEqualTo(IncidentActivityAction.STATUS_CHANGED);
        assertThat(results.get(2).getAction()).isEqualTo(IncidentActivityAction.UPDATED);
    }

    @Test
    @DisplayName("Should only return activities for the specified incident")
    void shouldOnlyReturnActivitiesForSpecifiedIncident() {
        // ARRANGE
        Incident incident1 = savedIncident();
        Incident incident2 = incidentRepository.save(Incident.builder()
                .title("Another incident title")
                .description("Another incident description for isolation test")
                .severity(Severity.LOW)
                .status(Status.OPEN)
                .build());

        activityRepository.save(IncidentActivity.builder()
                .incident(incident1)
                .action(IncidentActivityAction.CREATED)
                .performedBy("alice")
                .details("Created incident 1")
                .build());

        activityRepository.save(IncidentActivity.builder()
                .incident(incident2)
                .action(IncidentActivityAction.CREATED)
                .performedBy("bob")
                .details("Created incident 2")
                .build());

        // ACT
        List<IncidentActivity> results = activityRepository.findByIncidentIdOrderByCreatedAtAsc(incident1.getId());

        // ASSERT
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPerformedBy()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Should return empty list when incident has no activities")
    void shouldReturnEmptyListForIncidentWithNoActivities() {
        // ARRANGE
        Incident incident = savedIncident();

        // ACT
        List<IncidentActivity> results = activityRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

        // ASSERT
        assertThat(results).isEmpty();
    }
}
