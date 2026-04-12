package com.LogicProjector.exporttask.worker;

import com.fasterxml.jackson.databind.JsonNode;

public record MediaExportWorkerRequest(
        Long exportTaskId,
        Long generationTaskId,
        String algorithm,
        String summary,
        JsonNode visualizationPayload,
        String sourceCode,
        boolean subtitleEnabled,
        boolean ttsEnabled
) {
}
