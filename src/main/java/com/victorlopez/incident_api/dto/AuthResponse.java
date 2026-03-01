package com.victorlopez.incident_api.dto;

import com.victorlopez.incident_api.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String username;
    private Role role;
}
