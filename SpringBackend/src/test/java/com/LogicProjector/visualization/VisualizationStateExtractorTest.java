package com.LogicProjector.visualization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
    void shouldBuildMergeSortTimeline() {
        VisualizationPayload payload = SearchAndDivideExtractors.mergeSort().extract(
                "mergeSort",
                List.of(5, 2, 4, 1),
                "merge(arr, left, mid, right);"
        );

        assertThat(payload.steps()).isNotEmpty();
        assertThat(payload.steps().get(payload.steps().size() - 1).arrayState()).containsExactly(1, 2, 4, 5);
    }
}
