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
            List<Integer> compareLines = lineNumbers(sourceCode, SortingExtractors::isBubbleCompareLine);
            List<Integer> swapLines = lineNumbers(sourceCode, SortingExtractors::isBubbleSwapLine);

            for (int i = 0; i < values.size() - 1; i++) {
                for (int j = 0; j < values.size() - i - 1; j++) {
                    steps.add(step("Compare " + j + " and " + (j + 1), "Comparing adjacent values", values, List.of(j, j + 1), compareLines));
                    if (values.get(j) > values.get(j + 1)) {
                        Collections.swap(values, j, j + 1);
                        steps.add(step("Swap " + j + " and " + (j + 1), "Swap moves the larger value right", values, List.of(j, j + 1), swapLines));
                    }
                }
            }

            steps.add(step("Bubble sort complete", "The array is now sorted", values, List.of(), List.of()));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor selectionSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            SelectionSortLines lines = selectionSortLines(sourceCode);

            for (int i = 0; i < values.size() - 1; i++) {
                int minIndex = i;
                for (int j = i + 1; j < values.size(); j++) {
                    steps.add(step("Scan for minimum", "Track the smallest value in the unsorted region", values, List.of(minIndex, j), lines.scan()));
                    if (values.get(j) < values.get(minIndex)) {
                        minIndex = j;
                        steps.add(step("Update minimum", "A new minimum has been found", values, List.of(i, minIndex), lines.update()));
                    }
                }
                if (minIndex != i) {
                    Collections.swap(values, i, minIndex);
                    steps.add(step("Swap into place", "Move the smallest value into the sorted prefix", values, List.of(i, minIndex), lines.swap()));
                }
            }

            steps.add(step("Selection sort complete", "The array is now sorted", values, List.of(), List.of()));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor insertionSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            InsertionSortLines lines = insertionSortLines(sourceCode);

            for (int i = 1; i < values.size(); i++) {
                int key = values.get(i);
                int j = i - 1;
                steps.add(step("Pick key", "Take the next value from the unsorted region", values, List.of(i), lines.pick()));
                while (j >= 0 && values.get(j) > key) {
                    values.set(j + 1, values.get(j));
                    steps.add(step("Shift right", "Shift larger values one slot to the right", values, List.of(j, j + 1), lines.shift()));
                    j--;
                }
                values.set(j + 1, key);
                steps.add(step("Insert key", "Place the key into its sorted position", values, List.of(j + 1), lines.insert()));
            }

            steps.add(step("Insertion sort complete", "The array is now sorted", values, List.of(), List.of()));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor heapSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            List<Integer> heapifyLines = lineNumbers(sourceCode, line -> compact(line).contains("heapify("));
            List<Integer> swapLines = lineNumbers(sourceCode, SortingExtractors::isSwapLine);

            int size = values.size();
            for (int index = size / 2 - 1; index >= 0; index--) {
                heapify(values, size, index, steps, heapifyLines, swapLines);
            }
            for (int end = size - 1; end > 0; end--) {
                Collections.swap(values, 0, end);
                steps.add(step("Move max to sorted suffix", "Swap the heap root into its final sorted position", values, List.of(0, end), swapLines));
                heapify(values, end, 0, steps, heapifyLines, swapLines);
            }

            steps.add(step("Heap sort complete", "The array is now sorted", values, List.of(), List.of()));
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

    private static SelectionSortLines selectionSortLines(String sourceCode) {
        return new SelectionSortLines(
                lineNumbers(sourceCode, SortingExtractors::isSelectionScanLine),
                lineNumbers(sourceCode, line -> compact(line).toLowerCase().contains("minindex=j")
                        || compact(line).toLowerCase().contains("min=j")),
                lineNumbers(sourceCode, SortingExtractors::isSelectionSwapLine));
    }

    private static InsertionSortLines insertionSortLines(String sourceCode) {
        return new InsertionSortLines(
                lineNumbers(sourceCode, line -> compact(line).contains("key=") && compact(line).contains("[i]")),
                lineNumbers(sourceCode, SortingExtractors::isInsertionShiftLine),
                lineNumbers(sourceCode, line -> compact(line).contains("[j+1]=key")));
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

    private static boolean isSelectionScanLine(String line) {
        String compact = compact(line);
        return compact.contains("for(") && compact.contains("j=")
                || compact.contains("[j]") && compact.contains("[min") && compact.contains("<");
    }

    private static boolean isSelectionSwapLine(String line) {
        String compact = compact(line);
        return compact.contains("temp=") && compact.contains("[i]")
                || compact.contains("[i]=") && compact.contains("[min")
                || compact.contains("[min") && compact.contains("=temp");
    }

    private static boolean isInsertionShiftLine(String line) {
        String compact = compact(line);
        return compact.contains("[j]>key")
                || compact.contains("[j+1]=") && compact.contains("[j]")
                || compact.contains("j--");
    }

    private static boolean isSwapLine(String line) {
        String compact = compact(line);
        return compact.contains("swap(") || compact.contains("temp") || compact.contains("=temp");
    }

    private static String compact(String line) {
        return line.replaceAll("\\s+", "");
    }

    private record SelectionSortLines(
            List<Integer> scan,
            List<Integer> update,
            List<Integer> swap
    ) {
    }

    private record InsertionSortLines(
            List<Integer> pick,
            List<Integer> shift,
            List<Integer> insert
    ) {
    }
}
