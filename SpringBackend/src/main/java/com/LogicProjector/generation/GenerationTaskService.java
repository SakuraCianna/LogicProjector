package com.LogicProjector.generation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.analysis.AlgorithmRecognitionService;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.RecognitionResult;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
import com.LogicProjector.generation.api.GenerationTaskResponse;
import com.LogicProjector.systemlog.SystemLogService;
import com.LogicProjector.visualization.NarrationResult;
import com.LogicProjector.visualization.NarrationService;
import com.LogicProjector.visualization.VisualizationPayload;
import com.LogicProjector.visualization.VisualizationStateExtractorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GenerationTaskService {

    private final UserAccountRepository userAccountRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final AlgorithmRecognitionService algorithmRecognitionService;
    private final VisualizationStateExtractorFactory visualizationStateExtractorFactory;
    private final NarrationService narrationService;
    private final BillingService billingService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    public GenerationTaskService(UserAccountRepository userAccountRepository,
            GenerationTaskRepository generationTaskRepository,
            AlgorithmRecognitionService algorithmRecognitionService,
            VisualizationStateExtractorFactory visualizationStateExtractorFactory,
            NarrationService narrationService,
            BillingService billingService,
            SystemLogService systemLogService,
            ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.algorithmRecognitionService = algorithmRecognitionService;
        this.visualizationStateExtractorFactory = visualizationStateExtractorFactory;
        this.narrationService = narrationService;
        this.billingService = billingService;
        this.systemLogService = systemLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationTaskResponse createTask(CreateGenerationTaskRequest request) {
        UserAccount user = userAccountRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));

        GenerationTask task = generationTaskRepository.save(GenerationTask.pending(user, request.sourceCode(), request.language()));

        RecognitionResult recognition = algorithmRecognitionService.recognize(request.sourceCode());
        List<Integer> sampleInput = sampleInputFor(recognition.algorithm());
        VisualizationPayload payload = visualizationStateExtractorFactory.forAlgorithm(recognition.algorithm())
                .extract(recognition.algorithm().name(), sampleInput, request.sourceCode());
        NarrationResult narration = narrationService.createNarration(recognition.algorithm(), payload, request.sourceCode());
        JsonNode payloadJson = objectMapper.valueToTree(payload);

        task.complete(recognition.algorithm().name(), recognition.confidence(), payloadJson, narration.summary());
        int creditsCharged = billingService.chargeForCompletedGeneration(user, task);
        systemLogService.info(user.getId(), task.getId(), "generation", "Generation completed successfully");

        return new GenerationTaskResponse(
                task.getId(),
                task.getStatus().name(),
                task.getLanguage(),
                task.getDetectedAlgorithm(),
                task.getSummary(),
                task.getConfidenceScore(),
                payloadJson,
                task.getErrorMessage(),
                creditsCharged);
    }

    public GenerationTaskResponse getTask(Long taskId) {
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        JsonNode payloadJson = null;
        if (task.getVisualizationPayloadJson() != null) {
            try {
                payloadJson = objectMapper.readTree(task.getVisualizationPayloadJson());
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to parse visualization payload", exception);
            }
        }

        return new GenerationTaskResponse(
                task.getId(),
                task.getStatus().name(),
                task.getLanguage(),
                task.getDetectedAlgorithm(),
                task.getSummary(),
                task.getConfidenceScore() == null ? 0.0 : task.getConfidenceScore(),
                payloadJson,
                task.getErrorMessage(),
                0);
    }

    private List<Integer> sampleInputFor(DetectedAlgorithm algorithm) {
        return switch (algorithm) {
            case BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, QUICK_SORT, MERGE_SORT -> List.of(5, 1, 4, 2, 8);
            case BINARY_SEARCH -> List.of(1, 3, 5, 7, 9, 11);
            default -> List.of(3, 1, 2);
        };
    }
}
