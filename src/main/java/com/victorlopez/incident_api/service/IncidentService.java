package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.exception.IncidentNotFoundException;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentResponse createIncident(CreateIncidentRequest request) {
        log.info("Creating incident: {}", request.getTitle());

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Incident created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    public IncidentResponse getIncidentById(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        return mapToResponse(incident);
    }

    public List<IncidentResponse> getAllIncidents(Status status, Severity severity) {
        List<Incident> incidents;

        if (status != null && severity != null) {
            incidents = incidentRepository.findByStatusAndSeverity(status, severity);
        } else if (status != null) {
            incidents = incidentRepository.findByStatus(status);
        } else if (severity != null) {
            incidents = incidentRepository.findBySeverity(severity);
        } else {
            incidents = incidentRepository.findAll();
        }

        return incidents.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private IncidentResponse mapToResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .category(incident.getCategory())
                .status(incident.getStatus())
                .reportedBy(incident.getReportedBy())
                .assignedTeam(incident.getAssignedTeam())
                .suggestedSolution(incident.getSuggestedSolution())
                .estimatedResolutionHours(incident.getEstimatedResolutionHours())
                .aiConfidence(incident.getAiConfidence())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}