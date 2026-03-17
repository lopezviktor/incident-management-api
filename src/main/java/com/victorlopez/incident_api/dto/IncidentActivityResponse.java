package com.victorlopez.incident_api.dto;

import com.victorlopez.incident_api.model.IncidentActivityAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IncidentActivityResponse {

    private UUID id;
    private UUID incidentId;
    private IncidentActivityAction action;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;
}
