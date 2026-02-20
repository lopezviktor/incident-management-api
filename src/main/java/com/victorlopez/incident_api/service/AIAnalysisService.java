package com.victorlopez.incident_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.victorlopez.incident_api.dto.AIAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for analyzing incidents using OpenAI's GPT-4o-mini model.
 *
 * This service sends incident details to OpenAI and receives structured analysis including:
 * - Severity classification (CRITICAL/HIGH/MEDIUM/LOW)
 * - Category classification (BACKEND/FRONTEND/DATABASE/SECURITY/NETWORK)
 * - Team assignment recommendation
 * - Suggested solution based on the incident description
 * - Estimated resolution time in hours
 * - AI confidence score (0.0 to 1.0)
 *
 * The service uses Spring AI's ChatClient which handles:
 * - HTTP communication with OpenAI API
 * - Authentication with API key
 * - Request/response serialization
 * - Error handling and retries
 *
 * Cost optimization:
 * - Uses gpt-4o-mini (cheaper than gpt-4)
 * - Low temperature (0.3) for consistent responses
 * - Structured JSON output to minimize tokens
 */
@Service
public class AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * System prompt that defines the AI's role and expected output format.
     *
     * Key elements:
     * - Role definition: expert IT incident analyst
     * - Output format: strict JSON schema
     * - Classification rules: clear criteria for severity and category
     * - Constraints: must respond in valid JSON only
     */
    private static final String SYSTEM_PROMPT = """
        You are an expert IT incident analyst with deep knowledge of software systems,
        infrastructure, and security. Your job is to analyze incident reports and provide
        structured analysis.
        
        Classify incidents based on:
        
        SEVERITY:
        - CRITICAL: System down, data loss, security breach, affects all users
        - HIGH: Major feature broken, affects many users, security vulnerability
        - MEDIUM: Minor feature broken, affects some users, workaround available
        - LOW: Cosmetic issue, minor inconvenience, affects few users
        
        CATEGORY:
        - BACKEND: API, database, server, microservices, business logic
        - FRONTEND: UI, UX, client-side rendering, browser issues
        - DATABASE: Data integrity, queries, migrations, performance
        - SECURITY: Authentication, authorization, vulnerabilities, data exposure
        - NETWORK: Connectivity, latency, DNS, load balancing
        
        ASSIGNED TEAM suggestions:
        - "Backend Team" for BACKEND category
        - "Frontend Team" for FRONTEND category
        - "Database Team" for DATABASE category
        - "Security Team" for SECURITY category
        - "DevOps Team" for NETWORK category
        
        You must respond ONLY with valid JSON in this exact format:
        {
            "severity": "CRITICAL|HIGH|MEDIUM|LOW",
            "category": "BACKEND|FRONTEND|DATABASE|SECURITY|NETWORK",
            "assignedTeam": "appropriate team name",
            "suggestedSolution": "detailed solution with actionable steps",
            "estimatedResolutionHours": number between 1 and 72,
            "confidence": number between 0.0 and 1.0
        }
        
        Do not include any text outside the JSON. Do not use markdown code blocks.
        """;

    public AIAnalysisService(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes an incident using OpenAI and returns structured analysis.
     *
     * @param title incident title (short summary)
     * @param description detailed incident description
     * @return AIAnalysisResult with classification and recommendations
     * @throws RuntimeException if OpenAI API fails or returns invalid response
     */
    public AIAnalysisResult analyzeIncident(String title, String description) {
        log.info("Analyzing incident: {}", title);

        try {
            // Build the user message with incident details
            String userMessage = String.format("""
                Incident Title: %s
                
                Description: %s
                
                Provide your analysis in JSON format.
                """, title, description);

            // Call OpenAI using Spring AI ChatClient
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            log.debug("OpenAI raw response: {}", response);

            // Parse JSON response to AIAnalysisResult
            AIAnalysisResult result = parseAIResponse(response);

            log.info("Analysis complete - Severity: {}, Category: {}, Confidence: {}",
                    result.severity(), result.category(), result.confidence());

            return result;

        } catch (Exception e) {
            log.error("Failed to analyze incident with AI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze incident: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the JSON response from OpenAI into AIAnalysisResult.
     *
     * The AIAnalysisResult record constructor includes validation,
     * so if the response is missing required fields or has invalid values,
     * an IllegalArgumentException will be thrown.
     *
     * @param jsonResponse raw JSON string from OpenAI
     * @return parsed and validated AIAnalysisResult
     * @throws RuntimeException if JSON is invalid or parsing fails
     */
    private AIAnalysisResult parseAIResponse(String jsonResponse) {
        try {
            // Clean the response (sometimes OpenAI adds markdown code blocks)
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            // Parse JSON to AIAnalysisResult
            // The record's compact constructor will validate the fields
            return objectMapper.readValue(cleanJson, AIAnalysisResult.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", jsonResponse);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}