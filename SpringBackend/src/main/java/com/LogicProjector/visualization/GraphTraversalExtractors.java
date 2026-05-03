package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;

public final class GraphTraversalExtractors {

    private GraphTraversalExtractors() {
    }

    public static VisualizationStateExtractor bfs() {
        return (algorithm, input, sourceCode) -> {
            List<VisualizationStep> steps = new ArrayList<>();
            for (int index = 0; index < input.size(); index++) {
                steps.add(new VisualizationStep(
                        "Visit node " + input.get(index),
                        "Breadth-first search visits nodes level by level using a queue",
                        List.copyOf(input),
                        List.of(index),
                        List.of(3, 4)));
            }
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor dfs() {
        return (algorithm, input, sourceCode) -> {
            List<VisualizationStep> steps = new ArrayList<>();
            for (int index = 0; index < input.size(); index++) {
                steps.add(new VisualizationStep(
                        "Explore node " + input.get(index),
                        "Depth-first search follows one branch before backtracking",
                        List.copyOf(input),
                        List.of(index),
                        List.of(3, 4)));
            }
            return new VisualizationPayload(algorithm, steps);
        };
    }
}
