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
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByStatus(Status status);

    List<Incident> findBySeverity(Severity severity);

    List<Incident> findByStatusAndSeverity(Status status, Severity severity);

    Page<Incident> findByStatus(Status status, Pageable pageable);

    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    Page<Incident> findByStatusAndSeverity(Status status, Severity severity, Pageable pageable);

    long countByStatus(Status status);

    long countBySeverity(Severity severity);

    long countByCategory(Category category);

    long countByStatusAndSeverity(Status status, Severity severity);

    @Query("SELECT AVG(i.estimatedResolutionHours) FROM Incident i WHERE i.estimatedResolutionHours IS NOT NULL")
    Double findAverageResolutionHours();

    @Query("SELECT DISTINCT i FROM Incident i WHERE i.id != :excludeId AND " +
           "(:keywords IS NULL OR :keywords = '' OR " +
           "LOWER(i.title) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :keywords, '%')))")
    List<Incident> findSimilarIncidents(@Param("keywords") String keywords, @Param("excludeId") UUID excludeId);
}