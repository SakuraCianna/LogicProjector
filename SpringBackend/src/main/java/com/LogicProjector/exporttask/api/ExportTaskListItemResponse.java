package com.LogicProjector.exporttask.api;

import com.LogicProjector.exporttask.ExportTask;

public record ExportTaskListItemResponse(
        Long id,
        Long generationTaskId,
        String status,
        String detectedAlgorithm,
        String createdAt,
        String updatedAt
) {

    public static ExportTaskListItemResponse from(ExportTask exportTask) {
        return new ExportTaskListItemResponse(
                exportTask.getId(),
                exportTask.getGenerationTask().getId(),
                exportTask.getStatus().name(),
                exportTask.getGenerationTask().getDetectedAlgorithm(),
                exportTask.getCreatedAt().toString(),
                exportTask.getUpdatedAt().toString());
    }
}
