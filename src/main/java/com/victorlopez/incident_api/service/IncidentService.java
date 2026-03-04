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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AIAnalysisService aiAnalysisService;

    public IncidentResponse createIncident(CreateIncidentRequest request) {
        log.info("Creating incident: {}", request.getTitle());

        AIAnalysisResult aiAnalysis = aiAnalysisService.analyzeIncident(
                request.getTitle(),
                request.getDescription()
        );

        log.info("AI analysis - Severity: {}, Category: {}",
                aiAnalysis.severity(), aiAnalysis.category());

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .reportedBy(request.getReportedBy())
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

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(Status status, Severity severity, Pageable pageable) {
        Page<Incident> incidents;

        if (status != null && severity != null) {
            incidents = incidentRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            incidents = incidentRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            incidents = incidentRepository.findBySeverity(severity, pageable);
        } else {
            incidents = incidentRepository.findAll(pageable);
        }

        return incidents.map(this::mapToResponse);
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

    public List<IncidentResponse> findSimilarIncidents(String description, UUID excludeId) {
        log.info("Finding similar incidents for description: {} (excluding: {})", description, excludeId);
        
        String keywords = extractKeywords(description);
        log.debug("Extracted keywords: {}", keywords);
        
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> similarIncidents = allIncidents.stream()
                .filter(incident -> !incident.getId().equals(excludeId))
                .filter(incident -> containsAnyKeyword(incident, keywords))
                .toList();
        
        return similarIncidents.stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    private boolean containsAnyKeyword(Incident incident, String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return false;
        }
        
        String[] keywordArray = keywords.toLowerCase().split("\\s+");
        String titleLower = incident.getTitle().toLowerCase();
        String descriptionLower = incident.getDescription().toLowerCase();
        
        for (String keyword : keywordArray) {
            if (titleLower.contains(keyword) || descriptionLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractKeywords(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "";
        }
        
        return description.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\b(the|is|are|was|were|and|or|in|on|at|to|for|of|with|by|from|a|an|that|this|it)\\b", "")
                .trim()
                .replaceAll("\\s+", " ");
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