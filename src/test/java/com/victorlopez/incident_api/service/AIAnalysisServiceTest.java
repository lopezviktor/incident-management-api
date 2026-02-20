package com.victorlopez.incident_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.victorlopez.incident_api.dto.AIAnalysisResult;
import com.victorlopez.incident_api.model.Category;
import com.victorlopez.incident_api.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIAnalysisService.
 *
 * We mock the Spring AI ChatClient to avoid:
 * - Making real API calls during tests (costs money)
 * - Depending on external service availability
 * - Slow test execution
 *
 * Testing strategy:
 * 1. Mock ChatClient responses with realistic JSON
 * 2. Verify the service correctly parses OpenAI responses
 * 3. Test error handling (invalid JSON, missing fields, API failures)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Analysis Service Tests")
class AIAnalysisServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    private AIAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        aiAnalysisService = new AIAnalysisService(chatClient);
    }

    @Test
    @DisplayName("Should successfully analyze a database incident")
    void shouldAnalyzeDatabaseIncident() {
        // Given
        String title = "Database connection pool exhausted";
        String description = "Production database refusing connections. Connection pool showing 100% utilization.";

        String mockAIResponse = """
            {
                "severity": "CRITICAL",
                "category": "DATABASE",
                "assignedTeam": "Database Team",
                "suggestedSolution": "Increase connection pool size in application.properties. Check for connection leaks. Review slow queries consuming connections.",
                "estimatedResolutionHours": 2,
                "confidence": 0.92
            }
            """;

        // Mock the fluent API chain
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockAIResponse);

        // When
        AIAnalysisResult result = aiAnalysisService.analyzeIncident(title, description);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.category()).isEqualTo(Category.DATABASE);
        assertThat(result.assignedTeam()).isEqualTo("Database Team");
        assertThat(result.suggestedSolution()).contains("connection pool");
        assertThat(result.estimatedResolutionHours()).isEqualTo(2);
        assertThat(result.confidence()).isEqualTo(0.92);

        // Verify interactions
        verify(chatClient).prompt();
        verify(requestSpec).system(contains("IT incident analyst"));
        verify(requestSpec).user(contains(title));
        verify(requestSpec).user(contains(description));
    }

    @Test
    @DisplayName("Should analyze a frontend security incident")
    void shouldAnalyzeFrontendSecurityIncident() {
        // Given
        String title = "XSS vulnerability in user input";
        String description = "User-submitted content is not being sanitized, allowing script injection.";

        String mockAIResponse = """
            {
                "severity": "HIGH",
                "category": "SECURITY",
                "assignedTeam": "Security Team",
                "suggestedSolution": "Implement input sanitization using DOMPurify. Add Content Security Policy headers. Review all user input fields.",
                "estimatedResolutionHours": 4,
                "confidence": 0.88
            }
            """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockAIResponse);

        // When
        AIAnalysisResult result = aiAnalysisService.analyzeIncident(title, description);

        // Then
        assertThat(result.severity()).isEqualTo(Severity.HIGH);
        assertThat(result.category()).isEqualTo(Category.SECURITY);
        assertThat(result.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should handle low severity incidents")
    void shouldHandleLowSeverityIncident() {
        // Given
        String title = "Button alignment issue on mobile";
        String description = "Submit button is slightly misaligned on iPhone SE";

        String mockAIResponse = """
            {
                "severity": "LOW",
                "category": "FRONTEND",
                "assignedTeam": "Frontend Team",
                "suggestedSolution": "Adjust CSS flexbox alignment. Test on multiple mobile devices.",
                "estimatedResolutionHours": 1,
                "confidence": 0.95
            }
            """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockAIResponse);

        // When
        AIAnalysisResult result = aiAnalysisService.analyzeIncident(title, description);

        // Then
        assertThat(result.severity()).isEqualTo(Severity.LOW);
        assertThat(result.estimatedResolutionHours()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw exception when OpenAI returns invalid JSON")
    void shouldThrowExceptionOnInvalidJSON() {
        // Given
        String title = "Some incident";
        String description = "Some description";
        String invalidJSON = "This is not valid JSON";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(invalidJSON);

        // When / Then
        assertThatThrownBy(() -> aiAnalysisService.analyzeIncident(title, description))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse AI response");
    }

    @Test
    @DisplayName("Should throw exception when required fields are missing")
    void shouldThrowExceptionOnMissingFields() {
        // Given
        String title = "Some incident";
        String description = "Some description";

        // Missing 'severity' field
        String incompleteJSON = """
            {
                "category": "BACKEND",
                "assignedTeam": "Backend Team",
                "suggestedSolution": "Fix it",
                "estimatedResolutionHours": 2,
                "confidence": 0.8
            }
            """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(incompleteJSON);

        // When / Then
        assertThatThrownBy(() -> aiAnalysisService.analyzeIncident(title, description))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw exception when ChatClient fails")
    void shouldThrowExceptionWhenChatClientFails() {
        // Given
        String title = "Some incident";
        String description = "Some description";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("OpenAI API is down"));

        // When / Then
        assertThatThrownBy(() -> aiAnalysisService.analyzeIncident(title, description))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenAI API is down");
    }

    @Test
    @DisplayName("Should validate confidence score is between 0 and 1")
    void shouldValidateConfidenceScore() {
        // Given
        String title = "Some incident";
        String description = "Some description";

        String invalidConfidenceJSON = """
        {
            "severity": "MEDIUM",
            "category": "BACKEND",
            "assignedTeam": "Backend Team",
            "suggestedSolution": "Fix it",
            "estimatedResolutionHours": 2,
            "confidence": 1.5
        }
        """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(invalidConfidenceJSON);

        // When / Then
        assertThatThrownBy(() -> aiAnalysisService.analyzeIncident(title, description))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to analyze incident")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Confidence must be between 0.0 and 1.0");
    }
}