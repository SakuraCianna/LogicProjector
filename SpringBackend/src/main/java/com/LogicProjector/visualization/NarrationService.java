package com.LogicProjector.visualization;

import org.springframework.stereotype.Service;

import com.LogicProjector.analysis.AiChatClient;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class NarrationService {

    private static final String SYSTEM_PROMPT = """
            You write short teaching summaries for Java algorithm walkthroughs.
            Return strict JSON with one field: summary.
            Keep the summary to 1 sentence, concrete, and classroom-friendly.
            Do not mention unsupported behavior or implementation uncertainty.
            """;

    private final AiChatClient aiChatClient;

    public NarrationService(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    public NarrationResult createNarration(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        try {
            JsonNode response = aiChatClient.createStructuredResponse(
                    SYSTEM_PROMPT,
                    "Algorithm: " + algorithm.name() + "\nSteps: " + payload.steps().size() + "\nSource code:\n" + sourceCode);
            String summary = response.path("summary").asText("").trim();
            if (!summary.isEmpty()) {
                return new NarrationResult(summary);
            }
        } catch (RuntimeException ignored) {
            // Fall back to deterministic copy if narration generation fails.
        }

        String summary = switch (algorithm) {
            case QUICK_SORT -> "Quick sort picks a pivot, partitions the array, and recursively sorts both sides.";
            case BINARY_SEARCH -> "Binary search keeps halving the search range until the target position is isolated.";
            case MERGE_SORT -> "Merge sort splits the array, sorts each half, and merges the results in order.";
            case BUBBLE_SORT -> "Bubble sort repeatedly compares adjacent values and swaps larger values to the right.";
            case SELECTION_SORT -> "Selection sort finds the smallest remaining value and places it into the sorted prefix.";
            case INSERTION_SORT -> "Insertion sort inserts each next value into the already sorted left side.";
            default -> "The algorithm is explained step by step through data structure changes.";
        };
        return new NarrationResult(summary);
    }
}
