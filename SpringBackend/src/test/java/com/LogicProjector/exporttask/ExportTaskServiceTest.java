package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.billing.BillingRecordRepository;
import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;
import com.LogicProjector.exporttask.worker.MediaExportWorkerClient;
import com.LogicProjector.exporttask.worker.MediaExportWorkerResult;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
import com.LogicProjector.queue.TaskMessagePublisher;
import com.LogicProjector.systemlog.SystemLogEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Disabled("Requires a dedicated MySQL test database; do not run against the local development database.")
@SpringBootTest(properties = "pas.export.freeze-estimate=18")
class ExportTaskServiceTest {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private ExportTaskProcessor exportTaskProcessor;

    @Autowired
    private BillingRecordRepository billingRecordRepository;

    @Autowired
    private SystemLogEntryRepository systemLogEntryRepository;

    @MockBean
    private MediaExportWorkerClient workerClient;

    @MockBean
    private TaskMessagePublisher taskMessagePublisher;

    @TempDir
    Path tempDir;

    private Long generationTaskId;
    private Long userId;

    @BeforeEach
    void setUp() {
        exportTaskRepository.deleteAll();
        billingRecordRepository.deleteAll();
        systemLogEntryRepository.deleteAll();
        generationTaskRepository.deleteAll();
        userAccountRepository.deleteAll();

        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher", "hash", 2000, 0, "ACTIVE"));
        userId = user.getId();

        GenerationTask generationTask = generationTaskRepository.save(GenerationTask.pending(user, "class Demo {}", "java"));
        generationTask.complete("QUICK_SORT", 0.93, new ObjectMapper().createObjectNode().put("algorithm", "QUICK_SORT"), "summary");
        generationTask = generationTaskRepository.save(generationTask);
        generationTaskId = generationTask.getId();

        given(workerClient.createExport(any())).willReturn(new MediaExportWorkerResult(
                "COMPLETED", 100, "outputs/101.mp4", "outputs/101.srt", "outputs/101.mp3", 1200, 30, 1, null
        ));
    }

    @Test
    void shouldCreatePendingExportTaskAndPublishMessage() {
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);

        assertThat(createResponse.status()).isEqualTo("PENDING");
        assertThat(createResponse.progress()).isEqualTo(0);
        assertThat(createResponse.creditsFrozen()).isEqualTo(18);

