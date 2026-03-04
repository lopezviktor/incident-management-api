package com.victorlopez.incident_api.controller;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.dto.MetricsResponse;
import com.victorlopez.incident_api.dto.UpdateStatusRequest;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import com.victorlopez.incident_api.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Management", description = "APIs for managing IT incidents with AI-powered analysis")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @Operation(summary = "Create a new incident", description = "Creates a new incident with AI-powered analysis for severity, category, and suggested solution")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incident created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all incidents", description = "Retrieves incidents with optional filtering by status and severity. Supports pagination via page, size, and sort query parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidents retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @Parameter(description = "Filter by incident status") @RequestParam(required = false) Status status,
            @Parameter(description = "Filter by incident severity") @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<IncidentResponse> incidents = incidentService.getAllIncidents(status, severity, pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID", description = "Retrieves a specific incident by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Incident not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<IncidentResponse> getIncidentById(
            @Parameter(description = "Unique identifier of the incident") @PathVariable UUID id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update incident status", description = "Updates the status of an incident and optionally sets resolution details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Incident not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<IncidentResponse> updateStatus(
            @Parameter(description = "Unique identifier of the incident") @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        IncidentResponse response = incidentService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get incident metrics", description = "Retrieves comprehensive metrics and statistics about all incidents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<MetricsResponse> getMetrics() {
        MetricsResponse metrics = incidentService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/similar")
    @Operation(summary = "Find similar incidents", description = "Finds incidents similar to the provided description using keyword matching")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Similar incidents retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid description parameter"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<IncidentResponse>> getSimilarIncidents(
            @Parameter(description = "Description to search for similar incidents", required = true) 
            @RequestParam(required = true) String description,
            @Parameter(description = "ID of incident to exclude from results") 
            @RequestParam(required = false) UUID excludeId) {
        
        if (!StringUtils.hasText(description)) {
            return ResponseEntity.badRequest().build();
        }
        
        UUID actualExcludeId = excludeId != null ? excludeId : UUID.randomUUID();
        List<IncidentResponse> similarIncidents = incidentService.findSimilarIncidents(description, actualExcludeId);
        return ResponseEntity.ok(similarIncidents);
    }
}