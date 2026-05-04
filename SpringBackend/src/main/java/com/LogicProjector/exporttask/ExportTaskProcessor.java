package com.LogicProjector.exporttask;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.exporttask.worker.MediaExportWorkerClient;
import com.LogicProjector.exporttask.worker.MediaExportWorkerRequest;
import com.LogicProjector.exporttask.worker.MediaExportWorkerResult;
import com.LogicProjector.systemlog.SystemLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ExportTaskProcessor {

    private static final int TASK_ERROR_MESSAGE_MAX_LENGTH = 255;

    private final ExportTaskRepository exportTaskRepository;
    private final MediaExportWorkerClient workerClient;
    private final ObjectMapper objectMapper;
    private final BillingService billingService;
    private final SystemLogService systemLogService;
    private final UserAccountRepository userAccountRepository;

    public ExportTaskProcessor(ExportTaskRepository exportTaskRepository,
            MediaExportWorkerClient workerClient,
            ObjectMapper objectMapper,
            BillingService billingService,
            SystemLogService systemLogService,
            UserAccountRepository userAccountRepository) {
        this.exportTaskRepository = exportTaskRepository;
        this.workerClient = workerClient;
        this.objectMapper = objectMapper;
        this.billingService = billingService;
        this.systemLogService = systemLogService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long exportTaskId) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));

        if (exportTask.getStatus() == ExportTaskStatus.COMPLETED || exportTask.getStatus() == ExportTaskStatus.FAILED) {
            return;
        }

        exportTask.markProcessing();
        systemLogService.info(exportTask.getUser().getId(), exportTask.getId(), "export", "Export processing started");

        try {
            JsonNode payloadJson = objectMapper.readTree(exportTask.getGenerationTask().getVisualizationPayloadJson());
            MediaExportWorkerResult result = workerClient.createExport(new MediaExportWorkerRequest(
                    exportTask.getId(),
                    exportTask.getGenerationTask().getId(),
                    exportTask.getGenerationTask().getDetectedAlgorithm(),
                    exportTask.getGenerationTask().getSummary(),
                    payloadJson,
                    exportTask.getGenerationTask().getSourceCode(),
                    true,
                    true));

            if (!"COMPLETED".equals(result.status())) {
                failAndRefund(exportTask, result.errorMessage() == null ? "WORKER_UNAVAILABLE" : result.errorMessage(), "Export worker returned terminal failure");
                return;
            }

            int actualCharge = result.tokenUsage() + result.renderSeconds() + result.concurrencyUnits();
            UserAccount user = lockedUser(exportTask);
            try {
                billingService.settleExportCredits(user, exportTask.getGenerationTask(), exportTask, actualCharge);
            } catch (IllegalStateException settlementException) {
                failAndRefund(exportTask, "INSUFFICIENT_CREDITS", "Export settlement failed after render");
                return;
            }
            exportTask.complete(result.videoPath(), result.subtitlePath(), result.audioPath(), actualCharge,
                    summarizeWarnings(result.warnings()));
            systemLogService.info(exportTask.getUser().getId(), exportTask.getId(), "export", "Export processing completed");
        } catch (IOException exception) {
            failAndRefund(exportTask, "INVALID_VISUALIZATION_PAYLOAD", "Export failed due to invalid visualization payload");
        } catch (RuntimeException exception) {
            failAndRefund(exportTask, summarizeErrorMessage(exception.getMessage(), "EXPORT_PROCESSING_ERROR"), "Export processing failed unexpectedly");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDispatch(Long exportTaskId, String errorMessage) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));

        if (exportTask.getStatus() == ExportTaskStatus.COMPLETED || exportTask.getStatus() == ExportTaskStatus.FAILED) {
            return;
        }

        String taskErrorMessage = summarizeErrorMessage(errorMessage, "EXPORT_DISPATCH_FAILED");
        billingService.releaseExportCredits(lockedUser(exportTask), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(taskErrorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", "Export dispatch failed", errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDeadLetter(Long exportTaskId, String errorMessage) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));

        if (exportTask.getStatus() == ExportTaskStatus.COMPLETED || exportTask.getStatus() == ExportTaskStatus.FAILED) {
            return;
        }

        String taskErrorMessage = summarizeErrorMessage(errorMessage, "EXPORT_RETRY_EXHAUSTED");
        billingService.releaseExportCredits(lockedUser(exportTask), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(taskErrorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", "Export moved to dead-letter queue", errorMessage);
    }

    private void failAndRefund(ExportTask exportTask, String errorMessage, String logMessage) {
        String taskErrorMessage = summarizeErrorMessage(errorMessage, "EXPORT_FAILED");
        billingService.releaseExportCredits(lockedUser(exportTask), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(taskErrorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", logMessage, errorMessage);
    }

    private UserAccount lockedUser(ExportTask exportTask) {
        return userAccountRepository.findByIdForUpdate(exportTask.getUser().getId())
                .orElseThrow(() -> new ExportTaskException("USER_NOT_FOUND"));
    }

    private String summarizeErrorMessage(String errorMessage, String fallback) {
        if (errorMessage == null) {
            return fallback;
        }

        String normalized = errorMessage.replace('\r', '\n');
        int newlineIndex = normalized.indexOf('\n');
        String summary = newlineIndex >= 0 ? normalized.substring(0, newlineIndex) : normalized;
        summary = summary.trim();

        if (summary.isEmpty()) {
            return fallback;
        }

        if (summary.length() <= TASK_ERROR_MESSAGE_MAX_LENGTH) {
            return summary;
        }

        return summary.substring(0, TASK_ERROR_MESSAGE_MAX_LENGTH - 3) + "...";
    }

    private String summarizeWarnings(java.util.List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }
        String summary = String.join(",", warnings).trim();
        if (summary.isEmpty()) {
            return null;
        }
        if (summary.length() <= TASK_ERROR_MESSAGE_MAX_LENGTH) {
            return summary;
        }
        return summary.substring(0, TASK_ERROR_MESSAGE_MAX_LENGTH - 3) + "...";
    }
}
