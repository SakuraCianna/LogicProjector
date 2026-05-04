package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StepLineMappingApplier {

    private StepLineMappingApplier() {
    }

    public static VisualizationPayload apply(VisualizationPayload payload, Map<String, List<Integer>> lineMappings) {
        if (lineMappings == null || lineMappings.isEmpty()) {
            return payload;
        }

        List<VisualizationStep> mappedSteps = new ArrayList<>();
        for (VisualizationStep step : payload.steps()) {
            List<Integer> mappedLines = lineMappings.get(stepKey(step.title()));
            mappedSteps.add(new VisualizationStep(
                    step.title(),
                    step.narration(),
                    step.arrayState(),
                    step.activeIndices(),
                    mappedLines == null || mappedLines.isEmpty() ? step.highlightedLines() : mappedLines,
                    step.displayTitle()));
        }
        return new VisualizationPayload(payload.algorithm(), List.copyOf(mappedSteps));
    }

    private static String stepKey(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        if (normalized.contains("choose pivot")) {
            return "PIVOT";
        }
        if (normalized.contains("compare")) {
            return "COMPARE";
        }
        if (normalized.contains("move left")) {
            return "MOVE";
        }
        if (normalized.contains("place pivot")) {
            return "PLACE";
        }
        if (normalized.contains("scan for minimum")) {
            return "SCAN";
        }
        if (normalized.contains("update minimum")) {
            return "UPDATE_MIN";
        }
        if (normalized.contains("swap") || normalized.contains("restore heap") || normalized.contains("move max")) {
            return "SWAP";
        }
        if (normalized.contains("pick key")) {
            return "PICK";
        }
        if (normalized.contains("shift right")) {
            return "SHIFT";
        }
        if (normalized.contains("insert key")) {
            return "INSERT";
        }
        if (normalized.contains("heapify")) {
            return "HEAPIFY";
        }
        if (normalized.contains("split range")) {
            return "SPLIT";
        }
        if (normalized.contains("merge next")) {
            return "MERGE";
        }
        if (normalized.contains("append remaining left")) {
            return "APPEND_LEFT";
        }
        if (normalized.contains("append remaining right")) {
            return "APPEND_RIGHT";
        }
        if (normalized.contains("check middle")) {
            return "CHECK_MIDDLE";
        }
        if (normalized.contains("visit node") || normalized.contains("explore node")) {
            return "VISIT";
        }
        return "";
    }
}
