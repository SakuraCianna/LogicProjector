package com.LogicProjector.visualization;

import java.util.List;

public record VisualizationStep(
        String title,
        String narration,
        List<Integer> arrayState,
        List<Integer> activeIndices,
        List<Integer> highlightedLines
) {
}
