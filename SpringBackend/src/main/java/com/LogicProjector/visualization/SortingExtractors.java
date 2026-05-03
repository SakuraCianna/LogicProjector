package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class SortingExtractors {

    private SortingExtractors() {
    }

    public static VisualizationStateExtractor bubbleSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            List<Integer> compareLines = lineNumbers(sourceCode, List.of(3, 4), SortingExtractors::isBubbleCompareLine);
            List<Integer> swapLines = lineNumbers(sourceCode, List.of(5), SortingExtractors::isBubbleSwapLine);

            for (int i = 0; i < values.size() - 1; i++) {
                for (int j = 0; j < values.size() - i - 1; j++) {
                    steps.add(step("Compare " + j + " and " + (j + 1), "Comparing adjacent values", values, List.of(j, j + 1), compareLines));
                    if (values.get(j) > values.get(j + 1)) {
                        Collections.swap(values, j, j + 1);
                        steps.add(step("Swap " + j + " and " + (j + 1), "Swap moves the larger value right", values, List.of(j, j + 1), swapLines));
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

    public static VisualizationStateExtractor heapSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            List<Integer> heapifyLines = lineNumbers(sourceCode, List.of(3, 4), line -> compact(line).contains("heapify("));
            List<Integer> swapLines = lineNumbers(sourceCode, List.of(5), SortingExtractors::isBubbleSwapLine);

            int size = values.size();
            for (int index = size / 2 - 1; index >= 0; index--) {
                heapify(values, size, index, steps, heapifyLines, swapLines);
            }
            for (int end = size - 1; end > 0; end--) {
                Collections.swap(values, 0, end);
                steps.add(step("Move max to sorted suffix", "Swap the heap root into its final sorted position", values, List.of(0, end), swapLines));
                heapify(values, end, 0, steps, heapifyLines, swapLines);
            }

            steps.add(step("Heap sort complete", "The array is now sorted", values, List.of(), List.of(6)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    private static void heapify(List<Integer> values, int size, int root, List<VisualizationStep> steps,
            List<Integer> heapifyLines, List<Integer> swapLines) {
        int largest = root;
        int left = root * 2 + 1;
        int right = root * 2 + 2;

        if (left < size && values.get(left) > values.get(largest)) {
            largest = left;
        }
        if (right < size && values.get(right) > values.get(largest)) {
            largest = right;
        }

        steps.add(step("Heapify node " + root, "Compare parent with children to maintain the max heap", values, List.of(root, largest), heapifyLines));
        if (largest != root) {
            Collections.swap(values, root, largest);
            steps.add(step("Restore heap order", "Move the larger child above the parent", values, List.of(root, largest), swapLines));
            heapify(values, size, largest, steps, heapifyLines, swapLines);
        }
    }

    private static VisualizationStep step(String title, String narration, List<Integer> values, List<Integer> active,
            List<Integer> highlightedLines) {
        return new VisualizationStep(title, narration, List.copyOf(values), List.copyOf(active), List.copyOf(highlightedLines));
    }

    private static List<Integer> lineNumbers(String sourceCode, List<Integer> fallback, Predicate<String> matcher) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return fallback;
        }

        List<Integer> matches = new ArrayList<>();
        String[] lines = sourceCode.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (matcher.test(lines[index])) {
                matches.add(index + 1);
            }
        }
        return matches.isEmpty() ? fallback : matches;
    }

    private static boolean isBubbleCompareLine(String line) {
        String compact = compact(line);
        return compact.contains(">") && compact.contains("[j]") && compact.contains("[j+1]");
    }

    private static boolean isBubbleSwapLine(String line) {
        String compact = compact(line);
        if (compact.contains("swap(") || compact.contains("temp")) {
            return true;
        }
        return compact.contains("=") && compact.contains("[j]") && compact.contains("[j+1]") && !compact.contains(">");
    }

    private static String compact(String line) {
        return line.replaceAll("\\s+", "");
    }
}
