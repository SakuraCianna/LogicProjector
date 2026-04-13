package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pas-export-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "pas.export.freeze-estimate=18"
})
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

    private Long generationTaskId;
    private Long userId;

    @BeforeEach
    void setUp() {
        exportTaskRepository.deleteAll();
        billingRecordRepository.deleteAll();
        systemLogEntryRepository.deleteAll();
        generationTaskRepository.deleteAll();
        userAccountRepository.deleteAll();

        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher@example.com", 120, 0, "ACTIVE"));
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
        UserAccount lowCreditUser = userAccountRepository.save(new UserAccount(null, "low@example.com", 5, 0, "ACTIVE"));

        assertThatThrownBy(() -> exportTaskService.createExportTask(generationTaskId, lowCreditUser.getId()))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("INSUFFICIENT_CREDITS");
    }
}
