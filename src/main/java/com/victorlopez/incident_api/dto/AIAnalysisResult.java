package com.victorlopez.incident_api.dto;

import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Severity;

/**
 * DTO representing the AI analysis result from OpenAI.
 *
 * This structure maps directly to the JSON response we expect from the AI model.
 * The AI will analyze an incident's title and description, then provide:
 * - Classification (severity and category)
 * - Team assignment recommendation
 * - Suggested solution
 * - Estimated resolution time
 * - Confidence score (0.0 to 1.0)
 *
 * Example JSON from OpenAI:
 * {
 *   "severity": "HIGH",
 *   "category": "DATABASE",
 *   "assignedTeam": "Database Team",
 *   "suggestedSolution": "Check connection pool settings...",
 *   "estimatedResolutionHours": 4,
 *   "confidence": 0.85
 * }
 */
public record AIAnalysisResult(
        Severity severity,
        Category category,
        String assignedTeam,
        String suggestedSolution,
        Integer estimatedResolutionHours,
        Double confidence
) {

    /**
     * Validates that all required fields are present and valid.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public AIAnalysisResult {
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (assignedTeam == null || assignedTeam.isBlank()) {
            throw new IllegalArgumentException("Assigned team cannot be null or blank");
        }
        if (suggestedSolution == null || suggestedSolution.isBlank()) {
            throw new IllegalArgumentException("Suggested solution cannot be null or blank");
        }
        if (estimatedResolutionHours == null || estimatedResolutionHours < 0) {
            throw new IllegalArgumentException("Estimated resolution hours must be a positive number");
        }
        if (confidence == null || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
}