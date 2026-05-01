package com.LogicProjector.generation.api;

import com.LogicProjector.generation.GenerationTask;

public record GenerationTaskListItemResponse(
        Long id,
        String status,
        String detectedAlgorithm,
        String summary,
        String sourcePreview,
        String createdAt,
        String updatedAt
) {

    public static GenerationTaskListItemResponse from(GenerationTask task) {
        return new GenerationTaskListItemResponse(
                task.getId(),
                task.getStatus().name(),
                task.getDetectedAlgorithm(),
                task.getSummary(),
                buildSourcePreview(task.getSourceCode()),
                task.getCreatedAt().toString(),
                task.getUpdatedAt().toString());
    }

    private static String buildSourcePreview(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return "Untitled source";
        }

        String singleLine = sourceCode.strip().lines().findFirst().orElse(sourceCode.strip());
        return singleLine.length() <= 48 ? singleLine : singleLine.substring(0, 45) + "...";
    }
}
