package com.victorlopez.incident_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MetricsResponse {

    private long totalIncidents;
    private Map<String, Long> byStatus;
    private Map<String, Long> bySeverity;
    private Map<String, Long> byCategory;
    private double averageResolutionHours;
    private long openCriticalIncidents;
}