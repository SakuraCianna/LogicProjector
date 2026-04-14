package com.LogicProjector.visualization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.LogicProjector.analysis.AiChatClient;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;

class NarrationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseModelGeneratedSummaryWhenAvailable() {
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> objectMapper.createObjectNode()
                .put("summary", "Quick sort selects a pivot and recursively orders both partitions.");
        NarrationService service = new NarrationService(aiChatClient);

        NarrationResult result = service.createNarration(
                DetectedAlgorithm.QUICK_SORT,
                new VisualizationPayload("QUICK_SORT", List.of()),
                "class QuickSort {}"
        );

        assertThat(result.summary()).contains("pivot");
    }

    @Test
    void shouldFallbackToDefaultSummaryWhenModelCallFails() {
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> {
            throw new IllegalStateException("missing api key");
        };
        NarrationService service = new NarrationService(aiChatClient);

        NarrationResult result = service.createNarration(
                DetectedAlgorithm.BINARY_SEARCH,
                new VisualizationPayload("BINARY_SEARCH", List.of()),
                "class BinarySearch {}"
        );

        assertThat(result.summary()).contains("halving the search range");
    }
}
