package com.LogicProjector.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlgorithmRecognitionServiceImpl implements AlgorithmRecognitionService {

    private final AiCodeAnalysisClient aiCodeAnalysisClient;
    private final double confidenceThreshold;

    public AlgorithmRecognitionServiceImpl(AiCodeAnalysisClient aiCodeAnalysisClient,
            @Value("${pas.ai.confidence-threshold}") double confidenceThreshold) {
        this.aiCodeAnalysisClient = aiCodeAnalysisClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    public RecognitionResult recognize(String sourceCode, String language) {
        RecognitionResult result = aiCodeAnalysisClient.analyze(sourceCode, language);
        if (result.algorithm() == DetectedAlgorithm.UNKNOWN || result.confidence() < confidenceThreshold) {
            throw new UnsupportedAlgorithmException(
                    "Unsupported algorithm or low confidence: " + result.confidence());
        }
        return result;
    }
}
