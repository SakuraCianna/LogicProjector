package com.LogicProjector.generation.api;

import com.fasterxml.jackson.databind.JsonNode;

public record GenerationTaskResponse(
        Long id,
        String status,
        String language,
        String detectedAlgorithm,
        String summary,
        double confidenceScore,
        JsonNode visualizationPayload,
        String errorMessage,
        int creditsCharged,
        String sourceCode
) {
}
