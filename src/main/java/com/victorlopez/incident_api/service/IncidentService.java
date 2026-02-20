package com.victorlopez.incident_api.service;

import com.victorlopez.incident_api.dto.*;
import com.victorlopez.incident_api.exception.IncidentNotFoundException;
import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AIAnalysisService aiAnalysisService;

    public IncidentResponse createIncident(CreateIncidentRequest request) {
        log.info("Creating incident: {}", request.getTitle());

        // NUEVO: Análisis con AI
        AIAnalysisResult aiAnalysis = aiAnalysisService.analyzeIncident(
                request.getTitle(),
                request.getDescription()
        );

        log.info("AI analysis - Severity: {}, Category: {}",
                aiAnalysis.severity(), aiAnalysis.category());

        // Crear incident con datos de AI
        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
                // Datos del AI
                .severity(aiAnalysis.severity())
                .category(aiAnalysis.category())
                .assignedTeam(aiAnalysis.assignedTeam())
                .suggestedSolution(aiAnalysis.suggestedSolution())
                .estimatedResolutionHours(aiAnalysis.estimatedResolutionHours())
                .aiConfidence(aiAnalysis.confidence())
                .status(Status.OPEN)
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

    public IncidentResponse updateStatus(UUID id, UpdateStatusRequest request) {
        log.info("Updating status of incident {} to {}", id, request.getStatus());

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        incident.setStatus(request.getStatus());

        if (request.getActualResolution() != null) {
            incident.setActualResolution(request.getActualResolution());
        }

        // Set resolvedAt when incident is resolved or closed
        if (request.getStatus() == Status.RESOLVED || request.getStatus() == Status.CLOSED) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        Incident updated = incidentRepository.save(incident);
        return mapToResponse(updated);
    }

    public MetricsResponse getMetrics() {
        long total = incidentRepository.count();

        Map<String, Long> byStatus = Arrays.stream(Status.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        incidentRepository::countByStatus
                ));

        Map<String, Long> bySeverity = Arrays.stream(Severity.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        incidentRepository::countBySeverity
                ));

        Map<String, Long> byCategory = Arrays.stream(Category.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        incidentRepository::countByCategory
                ));

        Double avgHours = incidentRepository.findAverageResolutionHours();
        long openCritical = incidentRepository.countByStatusAndSeverity(Status.OPEN, Severity.CRITICAL);

        return MetricsResponse.builder()
                .totalIncidents(total)
                .byStatus(byStatus)
                .bySeverity(bySeverity)
                .byCategory(byCategory)
                .averageResolutionHours(avgHours != null ? avgHours : 0.0)
                .openCriticalIncidents(openCritical)
                .build();
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
                .actualResolution(incident.getActualResolution())
                .build();
    }
}