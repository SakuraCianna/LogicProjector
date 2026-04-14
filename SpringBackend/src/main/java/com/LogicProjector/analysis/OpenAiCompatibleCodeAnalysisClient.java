package com.LogicProjector.analysis;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class OpenAiCompatibleCodeAnalysisClient implements AiCodeAnalysisClient {

    private static final String SYSTEM_PROMPT = """
            You identify supported Java algorithms for a teaching product.
            Return strict JSON with fields: algorithm, confidence, rationale.
            Allowed algorithm values: BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, BINARY_SEARCH, QUICK_SORT, MERGE_SORT, UNKNOWN.
            Confidence must be a number between 0 and 1.
            Classify the intended algorithm even if the snippet is partial or not fully runnable, as long as the structure, naming, and key operations strongly indicate one supported algorithm.
            Use UNKNOWN only when the intended supported algorithm is not reasonably inferable.
            """;

    private final AiChatClient aiChatClient;

    public OpenAiCompatibleCodeAnalysisClient(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    @Override
    public RecognitionResult analyze(String sourceCode) {
        JsonNode response = aiChatClient.createStructuredResponse(SYSTEM_PROMPT, sourceCode);
        String algorithmName = response.path("algorithm").asText("UNKNOWN");
        double confidence = response.path("confidence").asDouble(0.0);
        String rationale = response.path("rationale").asText("No rationale provided");

        return new RecognitionResult(toDetectedAlgorithm(algorithmName), confidence, rationale);
    }

    private DetectedAlgorithm toDetectedAlgorithm(String algorithmName) {
        try {
            return DetectedAlgorithm.valueOf(algorithmName.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return DetectedAlgorithm.UNKNOWN;
        }
    }
}
