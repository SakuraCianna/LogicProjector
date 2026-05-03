package com.LogicProjector.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class SearchAndDivideExtractors {

    private SearchAndDivideExtractors() {
    }

    public static VisualizationStateExtractor binarySearch() {
        return (algorithm, input, sourceCode) -> {
            int middle = input.size() / 2;
            List<VisualizationStep> steps = List.of(
                    new VisualizationStep("Check middle", "Focus the middle candidate", List.copyOf(input), List.of(middle), List.of(4, 5))
            );
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor quickSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            QuickSortLines lines = quickSortLines(sourceCode);
            quickSort(values, 0, values.size() - 1, steps, lines);
            steps.add(new VisualizationStep("Quick sort complete", "All partitions are sorted", List.copyOf(values), List.of(), List.of(8)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor mergeSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            mergeSort(values, 0, values.size() - 1, steps);
            steps.add(new VisualizationStep("Merge sort complete", "All ranges have been merged back in order", List.copyOf(values), List.of(), List.of(9)));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    private static void quickSort(List<Integer> values, int low, int high, List<VisualizationStep> steps, QuickSortLines lines) {
        if (low >= high) {
            return;
        }

        int pivotIndex = partition(values, low, high, steps, lines);
        quickSort(values, low, pivotIndex - 1, steps, lines);
        quickSort(values, pivotIndex + 1, high, steps, lines);
    }

    private static int partition(List<Integer> values, int low, int high, List<VisualizationStep> steps, QuickSortLines lines) {
        int pivot = values.get(high);
        int i = low - 1;
        steps.add(new VisualizationStep("Choose pivot", "Use the last value as pivot", List.copyOf(values), List.of(high), lines.pivot()));
        for (int j = low; j < high; j++) {
            steps.add(new VisualizationStep("Compare to pivot", "Check whether the value belongs on the left side", List.copyOf(values), List.of(j, high), lines.compare()));
            if (values.get(j) <= pivot) {
                i++;
                Collections.swap(values, i, j);
                steps.add(new VisualizationStep("Move left of pivot", "Keep smaller values before the pivot", List.copyOf(values), List.of(i, j), lines.move()));
            }
        }
        Collections.swap(values, i + 1, high);
        steps.add(new VisualizationStep("Place pivot", "The pivot lands in its final position", List.copyOf(values), List.of(i + 1), lines.place()));
        return i + 1;
    }

    private static QuickSortLines quickSortLines(String sourceCode) {
        return new QuickSortLines(
                lineNumbers(sourceCode, List.of(3), line -> compact(line).contains("pivot=") && compact(line).contains("[high]")),
                lineNumbers(sourceCode, List.of(4, 5), line -> compact(line).contains("for(") && compact(line).contains("j<high")
                        || compact(line).contains("[j]") && compact(line).contains("pivot") && compact(line).contains("<=")),
                lineNumbers(sourceCode, List.of(6), SearchAndDivideExtractors::isQuickSortMoveLine),
                lineNumbers(sourceCode, List.of(7), SearchAndDivideExtractors::isQuickSortPlaceLine));
    }

    private static boolean isQuickSortMoveLine(String line) {
        String compact = compact(line);
        if (compact.contains("i++")) {
            return true;
        }
        return compact.contains("array[i]") && compact.contains("array[j]")
                || compact.contains("arr[i]") && compact.contains("arr[j]")
                || compact.contains("array[j]=temp")
                || compact.contains("arr[j]=temp")
                || compact.contains("temp=array[i]")
                || compact.contains("temp=arr[i]");
    }

    private static boolean isQuickSortPlaceLine(String line) {
        String compact = compact(line);
        return compact.contains("i+1") || compact.contains("[high]") && compact.contains("temp") || compact.contains("returni+1");
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

    private static String compact(String line) {
        return line.replaceAll("\\s+", "");
    }

    private record QuickSortLines(
            List<Integer> pivot,
            List<Integer> compare,
            List<Integer> move,
            List<Integer> place
    ) {
    }

    private static void mergeSort(List<Integer> values, int left, int right, List<VisualizationStep> steps) {
        if (left >= right) {
            return;
        }

        int mid = (left + right) / 2;
        mergeSort(values, left, mid, steps);
        mergeSort(values, mid + 1, right, steps);
        merge(values, left, mid, right, steps);
    }

    private static void merge(List<Integer> values, int left, int mid, int right, List<VisualizationStep> steps) {
        List<Integer> leftPart = new ArrayList<>(values.subList(left, mid + 1));
        List<Integer> rightPart = new ArrayList<>(values.subList(mid + 1, right + 1));

        int i = 0;
        int j = 0;
        int k = left;

        steps.add(new VisualizationStep("Split range", "Prepare two sorted halves for merge", List.copyOf(values), List.of(left, right), List.of(3, 4)));

        while (i < leftPart.size() && j < rightPart.size()) {
            if (leftPart.get(i) <= rightPart.get(j)) {
                values.set(k++, leftPart.get(i++));
            } else {
                values.set(k++, rightPart.get(j++));
            }
            steps.add(new VisualizationStep("Merge next value", "Write the smaller front value back into the array", List.copyOf(values), List.of(k - 1), List.of(5, 6)));
        }

        while (i < leftPart.size()) {
            values.set(k++, leftPart.get(i++));
            steps.add(new VisualizationStep("Append remaining left", "Copy leftover values from the left half", List.copyOf(values), List.of(k - 1), List.of(7)));
        }

        while (j < rightPart.size()) {
            values.set(k++, rightPart.get(j++));
            steps.add(new VisualizationStep("Append remaining right", "Copy leftover values from the right half", List.copyOf(values), List.of(k - 1), List.of(8)));
        }
    }
}
