package com.victorlopez.incident_api.repository;

import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Incident;
import com.victorlopez.incident_api.model.Severity;
import com.victorlopez.incident_api.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    // --- kept for repository tests ---
    List<Incident> findByStatus(Status status);
    List<Incident> findBySeverity(Severity severity);
    List<Incident> findByStatusAndSeverity(Status status, Severity severity);

    // --- non-archived pageable queries (used by service) ---
    Optional<Incident> findByIdAndArchivedFalse(UUID id);

    Page<Incident> findByArchivedFalse(Pageable pageable);
    Page<Incident> findByStatusAndArchivedFalse(Status status, Pageable pageable);
    Page<Incident> findBySeverityAndArchivedFalse(Severity severity, Pageable pageable);
    Page<Incident> findByStatusAndSeverityAndArchivedFalse(Status status, Severity severity, Pageable pageable);

    // --- user-scoped pageable queries (Step 5) ---
    Page<Incident> findByReportedByAndArchivedFalse(String reportedBy, Pageable pageable);
    Page<Incident> findByStatusAndReportedByAndArchivedFalse(Status status, String reportedBy, Pageable pageable);
    Page<Incident> findBySeverityAndReportedByAndArchivedFalse(Severity severity, String reportedBy, Pageable pageable);
    Page<Incident> findByStatusAndSeverityAndReportedByAndArchivedFalse(Status status, Severity severity, String reportedBy, Pageable pageable);

    // --- similarity search (non-archived) ---
    List<Incident> findByArchivedFalse();

    @Query("SELECT DISTINCT i FROM Incident i WHERE i.archived = false AND i.id != :excludeId AND " +
           "(:keywords IS NULL OR :keywords = '' OR " +
           "LOWER(i.title) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :keywords, '%')))")
    List<Incident> findSimilarIncidents(@Param("keywords") String keywords, @Param("excludeId") UUID excludeId);

    // --- count methods (non-archived) ---
    long countByArchivedFalse();
    long countByStatusAndArchivedFalse(Status status);
    long countBySeverityAndArchivedFalse(Severity severity);
    long countByCategoryAndArchivedFalse(Category category);
    long countByStatusAndSeverityAndArchivedFalse(Status status, Severity severity);

    @Query("SELECT AVG(i.estimatedResolutionHours) FROM Incident i WHERE i.estimatedResolutionHours IS NOT NULL AND i.archived = false")
    Double findAverageResolutionHours();
}
