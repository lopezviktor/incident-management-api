package com.victorlopez.incident_api.config;

import com.victorlopez.incident_api.model.*;
import com.victorlopez.incident_api.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final IncidentRepository incidentRepository;

    @Override
    public void run(String... args) {
        if (incidentRepository.count() == 0) {
            log.info("Database is empty. Initializing with seed data...");
            initializeSeedData();
            log.info("Seed data initialization completed. {} incidents created.", incidentRepository.count());
        } else {
            log.info("Database already contains {} incidents. Skipping seed data initialization.", 
                    incidentRepository.count());
        }
    }

    private void initializeSeedData() {
        List<Incident> seedIncidents = List.of(
            // CRITICAL incidents
            Incident.builder()
                    .title("Production database cluster down")
                    .description("Primary PostgreSQL cluster is completely unresponsive. All customer-facing services are affected.")
                    .severity(Severity.CRITICAL)
                    .status(Status.RESOLVED)
                    .category(Category.DATABASE)
                    .reportedBy("ops-team")
                    .assignedTeam("Database Team")
                    .suggestedSolution("Failover to secondary cluster and investigate primary cluster failure")
                    .estimatedResolutionHours(1)
                    .aiConfidence(0.95)
                    .actualResolution("Failed over to secondary cluster. Primary cluster had disk failure on master node. Replaced disk and restored from backup.")
                    .resolvedAt(LocalDateTime.now().minusDays(2))
                    .build(),

            Incident.builder()
                    .title("Security breach detected in user authentication")
                    .description("Multiple failed login attempts detected from suspicious IP addresses. Potential brute force attack in progress.")
                    .severity(Severity.CRITICAL)
                    .status(Status.CLOSED)
                    .category(Category.SECURITY)
                    .reportedBy("security-monitor")
                    .assignedTeam("Security Team")
                    .suggestedSolution("Block suspicious IPs and force password reset for affected accounts")
                    .estimatedResolutionHours(2)
                    .aiConfidence(0.92)
                    .actualResolution("Blocked 15 malicious IP addresses, implemented additional rate limiting, forced password reset for 23 potentially compromised accounts.")
                    .resolvedAt(LocalDateTime.now().minusDays(1))
                    .build(),

            // HIGH severity incidents
            Incident.builder()
                    .title("API gateway returning 503 errors")
                    .description("Gateway service is returning 503 Service Unavailable errors for 30% of requests. Load balancer shows backend services are healthy.")
                    .severity(Severity.HIGH)
                    .status(Status.IN_PROGRESS)
                    .category(Category.BACKEND)
                    .reportedBy("monitoring-system")
                    .assignedTeam("Backend Team")
                    .suggestedSolution("Restart gateway service and check connection pool configuration")
                    .estimatedResolutionHours(3)
                    .aiConfidence(0.88)
                    .build(),

            Incident.builder()
                    .title("Payment processing system timeout")
                    .description("Payment gateway integration is timing out for credit card transactions. Customers cannot complete purchases.")
                    .severity(Severity.HIGH)
                    .status(Status.RESOLVED)
                    .category(Category.BACKEND)
                    .reportedBy("customer-support")
                    .assignedTeam("Payment Team")
                    .suggestedSolution("Increase timeout settings and check payment gateway status")
                    .estimatedResolutionHours(2)
                    .aiConfidence(0.90)
                    .actualResolution("Payment gateway provider confirmed temporary issues on their end. Implemented retry logic with exponential backoff.")
                    .resolvedAt(LocalDateTime.now().minusHours(6))
                    .build(),

            Incident.builder()
                    .title("Email notification service degraded")
                    .description("Email notifications are being delivered with significant delays. Queue size is growing rapidly.")
                    .severity(Severity.HIGH)
                    .status(Status.OPEN)
                    .category(Category.BACKEND)
                    .reportedBy("user-complaint")
                    .assignedTeam("Infrastructure Team")
                    .suggestedSolution("Scale up email service instances and investigate queue bottleneck")
                    .estimatedResolutionHours(4)
                    .aiConfidence(0.85)
                    .build(),

            // MEDIUM severity incidents
            Incident.builder()
                    .title("User dashboard loading slowly")
                    .description("User dashboard is taking 8-12 seconds to load. Database queries appear to be the bottleneck.")
                    .severity(Severity.MEDIUM)
                    .status(Status.IN_PROGRESS)
                    .category(Category.FRONTEND)
                    .reportedBy("user-feedback")
                    .assignedTeam("Frontend Team")
                    .suggestedSolution("Optimize database queries and implement caching for dashboard data")
                    .estimatedResolutionHours(8)
                    .aiConfidence(0.82)
                    .build(),

            Incident.builder()
                    .title("Search functionality returning partial results")
                    .description("Product search is missing some results. Elasticsearch cluster health shows yellow status.")
                    .severity(Severity.MEDIUM)
                    .status(Status.RESOLVED)
                    .category(Category.BACKEND)
                    .reportedBy("qa-team")
                    .assignedTeam("Search Team")
                    .suggestedSolution("Reindex Elasticsearch and check cluster configuration")
                    .estimatedResolutionHours(6)
                    .aiConfidence(0.87)
                    .actualResolution("Reindexed product catalog in Elasticsearch. Issue was caused by outdated mapping configuration.")
                    .resolvedAt(LocalDateTime.now().minusDays(3))
                    .build(),

            Incident.builder()
                    .title("Mobile app crashes on iOS 18")
                    .description("Mobile application is crashing consistently on iOS 18 devices when accessing the profile section.")
                    .severity(Severity.MEDIUM)
                    .status(Status.OPEN)
                    .category(Category.FRONTEND)
                    .reportedBy("app-store-reviews")
                    .assignedTeam("Mobile Team")
                    .suggestedSolution("Update iOS SDK and test compatibility with iOS 18")
                    .estimatedResolutionHours(12)
                    .aiConfidence(0.80)
                    .build(),

            Incident.builder()
                    .title("VPN connection intermittent failures")
                    .description("Remote employees experiencing intermittent VPN connection drops every 2-3 hours.")
                    .severity(Severity.MEDIUM)
                    .status(Status.RESOLVED)
                    .category(Category.NETWORK)
                    .reportedBy("employee-helpdesk")
                    .assignedTeam("Network Team")
                    .suggestedSolution("Update VPN server configuration and check for network interference")
                    .estimatedResolutionHours(5)
                    .aiConfidence(0.78)
                    .actualResolution("Updated VPN server keepalive settings and fixed routing table configuration.")
                    .resolvedAt(LocalDateTime.now().minusHours(18))
                    .build(),

            // LOW severity incidents
            Incident.builder()
                    .title("Documentation links returning 404")
                    .description("Several help documentation links in the application are returning 404 errors after recent website migration.")
                    .severity(Severity.LOW)
                    .status(Status.OPEN)
                    .category(Category.FRONTEND)
                    .reportedBy("content-team")
                    .assignedTeam("Documentation Team")
                    .suggestedSolution("Update all documentation links to point to new URL structure")
                    .estimatedResolutionHours(3)
                    .aiConfidence(0.95)
                    .build(),

            Incident.builder()
                    .title("CSS styling issue on user settings page")
                    .description("User settings page has minor CSS alignment issues on screens smaller than 768px width.")
                    .severity(Severity.LOW)
                    .status(Status.RESOLVED)
                    .category(Category.FRONTEND)
                    .reportedBy("design-team")
                    .assignedTeam("Frontend Team")
                    .suggestedSolution("Fix responsive CSS media queries for mobile devices")
                    .estimatedResolutionHours(2)
                    .aiConfidence(0.92)
                    .actualResolution("Updated CSS media queries and tested on various mobile devices.")
                    .resolvedAt(LocalDateTime.now().minusDays(5))
                    .build(),

            Incident.builder()
                    .title("Log rotation not working on staging server")
                    .description("Application logs are not being rotated properly on staging server, causing disk space to fill up slowly.")
                    .severity(Severity.LOW)
                    .status(Status.IN_PROGRESS)
                    .category(Category.BACKEND)
                    .reportedBy("devops-monitoring")
                    .assignedTeam("DevOps Team")
                    .suggestedSolution("Fix logrotate configuration and set up proper log retention policy")
                    .estimatedResolutionHours(1)
                    .aiConfidence(0.88)
                    .build(),

            Incident.builder()
                    .title("Backup verification script failing")
                    .description("Nightly backup verification script is failing with exit code 1. Backups are being created but not verified.")
                    .severity(Severity.LOW)
                    .status(Status.CLOSED)
                    .category(Category.DATABASE)
                    .reportedBy("backup-system")
                    .assignedTeam("Database Team")
                    .suggestedSolution("Debug verification script and fix path configuration")
                    .estimatedResolutionHours(2)
                    .aiConfidence(0.85)
                    .actualResolution("Fixed file path issues in verification script and updated database connection string.")
                    .resolvedAt(LocalDateTime.now().minusDays(4))
                    .build(),

            Incident.builder()
                    .title("API rate limiting too aggressive")
                    .description("Some legitimate users are being rate limited during peak hours. Current limit might be too conservative.")
                    .severity(Severity.LOW)
                    .status(Status.OPEN)
                    .category(Category.BACKEND)
                    .reportedBy("customer-support")
                    .assignedTeam("Backend Team")
                    .suggestedSolution("Analyze usage patterns and adjust rate limiting thresholds")
                    .estimatedResolutionHours(4)
                    .aiConfidence(0.75)
                    .build(),

            Incident.builder()
                    .title("SSL certificate expiring soon")
                    .description("SSL certificate for api.example.com will expire in 15 days. Need to renew before expiration.")
                    .severity(Severity.LOW)
                    .status(Status.OPEN)
                    .category(Category.SECURITY)
                    .reportedBy("security-scanner")
                    .assignedTeam("Security Team")
                    .suggestedSolution("Renew SSL certificate through certificate authority")
                    .estimatedResolutionHours(1)
                    .aiConfidence(0.98)
                    .build()
        );

        incidentRepository.saveAll(seedIncidents);
    }
}