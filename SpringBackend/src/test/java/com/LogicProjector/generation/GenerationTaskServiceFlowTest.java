package com.LogicProjector.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.analysis.AiChatClient;
import com.LogicProjector.analysis.AlgorithmRecognitionService;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.RecognitionResult;
import com.LogicProjector.billing.BillingRecordRepository;
import com.LogicProjector.exporttask.ExportTaskRepository;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
import com.LogicProjector.generation.api.GenerationTaskResponse;
import com.LogicProjector.queue.TaskMessagePublisher;
import com.LogicProjector.systemlog.SystemLogEntryRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pas-task-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GenerationTaskServiceFlowTest {

    private static final String QUICK_SORT_SOURCE = """
            public class QuickSort {
                void sort(int[] arr, int low, int high) {
                    int pivot = arr[high];
                    partition(arr, low, high);
                }
            }
            """;

    @Autowired
    private GenerationTaskService service;

    @Autowired
    private GenerationTaskProcessor generationTaskProcessor;

    @Autowired
    private GenerationTaskRepository generationTaskRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BillingRecordRepository billingRecordRepository;

    @Autowired
    private SystemLogEntryRepository systemLogEntryRepository;

    @MockBean
    private AlgorithmRecognitionService algorithmRecognitionService;

    @MockBean
    private AiChatClient aiChatClient;

    @MockBean
    private TaskMessagePublisher taskMessagePublisher;

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

        given(algorithmRecognitionService.recognize(anyString()))
                .willReturn(new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.93, "Pivot and partition detected"));
        given(aiChatClient.createStructuredResponse(anyString(), anyString()))
                .willAnswer(invocation -> {
                    String userPrompt = invocation.getArgument(1, String.class);
                    Matcher matcher = Pattern.compile("Steps: (\\d+)").matcher(userPrompt);
                    int stepCount = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
                    var response = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                            .put("summary", "Quick sort picks a pivot and partitions the array.");
                    var stepNarrations = response.putArray("stepNarrations");
                    for (int index = 0; index < stepCount; index++) {
                        stepNarrations.add("AI narration step " + (index + 1));
                    }
                    return response;
                });
    }

    @Test
    void shouldCreatePendingTaskAndPublishMessage() {
        GenerationTaskResponse response = service.createTask(
                new CreateGenerationTaskRequest(userId, QUICK_SORT_SOURCE, "java")
        );

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.detectedAlgorithm()).isNull();
        assertThat(response.visualizationPayload()).isNull();
        verify(taskMessagePublisher).publishGenerationTask(response.id(), userId);
    }

    @Test
    void shouldCompleteTaskAndChargeCreditsWhenProcessorRuns() {
        GenerationTask task = generationTaskRepository.save(GenerationTask.pending(
                userAccountRepository.findById(userId).orElseThrow(),
                QUICK_SORT_SOURCE,
                "java"));

        generationTaskProcessor.process(task.getId());

        GenerationTaskResponse response = service.getTask(task.getId());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.detectedAlgorithm()).isEqualTo("QUICK_SORT");
        assertThat(response.summary()).contains("pivot");
        assertThat(response.visualizationPayload()).isNotNull();
        assertThat(response.visualizationPayload().path("steps").get(0).path("narration").asText())
                .isEqualTo("AI narration step 1");
        assertThat(billingRecordRepository.count()).isEqualTo(1);
        assertThat(systemLogEntryRepository.count()).isEqualTo(2);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getCreditsBalance).isEqualTo(112);
    }
}
