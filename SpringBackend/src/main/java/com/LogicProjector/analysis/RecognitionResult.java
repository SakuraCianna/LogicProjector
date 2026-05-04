package com.LogicProjector.analysis;

import java.util.List;
import java.util.Map;

public record RecognitionResult(
        DetectedAlgorithm algorithm,
        double confidence,
        String rationale,
        Map<String, List<Integer>> lineMappings) {

    public RecognitionResult(DetectedAlgorithm algorithm, double confidence, String rationale) {
        this(algorithm, confidence, rationale, Map.of());
    }

    public RecognitionResult {
        lineMappings = lineMappings == null ? Map.of() : Map.copyOf(lineMappings);
    }
}
