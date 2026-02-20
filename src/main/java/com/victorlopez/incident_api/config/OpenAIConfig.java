package com.victorlopez.incident_api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI integration using Spring AI.
 *
 * Model: gpt-4o-mini (cost-effective, fast, sufficient for classification tasks)
 * Temperature: 0.3 (low for consistent, deterministic responses)
 *
 * Spring AI handles:
 * - HTTP client setup
 * - Authentication with API key
 * - Request/response serialization
 * - Error handling and retries
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/">Spring AI Documentation</a>
 */
@Configuration
public class OpenAIConfig {

    /**
     * Creates a ChatClient bean for interacting with OpenAI's API.
     *
     * The ChatClient is auto-configured by Spring Boot based on properties:
     * - spring.ai.openai.api-key
     * - spring.ai.openai.chat.options.model
     * - spring.ai.openai.chat.options.temperature
     *
     * This bean can be injected into services that need AI capabilities.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}