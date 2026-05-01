package com.LogicProjector.exporttask;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.billing.BillingService;
import com.LogicProjector.exporttask.worker.MediaExportWorkerClient;
import com.LogicProjector.exporttask.worker.MediaExportWorkerRequest;
import com.LogicProjector.exporttask.worker.MediaExportWorkerResult;
import com.LogicProjector.systemlog.SystemLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ExportTaskProcessor {

    private final ExportTaskRepository exportTaskRepository;
    private final MediaExportWorkerClient workerClient;
    private final ObjectMapper objectMapper;
    private final BillingService billingService;
    private final SystemLogService systemLogService;

    public ExportTaskProcessor(ExportTaskRepository exportTaskRepository,
            MediaExportWorkerClient workerClient,
            ObjectMapper objectMapper,
            BillingService billingService,
            SystemLogService systemLogService) {
        this.exportTaskRepository = exportTaskRepository;
        this.workerClient = workerClient;
        this.objectMapper = objectMapper;
        this.billingService = billingService;
        this.systemLogService = systemLogService;
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
            billingService.settleExportCredits(exportTask.getUser(), exportTask.getGenerationTask(), exportTask, actualCharge);
            exportTask.complete(result.videoPath(), result.subtitlePath(), result.audioPath(), actualCharge);
            systemLogService.info(exportTask.getUser().getId(), exportTask.getId(), "export", "Export processing completed");
        } catch (IllegalStateException exception) {
            failAndRefund(exportTask, "INSUFFICIENT_CREDITS", "Export settlement failed after render");
        } catch (IOException exception) {
            failAndRefund(exportTask, "INVALID_VISUALIZATION_PAYLOAD", "Export failed due to invalid visualization payload");
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDispatch(Long exportTaskId, String errorMessage) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));

        if (exportTask.getStatus() == ExportTaskStatus.COMPLETED || exportTask.getStatus() == ExportTaskStatus.FAILED) {
            return;
        }

        billingService.releaseExportCredits(exportTask.getUser(), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(errorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", "Export dispatch failed", errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDeadLetter(Long exportTaskId, String errorMessage) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));

        if (exportTask.getStatus() == ExportTaskStatus.COMPLETED || exportTask.getStatus() == ExportTaskStatus.FAILED) {
            return;
        }

        billingService.releaseExportCredits(exportTask.getUser(), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(errorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", "Export moved to dead-letter queue", errorMessage);
    }

    private void failAndRefund(ExportTask exportTask, String errorMessage, String logMessage) {
        billingService.releaseExportCredits(exportTask.getUser(), exportTask.getGenerationTask(), exportTask);
        exportTask.fail(errorMessage);
        systemLogService.error(exportTask.getUser().getId(), exportTask.getId(), "export", logMessage, errorMessage);
    }
}
