package com.LogicProjector.analysis;

@FunctionalInterface
public interface AiCodeAnalysisClient {

    RecognitionResult analyze(String sourceCode, String language);
}
