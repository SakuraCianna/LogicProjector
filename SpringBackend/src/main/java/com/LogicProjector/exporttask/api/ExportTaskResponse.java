package com.LogicProjector.exporttask.api;

import com.LogicProjector.exporttask.ExportTask;

public record ExportTaskResponse(
        Long id,
        Long generationTaskId,
        String status,
        int progress,
        String videoUrl,
        String subtitleUrl,
        String audioUrl,
        String errorMessage,
        String warningMessage,
        Integer creditsFrozen,
        Integer creditsCharged,
        String createdAt,
        String updatedAt
) {

    public static ExportTaskResponse from(ExportTask exportTask) {
        String videoUrl = exportTask.getVideoPath() == null ? null : "/api/export-tasks/" + exportTask.getId() + "/download";
        return new ExportTaskResponse(
                exportTask.getId(),
                exportTask.getGenerationTask().getId(),
                exportTask.getStatus().name(),
                exportTask.getProgress(),
                videoUrl,
                exportTask.getSubtitlePath(),
                exportTask.getAudioPath(),
                exportTask.getErrorMessage(),
                exportTask.getWarningMessage(),
                exportTask.getCreditsFrozen(),
                exportTask.getCreditsCharged(),
                exportTask.getCreatedAt().toString(),
                exportTask.getUpdatedAt().toString());
    }
}
