package com.LogicProjector.exporttask.api;

public record CreateExportTaskResponse(
        Long id,
        Long generationTaskId,
        String status,
        int progress,
        int creditsFrozen
) {
}
