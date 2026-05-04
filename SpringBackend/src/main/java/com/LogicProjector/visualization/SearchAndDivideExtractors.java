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
            List<Integer> checkLines = lineNumbers(sourceCode, SearchAndDivideExtractors::isBinarySearchCheckLine);
            List<VisualizationStep> steps = List.of(
                    new VisualizationStep("Check middle", "Focus the middle candidate", List.copyOf(input), List.of(middle), checkLines)
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
            steps.add(new VisualizationStep("Quick sort complete", "All partitions are sorted", List.copyOf(values), List.of(), List.of()));
            return new VisualizationPayload(algorithm, steps);
        };
    }

    public static VisualizationStateExtractor mergeSort() {
        return (algorithm, input, sourceCode) -> {
            List<Integer> values = new ArrayList<>(input);
            List<VisualizationStep> steps = new ArrayList<>();
            MergeSortLines lines = mergeSortLines(sourceCode);
            mergeSort(values, 0, values.size() - 1, steps, lines);
            steps.add(new VisualizationStep("Merge sort complete", "All ranges have been merged back in order", List.copyOf(values), List.of(), List.of()));
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
                lineNumbers(sourceCode, line -> compact(line).contains("pivot=") && compact(line).contains("[high]")),
                lineNumbers(sourceCode, line -> compact(line).contains("for(") && compact(line).contains("j<high")
                        || compact(line).contains("[j]") && compact(line).contains("pivot") && compact(line).contains("<=")),
                lineNumbers(sourceCode, SearchAndDivideExtractors::isQuickSortMoveLine),
                lineNumbers(sourceCode, SearchAndDivideExtractors::isQuickSortPlaceLine));
    }

    private static MergeSortLines mergeSortLines(String sourceCode) {
        return new MergeSortLines(
                lineNumbers(sourceCode, SearchAndDivideExtractors::isMergeSortSplitLine),
                lineNumbers(sourceCode, SearchAndDivideExtractors::isMergeNextLine),
                lineNumbers(sourceCode, SearchAndDivideExtractors::isAppendRemainingLeftLine),
                lineNumbers(sourceCode, SearchAndDivideExtractors::isAppendRemainingRightLine));
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

    private static boolean isBinarySearchCheckLine(String line) {
        String compact = compact(line);
        return compact.contains("while(") && compact.contains("left<=right")
                || compact.contains("mid=")
                || compact.contains("[mid]") && (compact.contains("==") || compact.contains("<") || compact.contains(">"));
    }

    private static boolean isMergeSortSplitLine(String line) {
        String compact = compact(line).toLowerCase();
        return compact.contains("mid=")
                || compact.contains("mergesort(")
                || compact.contains("merge(") && compact.contains("mid");
    }

    private static boolean isMergeNextLine(String line) {
        String compact = compact(line).toLowerCase();
        return compact.contains("while(") && compact.contains("&&")
                || compact.contains("[i]") && compact.contains("[j]")
                || compact.contains("set(")
                || compact.contains("[left+k]");
    }

    private static boolean isAppendRemainingLeftLine(String line) {
        String compact = compact(line).toLowerCase();
        return compact.contains("while(") && (compact.contains("i<") || compact.contains("i<="))
                || compact.contains("leftpart")
                || compact.contains("array[i++]")
                || compact.contains("arr[i++]");
    }

    private static boolean isAppendRemainingRightLine(String line) {
        String compact = compact(line).toLowerCase();
        return compact.contains("while(") && (compact.contains("j<") || compact.contains("j<="))
                || compact.contains("rightpart")
                || compact.contains("array[j++]")
                || compact.contains("arr[j++]");
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

    private static void mergeSort(List<Integer> values, int left, int right, List<VisualizationStep> steps, MergeSortLines lines) {
        if (left >= right) {
            return;
        }

        int mid = (left + right) / 2;
        mergeSort(values, left, mid, steps, lines);
        mergeSort(values, mid + 1, right, steps, lines);
        merge(values, left, mid, right, steps, lines);
    }

    private static void merge(List<Integer> values, int left, int mid, int right, List<VisualizationStep> steps, MergeSortLines lines) {
        List<Integer> leftPart = new ArrayList<>(values.subList(left, mid + 1));
        List<Integer> rightPart = new ArrayList<>(values.subList(mid + 1, right + 1));

        int i = 0;
        int j = 0;
        int k = left;

        steps.add(new VisualizationStep("Split range", "Prepare two sorted halves for merge", List.copyOf(values), List.of(left, right), lines.split()));

        while (i < leftPart.size() && j < rightPart.size()) {
            if (leftPart.get(i) <= rightPart.get(j)) {
                values.set(k++, leftPart.get(i++));
            } else {
                values.set(k++, rightPart.get(j++));
            }
            steps.add(new VisualizationStep("Merge next value", "Write the smaller front value back into the array", List.copyOf(values), List.of(k - 1), lines.mergeNext()));
        }

        while (i < leftPart.size()) {
            values.set(k++, leftPart.get(i++));
            steps.add(new VisualizationStep("Append remaining left", "Copy leftover values from the left half", List.copyOf(values), List.of(k - 1), lines.appendLeft()));
        }

        while (j < rightPart.size()) {
            values.set(k++, rightPart.get(j++));
            steps.add(new VisualizationStep("Append remaining right", "Copy leftover values from the right half", List.copyOf(values), List.of(k - 1), lines.appendRight()));
        }
    }

    private record MergeSortLines(
            List<Integer> split,
            List<Integer> mergeNext,
            List<Integer> appendLeft,
            List<Integer> appendRight
    ) {
    }
}
