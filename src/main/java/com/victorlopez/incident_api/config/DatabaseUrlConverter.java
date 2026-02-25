package com.victorlopez.incident_api.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DatabaseUrlConverter {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(Environment environment, DataSourceProperties properties) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        
        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Convert Render/Railway's postgresql:// to Spring Boot's jdbc:postgresql://
            String jdbcUrl = "jdbc:" + databaseUrl;
            
            log.info("Converting DATABASE_URL from cloud provider format to JDBC format");
            log.info("Original: postgresql://...");
            log.info("Converted: jdbc:postgresql://...");
            
            HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setAutoCommit(false);

            // Set connection pool properties
            dataSource.setMaximumPoolSize(Integer.parseInt(environment.getProperty("DB_POOL_SIZE", "10")));
            dataSource.setMinimumIdle(Integer.parseInt(environment.getProperty("DB_POOL_MIN_IDLE", "2")));
            dataSource.setConnectionTimeout(Long.parseLong(environment.getProperty("DB_CONNECTION_TIMEOUT", "30000")));
            dataSource.setIdleTimeout(Long.parseLong(environment.getProperty("DB_IDLE_TIMEOUT", "600000")));
            dataSource.setMaxLifetime(Long.parseLong(environment.getProperty("DB_MAX_LIFETIME", "1800000")));

            log.info("Successfully configured DataSource with converted JDBC URL");
            return dataSource;
        } else {
            log.info("DATABASE_URL not in postgresql:// format, using default configuration");
            HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
            dataSource.setAutoCommit(false);
            return dataSource;
        }
    }
}