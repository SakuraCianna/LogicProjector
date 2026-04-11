package com.LogicProjector.visualization;

import java.util.List;

public record VisualizationPayload(String algorithm, List<VisualizationStep> steps) {
}
