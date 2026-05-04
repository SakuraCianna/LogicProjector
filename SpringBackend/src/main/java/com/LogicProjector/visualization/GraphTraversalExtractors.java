package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class GraphTraversalExtractors {

    private GraphTraversalExtractors() {
    }

    public static VisualizationStateExtractor bfs() {
        return (algorithm, input, sourceCode) -> {
            List<VisualizationStep> steps = new ArrayList<>();
            List<Integer> highlightedLines = lineNumbers(sourceCode, GraphTraversalExtractors::isBfsLine);
            for (int index = 0; index < input.size(); index++) {
                steps.add(new VisualizationStep(
                        "Visit node " + input.get(index),
                        "Breadth-first search visits nodes level by level using a queue",
                        List.copyOf(input),
                        List.of(index),
                        highlightedLines));
            }
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor dfs() {
        return (algorithm, input, sourceCode) -> {
            List<VisualizationStep> steps = new ArrayList<>();
            List<Integer> highlightedLines = lineNumbers(sourceCode, GraphTraversalExtractors::isDfsLine);
            for (int index = 0; index < input.size(); index++) {
                steps.add(new VisualizationStep(
                        "Explore node " + input.get(index),
                        "Depth-first search follows one branch before backtracking",
                        List.copyOf(input),
                        List.of(index),
                        highlightedLines));
            }
            return new VisualizationPayload(algorithm, steps);
        };
    }

    private static List<Integer> lineNumbers(String sourceCode, Predicate<String> matcher) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return List.of();
        }

        List<Integer> matches = new ArrayList<>();
        String[] lines = sourceCode.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (matcher.test(lines[index])) {
                matches.add(index + 1);
            }
        }
        return matches;
    }

    private static boolean isBfsLine(String line) {
        String compact = line.replaceAll("\\s+", "").toLowerCase();
        return compact.contains("queue")
                || compact.contains("while(")
                || compact.contains("poll(")
                || compact.contains("remove(")
                || compact.contains("visit(")
                || compact.contains("for(");
    }

    private static boolean isDfsLine(String line) {
        String compact = line.replaceAll("\\s+", "").toLowerCase();
        return compact.contains("dfs(") || compact.contains("visit(") || compact.contains("for(");
    }
}
