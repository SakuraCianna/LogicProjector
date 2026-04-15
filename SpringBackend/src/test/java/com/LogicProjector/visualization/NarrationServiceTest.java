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
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> {
            var response = objectMapper.createObjectNode()
                    .put("summary", "Quick sort selects a pivot and recursively orders both partitions.");
            response.putArray("stepNarrations")
                    .add("Choose the pivot value before partitioning.")
                    .add("Move values to the correct side of the pivot.");
            return response;
        };
        NarrationService service = new NarrationService(aiChatClient);

        NarrationResult result = service.createNarration(
                DetectedAlgorithm.QUICK_SORT,
                new VisualizationPayload("QUICK_SORT", List.of(
                        new VisualizationStep("Step 1", "Original 1", List.of(5, 1, 4), List.of(0), List.of(3)),
                        new VisualizationStep("Step 2", "Original 2", List.of(1, 5, 4), List.of(1), List.of(4)))),
                "class QuickSort {}"
        );

        assertThat(result.summary()).contains("pivot");
        assertThat(result.stepNarrations()).containsExactly(
                "Choose the pivot value before partitioning.",
                "Move values to the correct side of the pivot.");
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
        assertThat(result.stepNarrations()).isEmpty();
    }

    @Test
    void shouldIgnoreAiStepNarrationsWhenCountDoesNotMatchSteps() {
        AiChatClient aiChatClient = (systemPrompt, userPrompt) -> {
            var response = objectMapper.createObjectNode()
                    .put("summary", "Quick sort picks a pivot.");
            response.putArray("stepNarrations")
                    .add("Only one narration returned");
            return response;
        };
        NarrationService service = new NarrationService(aiChatClient);

        NarrationResult result = service.createNarration(
                DetectedAlgorithm.QUICK_SORT,
                new VisualizationPayload("QUICK_SORT", List.of(
                        new VisualizationStep("Step 1", "Original 1", List.of(5, 1, 4), List.of(0), List.of(3)),
                        new VisualizationStep("Step 2", "Original 2", List.of(1, 5, 4), List.of(1), List.of(4)))),
                "class QuickSort {}"
        );

        assertThat(result.stepNarrations()).isEmpty();
    }
}
