package com.victorlopez.incident_api.dto;

import com.victorlopez.incident_api.model.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private Status status;

    private String actualResolution;
}