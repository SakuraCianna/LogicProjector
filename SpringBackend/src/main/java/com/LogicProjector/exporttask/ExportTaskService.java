package com.LogicProjector.exporttask;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;
import com.LogicProjector.queue.TaskMessagePublisher;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
import com.LogicProjector.generation.GenerationTaskStatus;

@Service
public class ExportTaskService {

    private final ExportTaskRepository exportTaskRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final UserAccountRepository userAccountRepository;
    private final TaskMessagePublisher taskMessagePublisher;
    private final BillingService billingService;
    private final int freezeEstimate;
    private final String downloadRoot;

    public ExportTaskService(ExportTaskRepository exportTaskRepository,
            GenerationTaskRepository generationTaskRepository,
            UserAccountRepository userAccountRepository,
            TaskMessagePublisher taskMessagePublisher,
            BillingService billingService,
            @Value("${pas.export.freeze-estimate}") int freezeEstimate,
            @Value("${pas.export.download-root}") String downloadRoot) {
        this.exportTaskRepository = exportTaskRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.userAccountRepository = userAccountRepository;
        this.taskMessagePublisher = taskMessagePublisher;
        this.billingService = billingService;
        this.freezeEstimate = freezeEstimate;
        this.downloadRoot = downloadRoot;
    }

    @Transactional
    public CreateExportTaskResponse createExportTask(Long generationTaskId, Long userId) {
        GenerationTask generationTask = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new ExportTaskException("GENERATION_NOT_FOUND"));
        if (generationTask.getStatus() != GenerationTaskStatus.COMPLETED) {
            throw new ExportTaskException("GENERATION_NOT_READY");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ExportTaskException("USER_NOT_FOUND"));

        try {
            user.freezeCredits(freezeEstimate);
        } catch (IllegalStateException exception) {
            throw new ExportTaskException("INSUFFICIENT_CREDITS");
        }

        ExportTask exportTask = exportTaskRepository.save(ExportTask.pending(generationTask, user, freezeEstimate));
        billingService.recordExportFreeze(user, generationTask, exportTask);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskMessagePublisher.publishExportTask(exportTask.getId(), user.getId());
            }
        });

        return new CreateExportTaskResponse(
                exportTask.getId(),
                generationTask.getId(),
                exportTask.getStatus().name(),
                exportTask.getProgress(),
                exportTask.getCreditsFrozen());
    }

    @Transactional
    public ExportTaskResponse getExportTask(Long exportTaskId, Long userId) {
        ExportTask exportTask = getOwnedExportTask(exportTaskId, userId);
        return ExportTaskResponse.from(exportTask);
    }

    public ResponseEntity<FileSystemResource> download(Long exportTaskId, Long userId) {
        ExportTask exportTask = getOwnedExportTask(exportTaskId, userId);
        if (exportTask.getStatus() != ExportTaskStatus.COMPLETED || exportTask.getVideoPath() == null) {
            throw new ExportTaskException("EXPORT_NOT_READY");
        }

        FileSystemResource resource = new FileSystemResource(Path.of(downloadRoot).resolve(Path.of(exportTask.getVideoPath()).getFileName()));
        if (!resource.exists()) {
            resource = new FileSystemResource(exportTask.getVideoPath());
        }
        if (!resource.exists()) {
            throw new ExportTaskException("EXPORT_FILE_MISSING");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pas-export-" + exportTaskId + ".mp4")
                .body(resource);
    }

    private ExportTask getOwnedExportTask(Long exportTaskId, Long userId) {
        ExportTask exportTask = exportTaskRepository.findById(exportTaskId)
                .orElseThrow(() -> new ExportTaskException("EXPORT_NOT_FOUND"));
        if (!exportTask.getUser().getId().equals(userId)) {
            throw new ExportTaskException("EXPORT_NOT_OWNED_BY_USER");
        }
        return exportTask;
    }
}
