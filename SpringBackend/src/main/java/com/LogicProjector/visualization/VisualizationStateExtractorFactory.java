package com.LogicProjector.visualization;

import org.springframework.stereotype.Component;

import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.UnsupportedAlgorithmException;

@Component
public class VisualizationStateExtractorFactory {

    public VisualizationStateExtractor forAlgorithm(DetectedAlgorithm algorithm) {
        return switch (algorithm) {
            case BUBBLE_SORT -> SortingExtractors.bubbleSort();
            case SELECTION_SORT -> SortingExtractors.selectionSort();
            case INSERTION_SORT -> SortingExtractors.insertionSort();
            case BINARY_SEARCH -> SearchAndDivideExtractors.binarySearch();
            case QUICK_SORT -> SearchAndDivideExtractors.quickSort();
            case MERGE_SORT -> SearchAndDivideExtractors.mergeSort();
            case HEAP_SORT -> SortingExtractors.heapSort();
            case BFS -> GraphTraversalExtractors.bfs();
            case DFS -> GraphTraversalExtractors.dfs();
            default -> throw new UnsupportedAlgorithmException("No extractor for algorithm " + algorithm);
        };
    }
}
