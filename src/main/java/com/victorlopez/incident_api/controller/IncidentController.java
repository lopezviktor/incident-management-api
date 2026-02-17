package com.victorlopez.incident_api.controller;

import com.victorlopez.incident_api.dto.CreateIncidentRequest;
import com.victorlopez.incident_api.dto.IncidentResponse;
import com.victorlopez.incident_api.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable UUID id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }
}