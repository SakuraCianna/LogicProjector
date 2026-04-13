package com.LogicProjector.generation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.analysis.AlgorithmRecognitionService;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.RecognitionResult;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.systemlog.SystemLogService;
import com.LogicProjector.visualization.NarrationResult;
import com.LogicProjector.visualization.NarrationService;
import com.LogicProjector.visualization.VisualizationPayload;
import com.LogicProjector.visualization.VisualizationStateExtractorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GenerationTaskProcessor {

    private final GenerationTaskRepository generationTaskRepository;
    private final AlgorithmRecognitionService algorithmRecognitionService;
    private final VisualizationStateExtractorFactory visualizationStateExtractorFactory;
    private final NarrationService narrationService;
    private final BillingService billingService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    public GenerationTaskProcessor(GenerationTaskRepository generationTaskRepository,
            AlgorithmRecognitionService algorithmRecognitionService,
            VisualizationStateExtractorFactory visualizationStateExtractorFactory,
            NarrationService narrationService,
            BillingService billingService,
            SystemLogService systemLogService,
            ObjectMapper objectMapper) {
        this.generationTaskRepository = generationTaskRepository;
        this.algorithmRecognitionService = algorithmRecognitionService;
        this.visualizationStateExtractorFactory = visualizationStateExtractorFactory;
        this.narrationService = narrationService;
        this.billingService = billingService;
        this.systemLogService = systemLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long generationTaskId) {
        GenerationTask task = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + generationTaskId));

        if (task.getStatus() == GenerationTaskStatus.COMPLETED || task.getStatus() == GenerationTaskStatus.FAILED) {
            return;
        }

        task.markAnalyzing();
        systemLogService.info(task.getUser().getId(), task.getId(), "generation", "Generation processing started");

        RecognitionResult recognition = algorithmRecognitionService.recognize(task.getSourceCode());
        List<Integer> sampleInput = sampleInputFor(recognition.algorithm());
        VisualizationPayload payload = visualizationStateExtractorFactory.forAlgorithm(recognition.algorithm())
                .extract(recognition.algorithm().name(), sampleInput, task.getSourceCode());
        NarrationResult narration = narrationService.createNarration(recognition.algorithm(), payload, task.getSourceCode());
        JsonNode payloadJson = objectMapper.valueToTree(payload);

        task.complete(recognition.algorithm().name(), recognition.confidence(), payloadJson, narration.summary());
        billingService.chargeForCompletedGeneration(task.getUser(), task);
        systemLogService.info(task.getUser().getId(), task.getId(), "generation", "Generation processing completed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDeadLetter(Long generationTaskId, String errorMessage) {
        GenerationTask task = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + generationTaskId));

        if (task.getStatus() == GenerationTaskStatus.COMPLETED || task.getStatus() == GenerationTaskStatus.FAILED) {
            return;
        }

        task.fail(errorMessage);
        systemLogService.error(task.getUser().getId(), task.getId(), "generation", "Generation moved to dead-letter queue", errorMessage);
    }

    private List<Integer> sampleInputFor(DetectedAlgorithm algorithm) {
        return switch (algorithm) {
            case BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, QUICK_SORT, MERGE_SORT -> List.of(5, 1, 4, 2, 8);
            case BINARY_SEARCH -> List.of(1, 3, 5, 7, 9, 11);
            default -> List.of(3, 1, 2);
        };
    }
}
