package com.victorlopez.incident_api.dto;

import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IncidentResponse {

    private UUID id;
    private String title;
    private String description;
    private Severity severity;
    private Category category;
    private Status status;
    private String reportedBy;
    private String assignedTeam;
    private String suggestedSolution;
    private Integer estimatedResolutionHours;
    private Double aiConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}