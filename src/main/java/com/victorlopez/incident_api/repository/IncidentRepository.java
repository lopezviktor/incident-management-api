package com.victorlopez.incident_api.repository;

import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByStatus(Status status);

    List<Incident> findBySeverity(Severity severity);

    List<Incident> findByStatusAndSeverity(Status status, Severity severity);
}