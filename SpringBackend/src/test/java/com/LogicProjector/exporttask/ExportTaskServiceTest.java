package com.LogicProjector.exporttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;
import com.LogicProjector.exporttask.worker.MediaExportWorkerClient;
import com.LogicProjector.exporttask.worker.MediaExportWorkerResult;
import com.LogicProjector.generation.GenerationTask;
import com.LogicProjector.generation.GenerationTaskRepository;
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

    @MockBean
    private MediaExportWorkerClient workerClient;

    private Long generationTaskId;
    private Long userId;

    @BeforeEach
    void setUp() {
        exportTaskRepository.deleteAll();
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
    void shouldCreatePendingExportTaskAndCompleteOnPoll() {
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);

        assertThat(createResponse.status()).isEqualTo("PENDING");
        assertThat(createResponse.progress()).isEqualTo(0);
        assertThat(createResponse.creditsFrozen()).isEqualTo(18);

        ExportTaskResponse pollResponse = exportTaskService.getExportTask(createResponse.id(), userId);

        assertThat(pollResponse.status()).isEqualTo("COMPLETED");
        assertThat(pollResponse.progress()).isEqualTo(100);
        assertThat(pollResponse.videoUrl()).contains("/api/export-tasks/");
        assertThat(pollResponse.creditsCharged()).isEqualTo(1231);
    }

    @Test
    void shouldRejectDownloadBeforeCompletion() {
        CreateExportTaskResponse createResponse = exportTaskService.createExportTask(generationTaskId, userId);

        assertThatThrownBy(() -> exportTaskService.download(createResponse.id(), userId))
                .isInstanceOf(ExportTaskException.class)
                .hasMessageContaining("EXPORT_NOT_READY");
    }
}
