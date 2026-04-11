package com.LogicProjector.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.analysis.AlgorithmRecognitionService;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.RecognitionResult;
import com.LogicProjector.billing.BillingRecordRepository;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
import com.LogicProjector.generation.api.GenerationTaskResponse;
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
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BillingRecordRepository billingRecordRepository;

    @Autowired
    private SystemLogEntryRepository systemLogEntryRepository;

    @MockBean
    private AlgorithmRecognitionService algorithmRecognitionService;

    private Long userId;

    @BeforeEach
    void setUp() {
        billingRecordRepository.deleteAll();
        systemLogEntryRepository.deleteAll();
        userAccountRepository.deleteAll();

        UserAccount user = userAccountRepository.save(new UserAccount(null, "teacher@example.com", 120, "ACTIVE"));
        userId = user.getId();

        given(algorithmRecognitionService.recognize(anyString()))
                .willReturn(new RecognitionResult(DetectedAlgorithm.QUICK_SORT, 0.93, "Pivot and partition detected"));
    }

    @Test
    void shouldCompleteTaskAndChargeCredits() {
        GenerationTaskResponse response = service.createTask(
                new CreateGenerationTaskRequest(userId, QUICK_SORT_SOURCE, "java")
        );

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.detectedAlgorithm()).isEqualTo("QUICK_SORT");
        assertThat(response.summary()).contains("pivot");
        assertThat(response.visualizationPayload()).isNotNull();
        assertThat(response.creditsCharged()).isEqualTo(8);
        assertThat(billingRecordRepository.count()).isEqualTo(1);
        assertThat(systemLogEntryRepository.count()).isEqualTo(1);
        assertThat(userAccountRepository.findById(userId)).get().extracting(UserAccount::getCreditsBalance).isEqualTo(112);
    }
}
