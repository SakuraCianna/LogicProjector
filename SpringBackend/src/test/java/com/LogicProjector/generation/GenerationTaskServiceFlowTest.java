package com.LogicProjector.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

@Disabled("Requires a dedicated MySQL test database; do not run against the local development database.")
@SpringBootTest
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

    @MockitoBean
    private AlgorithmRecognitionService algorithmRecognitionService;

    @MockitoBean
    private AiChatClient aiChatClient;

    @MockitoBean
    private TaskMessagePublisher taskMessagePublisher;

    private Long userId;

    @BeforeEach
    void setUp() {
        exportTaskRepository.deleteAll();
        billingRecordRepository.deleteAll();
        systemLogEntryRepository.deleteAll();
        generationTaskRepository.deleteAll();
        userAccountRepository.deleteAll();

        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher", "hash", 120, 0, "ACTIVE"));
        userId = user.getId();

        given(algorithmRecognitionService.recognize(anyString()))
                .willReturn(new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.93, "Pivot and partition detected"));
        given(aiChatClient.createStructuredResponse(anyString(), anyString()))
                .willAnswer(invocation -> {
                    String userPrompt = invocation.getArgument(1, String.class);
                    Matcher matcher = Pattern.compile("Step count: (\\d+)").matcher(userPrompt);
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
                new CreateGenerationTaskRequest(QUICK_SORT_SOURCE, "java"),
                userId
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

        GenerationTaskResponse response = service.getTask(task.getId(), userId);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.detectedAlgorithm()).isEqualTo("QUICK_SORT");
        assertThat(response.summary()).contains("pivot");
        assertThat(response.visualizationPayload()).isNotNull();
        assertThat(response.creditsCharged()).isEqualTo(8);
        assertThat(response.visualizationPayload().path("steps").get(0).path("narration").asText())
                .isEqualTo("AI narration step 1");
        assertThat(billingRecordRepository.count()).isEqualTo(1);
        assertThat(systemLogEntryRepository.count()).isEqualTo(2);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getCreditsBalance).isEqualTo(112);
    }

    @Test
    void shouldRejectTaskCreationWhenGenerationCreditsAreInsufficient() {
        UserAccount lowCreditUser = userAccountRepository.save(new UserAccount(null, "low-credit", "hash", 3, 0, "ACTIVE"));

        assertThatThrownBy(() -> service.createTask(new CreateGenerationTaskRequest(QUICK_SORT_SOURCE, "java"), lowCreditUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INSUFFICIENT_CREDITS");
    }

    @Test
    void shouldRejectNonJavaLanguageBeforeQueueingTask() {
        assertThatThrownBy(() -> service.createTask(new CreateGenerationTaskRequest(QUICK_SORT_SOURCE, "python"), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JAVA_ONLY");
    }

    @Test
    void shouldFailTaskIfDispatchToQueueFailsAfterCommit() {
        doThrow(new RuntimeException("AMQP_DOWN")).when(taskMessagePublisher).publishGenerationTask(anyLong(), anyLong());

        GenerationTaskResponse response = service.createTask(new CreateGenerationTaskRequest(QUICK_SORT_SOURCE, "java"), userId);

        assertThat(service.getTask(response.id(), userId).status()).isEqualTo("FAILED");
        assertThat(service.getTask(response.id(), userId).errorMessage()).isEqualTo("GENERATION_DISPATCH_FAILED");
    }

    @Test
    void shouldFailUnsupportedAlgorithmWithoutChargingCredits() {
        given(algorithmRecognitionService.recognize(anyString()))
                .willThrow(new com.LogicProjector.analysis.UnsupportedAlgorithmException("Unsupported algorithm or low confidence: 0.41"));
        GenerationTask task = generationTaskRepository.save(GenerationTask.pending(
                userAccountRepository.findById(userId).orElseThrow(),
                QUICK_SORT_SOURCE,
                "java"));

        generationTaskProcessor.process(task.getId());

        GenerationTaskResponse response = service.getTask(task.getId(), userId);
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorMessage()).contains("Unsupported algorithm");
        assertThat(billingRecordRepository.count()).isEqualTo(0);
    }
}
