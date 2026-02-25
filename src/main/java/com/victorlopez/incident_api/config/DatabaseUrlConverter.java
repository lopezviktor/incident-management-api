package com.victorlopez.incident_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DatabaseUrlConverter implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String databaseUrl = environment.getProperty("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Convert Railway's postgresql:// to Spring Boot's jdbc:postgresql://
            String jdbcUrl = "jdbc:" + databaseUrl;
            
            log.info("Converting DATABASE_URL from Railway format to JDBC format");
            log.debug("Original: {}", databaseUrl);
            log.debug("Converted: {}", jdbcUrl);
            
            Map<String, Object> jdbcProperties = new HashMap<>();
            jdbcProperties.put("JDBC_DATABASE_URL", jdbcUrl);
            
            MapPropertySource jdbcPropertySource = new MapPropertySource("jdbcConversion", jdbcProperties);
            environment.getPropertySources().addFirst(jdbcPropertySource);
        }
    }
}