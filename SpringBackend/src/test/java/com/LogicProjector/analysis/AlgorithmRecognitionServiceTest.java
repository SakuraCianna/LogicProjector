package com.LogicProjector.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AlgorithmRecognitionServiceTest {

    private final AiCodeAnalysisClient aiCodeAnalysisClient = (sourceCode, language) ->
            new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.91, "Pivot and partition detected");

    private final AlgorithmRecognitionService service =
            new AlgorithmRecognitionServiceImpl(aiCodeAnalysisClient, 0.80);

    @Test
    void shouldAcceptSupportedAlgorithmAboveThreshold() {
        RecognitionResult result = service.recognize("quickSort(nums, low, high);", "cpp");

        assertThat(result.algorithm()).isEqualTo(DetectedAlgorithm.QUICK_SORT);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    void shouldRejectUnsupportedAlgorithmBelowThreshold() {
        AlgorithmRecognitionService rejectingService = new AlgorithmRecognitionServiceImpl(
                (sourceCode, language) -> new RecognitionResult(DetectedAlgorithm.UNKNOWN, 0.42, "No supported pattern found"),
                0.80
        );

        assertThatThrownBy(() -> rejectingService.recognize("dp[i] = Math.max(dp[i - 1], score);", "java") )
                .isInstanceOf(UnsupportedAlgorithmException.class)
                .hasMessageContaining("confidence");
    }
}
