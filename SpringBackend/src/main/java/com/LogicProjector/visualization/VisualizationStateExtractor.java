package com.LogicProjector.visualization;

import java.util.List;

@FunctionalInterface
public interface VisualizationStateExtractor {

    VisualizationPayload extract(String algorithm, List<Integer> input, String sourceCode);
}
