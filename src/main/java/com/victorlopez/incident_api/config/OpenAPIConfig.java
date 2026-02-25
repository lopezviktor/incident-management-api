package com.victorlopez.incident_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Incident Management API")
                        .version("1.0.0")
                        .description("A comprehensive REST API for managing IT incidents with AI-powered analysis and similarity search capabilities")
                        .contact(new Contact()
                                .name("Victor Lopez")
                                .email("victor.lopez@example.com")
                                .url("https://github.com/lopezviktor/incident-management-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}