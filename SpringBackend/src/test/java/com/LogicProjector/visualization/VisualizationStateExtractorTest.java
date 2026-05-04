package com.LogicProjector.visualization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VisualizationStateExtractorTest {

    @Test
    void shouldBuildBubbleSortTimeline() {
        VisualizationStateExtractor extractor = SortingExtractors.bubbleSort();

        VisualizationPayload payload = extractor.extract(
                "bubbleSort",
                List.of(5, 1, 4),
                "for (int j = 0; j < n - i - 1; j++) { if (arr[j] > arr[j + 1]) { swap(...); } }"
        );

        assertThat(payload.steps()).hasSizeGreaterThan(2);
        assertThat(payload.steps().get(0).title()).contains("Compare");
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 4, 5);
    }

    @Test
    void shouldHighlightBubbleSortLinesFromSubmittedSource() {
        VisualizationPayload payload = SortingExtractors.bubbleSort().extract(
                "bubbleSort",
                List.of(4, 1),
                """
                public class BubbleSort {
                    public static void sort(int[] array) {
                        if (array == null || array.length < 2) {
                            return;
                        }
                        int n = array.length;
                        for (int i = 0; i < n - 1; i++) {
                            for (int j = 0; j < n - i - 1; j++) {
                                if (array[j] > array[j + 1]) {
                                    int temp = array[j];
                                    array[j] = array[j + 1];
                                    array[j + 1] = temp;
                                }
                            }
                        }
                    }
                }
                """
        );

        VisualizationStep compareStep = payload.steps().stream()
                .filter(step -> step.title().startsWith("Compare"))
                .findFirst()
                .orElseThrow();
        VisualizationStep swapStep = payload.steps().stream()
                .filter(step -> step.title().startsWith("Swap"))
                .findFirst()
                .orElseThrow();

        assertThat(compareStep.highlightedLines()).containsExactly(9);
        assertThat(swapStep.highlightedLines()).containsExactly(10, 11, 12);
        assertThat(swapStep.highlightedLines()).doesNotContain(4);
    }

    @Test
    void shouldBuildSelectionSortTimeline() {
        VisualizationPayload payload = SortingExtractors.selectionSort().extract(
                "selectionSort",
                List.of(3, 1, 2),
                "for (int j = i + 1; j < n; j++) { if (arr[j] < arr[minIndex]) { minIndex = j; } }"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 2, 3);
    }

    @Test
    void shouldHighlightSelectionSortLinesFromSubmittedSource() {
        VisualizationPayload payload = SortingExtractors.selectionSort().extract(
                "selectionSort",
                List.of(3, 1, 2),
                """
                public class SelectionSort {
                    public static void sort(int[] array) {
                        for (int i = 0; i < array.length - 1; i++) {
                            int minIndex = i;
                            for (int j = i + 1; j < array.length; j++) {
                                if (array[j] < array[minIndex]) {
                                    minIndex = j;
                                }
                            }
                            if (minIndex != i) {
                                int temp = array[i];
                                array[i] = array[minIndex];
                                array[minIndex] = temp;
                            }
                        }
                    }
                }
                """
        );

        assertThat(stepNamed(payload, "Scan for minimum").highlightedLines()).containsExactly(5, 6);
        assertThat(stepNamed(payload, "Update minimum").highlightedLines()).containsExactly(7);
        assertThat(stepNamed(payload, "Swap into place").highlightedLines()).containsExactly(11, 12, 13);
    }

    @Test
    void shouldBuildInsertionSortTimeline() {
        VisualizationPayload payload = SortingExtractors.insertionSort().extract(
                "insertionSort",
                List.of(4, 3, 1),
                "while (j >= 0 && arr[j] > key) { arr[j + 1] = arr[j]; j--; }"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 3, 4);
    }

    @Test
    void shouldHighlightInsertionSortLinesFromSubmittedSource() {
        VisualizationPayload payload = SortingExtractors.insertionSort().extract(
                "insertionSort",
                List.of(4, 3, 1),
                """
                public class InsertionSort {
                    public static void sort(int[] array) {
                        for (int i = 1; i < array.length; i++) {
                            int key = array[i];
                            int j = i - 1;
                            while (j >= 0 && array[j] > key) {
                                array[j + 1] = array[j];
                                j--;
                            }
                            array[j + 1] = key;
                        }
                    }
                }
                """
        );

        assertThat(stepNamed(payload, "Pick key").highlightedLines()).containsExactly(4);
        assertThat(stepNamed(payload, "Shift right").highlightedLines()).containsExactly(6, 7, 8);
        assertThat(stepNamed(payload, "Insert key").highlightedLines()).containsExactly(10);
    }

    @Test
    void shouldBuildBinarySearchTimeline() {
        VisualizationPayload payload = SearchAndDivideExtractors.binarySearch().extract(
                "binarySearch",
                List.of(1, 3, 5, 7, 9),
                "while (left <= right) { int mid = (left + right) / 2; }"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(0).activeIndices()).contains(2);
    }

    @Test
    void shouldHighlightBinarySearchLinesFromSubmittedSource() {
        VisualizationPayload payload = SearchAndDivideExtractors.binarySearch().extract(
                "binarySearch",
                List.of(1, 3, 5, 7, 9),
                """
                public class BinarySearch {
                    public static int search(int[] array, int target) {
                        int left = 0;
                        int right = array.length - 1;
                        while (left <= right) {
                            int mid = left + (right - left) / 2;
                            if (array[mid] == target) {
                                return mid;
                            }
                            if (array[mid] < target) {
                                left = mid + 1;
                            } else {
                                right = mid - 1;
                            }
                        }
                        return -1;
                    }
                }
                """
        );

        assertThat(payload.steps().get(0).highlightedLines()).containsExactly(5, 6, 7, 10);
    }

    @Test
    void shouldBuildQuickSortTimeline() {
        VisualizationPayload payload = SearchAndDivideExtractors.quickSort().extract(
                "quickSort",
                List.of(5, 1, 4, 2),
                "int pivot = arr[high]; partition(arr, low, high);"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 2, 4, 5);
    }

    @Test
    void shouldHighlightQuickSortLinesFromSubmittedSource() {
        VisualizationPayload payload = SearchAndDivideExtractors.quickSort().extract(
                "quickSort",
                List.of(5, 1, 4, 2),
                """
                public class QuickSort {
                    public static void quickSort(int[] array, int low, int high) {
                        if (low >= high) {
                            return;
                        }

                        int pivotIndex = partition(array, low, high);
                        quickSort(array, low, pivotIndex - 1);
                        quickSort(array, pivotIndex + 1, high);
                    }

                    private static int partition(int[] array, int low, int high) {
                        int pivot = array[high];
                        int i = low - 1;

                        for (int j = low; j < high; j++) {
                            if (array[j] <= pivot) {
                                i++;
                                int temp = array[i];
                                array[i] = array[j];
                                array[j] = temp;
                            }
                        }

                        int temp = array[i + 1];
                        array[i + 1] = array[high];
                        array[high] = temp;
                        return i + 1;
                    }
                }
                """
        );

        VisualizationStep pivotStep = payload.steps().stream()
                .filter(step -> step.title().equals("Choose pivot"))
                .findFirst()
                .orElseThrow();
        VisualizationStep compareStep = payload.steps().stream()
                .filter(step -> step.title().equals("Compare to pivot"))
                .findFirst()
                .orElseThrow();
        VisualizationStep moveStep = payload.steps().stream()
                .filter(step -> step.title().equals("Move left of pivot"))
                .findFirst()
                .orElseThrow();
        VisualizationStep placeStep = payload.steps().stream()
                .filter(step -> step.title().equals("Place pivot"))
                .findFirst()
                .orElseThrow();

        assertThat(pivotStep.highlightedLines()).containsExactly(13);
        assertThat(compareStep.highlightedLines()).containsExactly(16, 17);
        assertThat(moveStep.highlightedLines()).containsExactly(18, 19, 20, 21);
        assertThat(placeStep.highlightedLines()).containsExactly(25, 26, 27, 28);
    }

    @Test
    void shouldBuildMergeSortTimeline() {
        VisualizationPayload payload = SearchAndDivideExtractors.mergeSort().extract(
                "mergeSort",
                List.of(5, 2, 4, 1),
                "merge(arr, left, mid, right);"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 2, 4, 5);
    }

    @Test
    void shouldBuildHeapSortTimeline() {
        VisualizationPayload payload = SortingExtractors.heapSort().extract(
                "heapSort",
                List.of(4, 10, 3, 5, 1),
                "void heapSort(int arr[], int n) { heapify(arr, n, i); }"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 3, 4, 5, 10);
    }

    @Test
    void shouldBuildGraphTraversalTimelines() {
        VisualizationPayload bfs = GraphTraversalExtractors.bfs().extract("BFS", List.of(0, 1, 2, 3, 4), "queue<int> q;");
        VisualizationPayload dfs = GraphTraversalExtractors.dfs().extract("DFS", List.of(0, 1, 2, 3, 4), "void dfs(int node) {}");

        assertThat(bfs.steps()).isNotEmpty();
        assertThat(dfs.steps()).isNotEmpty();
        assertThat(bfs.steps().get(0).activeIndices()).containsExactly(0);
        assertThat(dfs.steps().get(0).activeIndices()).containsExactly(0);
    }

    @Test
    void shouldHighlightGraphTraversalLinesFromSubmittedSource() {
        VisualizationPayload bfs = GraphTraversalExtractors.bfs().extract(
                "BFS",
                List.of(0, 1, 2),
                """
                void bfs(int start) {
                    Queue<Integer> queue = new ArrayDeque<>();
                    queue.add(start);
                    while (!queue.isEmpty()) {
                        int node = queue.remove();
                        visit(node);
                        for (int neighbor : graph.get(node)) {
                            queue.add(neighbor);
                        }
                    }
                }
                """
        );
        VisualizationPayload dfs = GraphTraversalExtractors.dfs().extract(
                "DFS",
                List.of(0, 1, 2),
                """
                void dfs(int node) {
                    visit(node);
                    for (int neighbor : graph.get(node)) {
                        dfs(neighbor);
                    }
                }
                """
        );

        assertThat(bfs.steps().get(0).highlightedLines()).containsExactly(2, 3, 4, 5, 6, 7, 8);
        assertThat(dfs.steps().get(0).highlightedLines()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void shouldAvoidTemplateLineHighlightsWhenSourceDoesNotMatch() {
        VisualizationPayload payload = SearchAndDivideExtractors.binarySearch().extract(
                "binarySearch",
                List.of(1, 3, 5),
                "class Unknown {}"
        );

        assertThat(payload.steps().get(0).highlightedLines()).isEmpty();
    }

    @Test
    void shouldApplyAiLineMappingsToMatchingVisualizationSteps() {
        VisualizationPayload payload = new VisualizationPayload("QUICK_SORT", List.of(
                new VisualizationStep("Choose pivot", "Use pivot", List.of(3, 1, 2), List.of(2), List.of(1)),
                new VisualizationStep("Compare to pivot", "Compare", List.of(3, 1, 2), List.of(0, 2), List.of(2)),
                new VisualizationStep("Move left of pivot", "Move", List.of(1, 3, 2), List.of(0, 1), List.of(3))
        ));

        VisualizationPayload mapped = StepLineMappingApplier.apply(payload, Map.of(
                "PIVOT", List.of(9),
                "COMPARE", List.of(10, 11)
        ));

        assertThat(mapped.steps().get(0).highlightedLines()).containsExactly(9);
        assertThat(mapped.steps().get(1).highlightedLines()).containsExactly(10, 11);
        assertThat(mapped.steps().get(2).highlightedLines()).containsExactly(3);
    }

    @Test
    void shouldAddChineseDisplayTitlesForExportRendering() {
        VisualizationPayload payload = new VisualizationPayload("QUICK_SORT", List.of(
                new VisualizationStep("Choose pivot", "Use pivot", List.of(3, 1, 2), List.of(2), List.of(1)),
                new VisualizationStep("Compare 0 and 1", "Compare", List.of(3, 1, 2), List.of(0, 1), List.of(2))
        ));

        VisualizationPayload localized = StepDisplayTitleLocalizer.apply(payload);

        assertThat(localized.steps().get(0).displayTitle()).isEqualTo("选择基准");
        assertThat(localized.steps().get(1).displayTitle()).isEqualTo("比较元素");
    }

    private static VisualizationStep stepNamed(VisualizationPayload payload, String title) {
        return payload.steps().stream()
                .filter(step -> step.title().equals(title))
                .findFirst()
                .orElseThrow();
    }
}
