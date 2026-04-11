package com.LogicProjector.analysis;

import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleCodeAnalysisClient implements AiCodeAnalysisClient {

    @Override
    public RecognitionResult analyze(String sourceCode) {
        String normalized = sourceCode.toLowerCase();

        if (normalized.contains("pivot") || normalized.contains("partition")) {
            return new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.91, "Pivot and partition detected");
        }
        if (normalized.contains("mid") && normalized.contains("left") && normalized.contains("right")) {
            return new RecognitionResult(DetectedAlgorithm.BINARY_SEARCH, 0.87, "Binary search boundaries detected");
        }
        if (normalized.contains("arr[j] > arr[j + 1]")) {
            return new RecognitionResult(DetectedAlgorithm.BUBBLE_SORT, 0.88, "Adjacent comparison swap loop detected");
        }
        if (normalized.contains("minindex") || normalized.contains("min_index")) {
            return new RecognitionResult(DetectedAlgorithm.SELECTION_SORT, 0.86, "Minimum selection loop detected");
        }
        if (normalized.contains("key =") || normalized.contains("current =")) {
            return new RecognitionResult(DetectedAlgorithm.INSERTION_SORT, 0.84, "Insertion step variable detected");
        }
        if (normalized.contains("merge(") && normalized.contains("mid")) {
            return new RecognitionResult(DetectedAlgorithm.MERGE_SORT, 0.89, "Merge recursion detected");
        }

        return new RecognitionResult(DetectedAlgorithm.UNKNOWN, 0.35, "No supported pattern found");
    }
}
