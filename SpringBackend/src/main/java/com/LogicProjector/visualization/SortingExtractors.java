package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SortingExtractors {

    private SortingExtractors() {
    }

    public static VisualizationStateExtractor bubbleSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();

            for (int i = 0; i < values.size() - 1; i++) {
                for (int j = 0; j < values.size() - i - 1; j++) {
                    steps.add(step("Compare " + j + " and " + (j + 1), "Comparing adjacent values", values, List.of(j, j + 1), List.of(3, 4)));
                    if (values.get(j) > values.get(j + 1)) {
                        Collections.swap(values, j, j + 1);
                        steps.add(step("Swap " + j + " and " + (j + 1), "Swap moves the larger value right", values, List.of(j, j + 1), List.of(5)));
                    }
                }
            }

            steps.add(step("Bubble sort complete", "The array is now sorted", values, List.of(), List.of(6)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor selectionSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();

            for (int i = 0; i < values.size() - 1; i++) {
                int minIndex = i;
                for (int j = i + 1; j < values.size(); j++) {
                    steps.add(step("Scan for minimum", "Track the smallest value in the unsorted region", values, List.of(minIndex, j), List.of(3, 4)));
                    if (values.get(j) < values.get(minIndex)) {
                        minIndex = j;
                        steps.add(step("Update minimum", "A new minimum has been found", values, List.of(i, minIndex), List.of(4)));
                    }
                }
                if (minIndex != i) {
                    Collections.swap(values, i, minIndex);
                    steps.add(step("Swap into place", "Move the smallest value into the sorted prefix", values, List.of(i, minIndex), List.of(5)));
                }
            }

            steps.add(step("Selection sort complete", "The array is now sorted", values, List.of(), List.of(6)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor insertionSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();

            for (int i = 1; i < values.size(); i++) {
                int key = values.get(i);
                int j = i - 1;
                steps.add(step("Pick key", "Take the next value from the unsorted region", values, List.of(i), List.of(3)));
                while (j >= 0 && values.get(j) > key) {
                    values.set(j + 1, values.get(j));
                    steps.add(step("Shift right", "Shift larger values one slot to the right", values, List.of(j, j + 1), List.of(4, 5)));
                    j--;
                }
                values.set(j + 1, key);
                steps.add(step("Insert key", "Place the key into its sorted position", values, List.of(j + 1), List.of(6)));
            }

            steps.add(step("Insertion sort complete", "The array is now sorted", values, List.of(), List.of(7)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    private static VisualizationStep step(String title, String narration, List<Integer> values, List<Integer> active,
            List<Integer> highlightedLines) {
        return new VisualizationStep(title, narration, List.copyOf(values), List.copyOf(active), List.copyOf(highlightedLines));
    }
}