        verify(taskMessagePublisher).publishExportTask(createResponse.id(), userId);
    }

    @Test
    void shouldCompleteExportTaskWhenProcessorRuns() {
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);
        exportTaskProcessor.process(createResponse.id());

        ExportTaskResponse pollResponse = exportTaskService.getExportTask(createResponse.id(), userId);

        assertThat(pollResponse.status()).isEqualTo("COMPLETED");
        assertThat(pollResponse.progress()).isEqualTo(100);
        assertThat(pollResponse.videoUrl()).contains("/api/export-tasks/");
        assertThat(pollResponse.creditsCharged()).isEqualTo(1231);
        assertThat(billingRecordRepository.count()).isEqualTo(2);
        assertThat(systemLogEntryRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }

    @Test
    void shouldRejectDownloadBeforeCompletion() {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        GenerationTask generationTask = generationTaskRepository.findById(generationTaskId).orElseThrow();
        user.freezeCredits(18);
        ExportTask exportTask = exportTaskRepository.save(ExportTask.pending(generationTask, user, 18));

        assertThatThrownBy(() -> exportTaskService.download(exportTask.getId(), userId))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("EXPORT_NOT_READY");
    }

    @Test
    void shouldReturnReadableErrorWhenCreditsAreInsufficient() {
        UserAccount lowCreditUser = userAccountRepository.save(new UserAccount(null, "low", "hash", 5, 0, "ACTIVE"));
        GenerationTask lowCreditTask = generationTaskRepository.save(GenerationTask.pending(lowCreditUser, "class LowCredit {}", "java"));
        lowCreditTask.complete("QUICK_SORT", 0.93, new ObjectMapper().createObjectNode().put("algorithm", "QUICK_SORT"), "summary");
        lowCreditTask = generationTaskRepository.save(lowCreditTask);
        Long lowCreditTaskId = lowCreditTask.getId();

        assertThatThrownBy(() -> exportTaskService.createExportTask(lowCreditTaskId, lowCreditUser.getId()))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("INSUFFICIENT_CREDITS");
    }

    @Test
    void shouldRejectExportForGenerationOwnedByAnotherUser() {
        UserAccount otherUser = userAccountRepository.save(new UserAccount(null, "other", "hash", 120, 0, "ACTIVE"));

        assertThatThrownBy(() -> exportTaskService.createExportTask(generationTaskId, otherUser.getId()))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("GENERATION_NOT_OWNED_BY_USER");
    }

    @Test
    void shouldRejectExportWhenGenerationIsNotCompleted() {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        GenerationTask pendingGenerationTask = generationTaskRepository.save(GenerationTask.pending(user, "class Pending {}", "java"));

        assertThatThrownBy(() -> exportTaskService.createExportTask(pendingGenerationTask.getId(), userId))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("GENERATION_NOT_READY");

        assertThat(exportTaskRepository.count()).isZero();
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }

    @Test
    void shouldRefundDifferenceWhenActualExportChargeIsLowerThanFrozenAmount() {
        given(workerClient.createExport(any())).willReturn(new MediaExportWorkerResult(
                "COMPLETED", 100, "outputs/101.mp4", "outputs/101.srt", "outputs/101.mp3", 5, 4, 1, null
        ));
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);

        exportTaskProcessor.process(createResponse.id());

        ExportTaskResponse pollResponse = exportTaskService.getExportTask(createResponse.id(), userId);
        assertThat(pollResponse.status()).isEqualTo("COMPLETED");
        assertThat(pollResponse.creditsCharged()).isEqualTo(10);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getCreditsBalance).isEqualTo(1990);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }

    @Test
    void shouldFailAndRefundWhenActualExportChargeExceedsAvailableCredits() {
        UserAccount lowCreditUser = userAccountRepository.save(new UserAccount(null, "settlement-low", "hash", 20, 0, "ACTIVE"));
        GenerationTask lowCreditTask = generationTaskRepository.save(GenerationTask.pending(lowCreditUser, "class LowSettlement {}", "java"));
        lowCreditTask.complete("QUICK_SORT", 0.93, new ObjectMapper().createObjectNode().put("algorithm", "QUICK_SORT"), "summary");
        lowCreditTask = generationTaskRepository.save(lowCreditTask);
        given(workerClient.createExport(any())).willReturn(new MediaExportWorkerResult(
                "COMPLETED", 100, "outputs/102.mp4", "outputs/102.srt", "outputs/102.mp3", 20, 9, 1, null
        ));
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(lowCreditTask.getId(), lowCreditUser.getId());

        exportTaskProcessor.process(createResponse.id());

        ExportTaskResponse pollResponse = exportTaskService.getExportTask(createResponse.id(), lowCreditUser.getId());
        assertThat(pollResponse.status()).isEqualTo("FAILED");
        assertThat(pollResponse.errorMessage()).isEqualTo("INSUFFICIENT_CREDITS");
        assertThat(userAccountRepository.findById(lowCreditUser.getId())).get().extracting(UserAccount::getCreditsBalance).isEqualTo(20);
        assertThat(userAccountRepository.findById(lowCreditUser.getId())).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }

    @Test
    void shouldNotReprocessCompletedExportTask() {
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);
        exportTaskProcessor.process(createResponse.id());
        long billingCountAfterCompletion = billingRecordRepository.count();

        exportTaskProcessor.process(createResponse.id());

        assertThat(billingRecordRepository.count()).isEqualTo(billingCountAfterCompletion);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
        verify(workerClient, times(1)).createExport(any());
    }

    @Test
    void shouldNotServeWorkerReturnedAbsolutePathOutsideDownloadRoot() throws Exception {
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        GenerationTask generationTask = generationTaskRepository.findById(generationTaskId).orElseThrow();
        user.freezeCredits(18);

        Path outsideVideo = Files.createFile(tempDir.resolve("outside-export.mp4"));
        ExportTask exportTask = exportTaskRepository.save(ExportTask.pending(generationTask, user, 18));
        exportTask.complete(outsideVideo.toString(), null, null, 18);
        exportTask = exportTaskRepository.save(exportTask);
        Long exportTaskId = exportTask.getId();

        assertThatThrownBy(() -> exportTaskService.download(exportTaskId, userId))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("EXPORT_FILE_MISSING");
    }

    @Test
    void shouldFailExportAndRefundWhenDispatchToQueueFailsAfterCommit() {
        doThrow(new RuntimeException("AMQP_DOWN")).when(taskMessagePublisher).publishExportTask(anyLong(), anyLong());

        CreateExportTaskResponse response = exportTaskService.createExportTask(generationTaskId, userId);

        assertThat(exportTaskService.getExportTask(response.id(), userId).status()).isEqualTo("FAILED");
        assertThat(exportTaskService.getExportTask(response.id(), userId).errorMessage()).isEqualTo("EXPORT_DISPATCH_FAILED");
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }

    @Test
    void shouldFailAndRefundWhenWorkerReturnsTerminalFailure() {
        given(workerClient.createExport(any())).willReturn(new MediaExportWorkerResult(
                "FAILED", 100, "", null, null, 0, 0, 1, "FFMPEG_MISSING"
        ));
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);

        exportTaskProcessor.process(createResponse.id());

        ExportTaskResponse pollResponse = exportTaskService.getExportTask(createResponse.id(), userId);
        assertThat(pollResponse.status()).isEqualTo("FAILED");
        assertThat(pollResponse.errorMessage()).isEqualTo("FFMPEG_MISSING");
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getFrozenCreditsBalance).isEqualTo(0);
    }
}
