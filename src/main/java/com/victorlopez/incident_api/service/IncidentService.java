package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentResponse createIncident(CreateIncidentRequest request) {
        log.info("Creating incident: {}", request.getTitle());

        // Map request to entity
        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
                .build();

        // Save to database
        Incident saved = incidentRepository.save(incident);

        log.info("Incident created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    public IncidentResponse getIncidentById(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));

        return mapToResponse(incident);
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