package com.LogicProjector.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class OpenAiCompatibleCodeAnalysisClient implements AiCodeAnalysisClient {

    private static final String SYSTEM_PROMPT = """
            You identify supported Java, C, and C++ algorithms for a teaching product.
            Return strict JSON with fields: algorithm, confidence, rationale, lineMappings.
            Allowed algorithm values: BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, BINARY_SEARCH, QUICK_SORT, MERGE_SORT, HEAP_SORT, BFS, DFS, UNKNOWN.
            lineMappings maps teaching step keys to source line numbers from the submitted code. Use these keys when relevant: COMPARE, SWAP, PICK, SHIFT, INSERT, SCAN, UPDATE_MIN, HEAPIFY, PIVOT, MOVE, PLACE, SPLIT, MERGE, APPEND_LEFT, APPEND_RIGHT, CHECK_MIDDLE, VISIT, ENQUEUE, RECURSE.
            Return only line numbers that directly correspond to the submitted source code. If unsure about a key, omit it or use an empty array.
            Confidence must be a number between 0 and 1.
            Classify the intended algorithm even if the snippet is partial or not fully runnable, as long as the structure, naming, and key operations strongly indicate one supported algorithm.
            Use UNKNOWN only when the intended supported algorithm is not reasonably inferable.
            """;

    private final AiChatClient aiChatClient;

    public OpenAiCompatibleCodeAnalysisClient(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    @Override
    public RecognitionResult analyze(String sourceCode, String language) {
        JsonNode response = aiChatClient.createStructuredResponse(SYSTEM_PROMPT,
                "Language: " + language + "\nSource code:\n" + sourceCode);
        String algorithmName = response.path("algorithm").asText("UNKNOWN");
        double confidence = response.path("confidence").asDouble(0.0);
        String rationale = response.path("rationale").asText("No rationale provided");
        Map<String, List<Integer>> lineMappings = parseLineMappings(response.path("lineMappings"), sourceLineCount(sourceCode));

        return new RecognitionResult(toDetectedAlgorithm(algorithmName), confidence, rationale, lineMappings);
    }

    private DetectedAlgorithm toDetectedAlgorithm(String algorithmName) {
        try {
            return DetectedAlgorithm.valueOf(algorithmName.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return DetectedAlgorithm.UNKNOWN;
        }
    }

    private Map<String, List<Integer>> parseLineMappings(JsonNode mappingsNode, int sourceLineCount) {
        if (!mappingsNode.isObject() || sourceLineCount <= 0) {
            return Map.of();
        }

        Map<String, List<Integer>> mappings = new LinkedHashMap<>();
        mappingsNode.properties().forEach(entry -> {
            if (!entry.getValue().isArray()) {
                return;
            }

            String key = entry.getKey().trim().toUpperCase();
            List<Integer> lines = new ArrayList<>();
            entry.getValue().forEach(lineNode -> {
                if (!lineNode.isInt()) {
                    return;
                }
                int line = lineNode.asInt();
                if (line >= 1 && line <= sourceLineCount && !lines.contains(line)) {
                    lines.add(line);
                }
            });
            if (!lines.isEmpty()) {
                lines.sort(Integer::compareTo);
                mappings.put(key, List.copyOf(lines));
            }
        });
        return Map.copyOf(mappings);
    }

    private int sourceLineCount(String sourceCode) {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return 0;
        }
        return sourceCode.split("\\R", -1).length;
    }
}
