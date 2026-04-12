package com.LogicProjector.exporttask.worker;

public record MediaExportWorkerResult(
        String status,
        int progress,
        String videoPath,
        String subtitlePath,
        String audioPath,
        int tokenUsage,
        int renderSeconds,
        int concurrencyUnits,
        String errorMessage
) {
}
