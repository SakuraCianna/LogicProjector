package com.LogicProjector.visualization;

import java.util.List;

public record VisualizationStep(
        String title,
        String narration,
        List<Integer> arrayState,
        List<Integer> activeIndices,
        List<Integer> highlightedLines,
        String displayTitle
) {
    public VisualizationStep(
            String title,
            String narration,
            List<Integer> arrayState,
            List<Integer> activeIndices,
            List<Integer> highlightedLines) {
        this(title, narration, arrayState, activeIndices, highlightedLines, null);
    }
}
