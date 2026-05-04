package com.LogicProjector.exporttask.worker;

import java.util.List;

public record MediaExportWorkerResult(
        String status,
        int progress,
        String videoPath,
        String subtitlePath,
        String audioPath,
        int tokenUsage,
        int renderSeconds,
        int concurrencyUnits,
        String errorMessage,
        List<String> warnings
) {
    public MediaExportWorkerResult(
            String status,
            int progress,
            String videoPath,
            String subtitlePath,
            String audioPath,
            int tokenUsage,
            int renderSeconds,
            int concurrencyUnits,
            String errorMessage) {
        this(status, progress, videoPath, subtitlePath, audioPath, tokenUsage, renderSeconds, concurrencyUnits, errorMessage, List.of());
    }

    public MediaExportWorkerResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
