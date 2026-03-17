package com.victorlopez.incident_api.repository;

import com.victorlopez.incident_api.model.IncidentActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentActivityRepository extends JpaRepository<IncidentActivity, UUID> {

    List<IncidentActivity> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
