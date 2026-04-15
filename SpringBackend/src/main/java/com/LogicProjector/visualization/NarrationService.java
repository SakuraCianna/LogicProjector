package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.LogicProjector.analysis.AiChatClient;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class NarrationService {

    private static final String SYSTEM_PROMPT = """
            You write short teaching summaries and step narration for Java algorithm walkthroughs.
            Return strict JSON with fields: summary, stepNarrations.
            Keep the summary to 1 sentence, concrete, and classroom-friendly.
            stepNarrations must be an array of exactly one short sentence per step, matching the input step count and order.
            Preserve the deterministic meaning of each step. Improve clarity, but do not invent state changes that are not present.
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
                    buildNarrationPrompt(algorithm, payload, sourceCode));
            String summary = response.path("summary").asText("").trim();
            List<String> stepNarrations = extractStepNarrations(response, payload.steps().size());
            if (!summary.isEmpty()) {
                return new NarrationResult(summary, stepNarrations);
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
        return new NarrationResult(summary, List.of());
    }

    private List<String> extractStepNarrations(JsonNode response, int stepCount) {
        JsonNode stepNarrationsNode = response.path("stepNarrations");
        if (!stepNarrationsNode.isArray() || stepNarrationsNode.size() != stepCount) {
            return List.of();
        }

        List<String> stepNarrations = new ArrayList<>();
        for (JsonNode stepNarrationNode : stepNarrationsNode) {
            String narration = stepNarrationNode.asText("").trim();
            if (narration.isEmpty()) {
                return List.of();
            }
            stepNarrations.add(narration);
        }
        return List.copyOf(stepNarrations);
    }

    private String buildNarrationPrompt(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Algorithm: ").append(algorithm.name()).append("\n");
        prompt.append("Step count: ").append(payload.steps().size()).append("\n");
        prompt.append("Steps:\n");

        for (int index = 0; index < payload.steps().size(); index++) {
            VisualizationStep step = payload.steps().get(index);
            prompt.append(index + 1)
                    .append(". title=")
                    .append(step.title())
                    .append("; currentNarration=")
                    .append(step.narration())
                    .append("\n");
        }

        prompt.append("Source code:\n").append(sourceCode);
        return prompt.toString();
    }
}
