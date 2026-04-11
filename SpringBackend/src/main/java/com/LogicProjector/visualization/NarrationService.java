package com.LogicProjector.visualization;

import org.springframework.stereotype.Service;

import com.LogicProjector.analysis.DetectedAlgorithm;

@Service
public class NarrationService {

    public NarrationResult createNarration(DetectedAlgorithm algorithm, VisualizationPayload payload, String sourceCode) {
        String summary = switch (algorithm) {
            case QUICK_SORT -> "Quick sort picks a pivot, partitions the array, and recursively sorts both sides.";
            case BINARY_SEARCH -> "Binary search keeps halving the search range until the target position is isolated.";
            case MERGE_SORT -> "Merge sort splits the array, sorts each half, and merges the results in order.";
            case BUBBLE_SORT -> "Bubble sort repeatedly compares adjacent values and swaps larger values to the right.";
            case SELECTION_SORT -> "Selection sort finds the smallest remaining value and places it into the sorted prefix.";
            case INSERTION_SORT -> "Insertion sort inserts each next value into the already sorted left side.";
            default -> "The algorithm is explained step by step through data structure changes.";
        };
        return new NarrationResult(summary);
    }
}
