package com.LogicProjector.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.LogicProjector.http.WebClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DeepSeekChatClient implements AiChatClient {

    private final WebClient webClient;
    private final String model;
    private final String configuredApiKey;
    private final String apiKeyEnv;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    @Autowired
    public DeepSeekChatClient(@Value("${pas.ai.base-url}") String baseUrl,
            @Value("${pas.ai.model}") String model,
            @Value("${pas.ai.deepseek-api-key:}") String configuredApiKey,
            @Value("${pas.ai.api-key-env:DEEPSEEK_API_KEY}") String apiKeyEnv,
            ObjectMapper objectMapper,
            WebClientFactory webClientFactory,
            @Value("${pas.ai.timeout-seconds:30}") long timeoutSeconds) {
        this(webClientFactory.forBaseUrl(baseUrl), model, configuredApiKey, apiKeyEnv, objectMapper, Duration.ofSeconds(timeoutSeconds));
    }

    DeepSeekChatClient(WebClient webClient,
            String model,
            String configuredApiKey,
            String apiKeyEnv,
            ObjectMapper objectMapper,
            Duration timeout) {
        this.webClient = webClient;
        this.model = model;
        this.configuredApiKey = configuredApiKey;
        this.apiKeyEnv = apiKeyEnv;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
    }

    @Override
    public JsonNode createStructuredResponse(String systemPrompt, String userPrompt) {
        String apiKey = configuredApiKey == null || configuredApiKey.isBlank()
                ? System.getenv(apiKeyEnv)
                : configuredApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing DeepSeek API key. Checked pas.ai.deepseek-api-key and environment variable: " + apiKeyEnv);
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        JsonNode response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(timeout);

        if (response == null) {
            throw new IllegalStateException("DeepSeek returned an empty response");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("DeepSeek response did not include message content");
        }

        try {
            return objectMapper.readTree(stripMarkdownCodeFence(contentNode.asText()));
        } catch (Exception exception) {
            throw new IllegalStateException("DeepSeek returned invalid JSON content", exception);
        }
    }

    private String stripMarkdownCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                return trimmed.substring(firstLineBreak + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
