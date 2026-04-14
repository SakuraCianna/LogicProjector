package com.LogicProjector.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class OpenAiCompatibleCodeAnalysisClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapStructuredModelResponseToRecognitionResult() {
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> objectMapper.createObjectNode()
                .put("algorithm", "QUICK_SORT")
                .put("confidence", 0.94)
                .put("rationale", "Partition and pivot flow detected");

        OpenAiCompatibleCodeAnalysisClient client = new OpenAiCompatibleCodeAnalysisClient(aiChatClient);

        RecognitionResult result = client.analyze("public class QuickSort { } ");

        assertThat(result.algorithm()).isEqualTo(DetectedAlgorithm.QUICK_SORT);
        assertThat(result.confidence()).isEqualTo(0.94);
        assertThat(result.rationale()).contains("pivot");
    }

    @Test
    void shouldReturnUnknownWhenModelReturnsUnsupportedAlgorithmName() {
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> objectMapper.createObjectNode()
                .put("algorithm", "TOPOLOGICAL_SORT")
                .put("confidence", 0.72)
                .put("rationale", "Unsupported for MVP");

        OpenAiCompatibleCodeAnalysisClient client = new OpenAiCompatibleCodeAnalysisClient(aiChatClient);

        RecognitionResult result = client.analyze("graph code");

        assertThat(result.algorithm()).isEqualTo(DetectedAlgorithm.UNKNOWN);
        assertThat(result.confidence()).isEqualTo(0.72);
    }
}
