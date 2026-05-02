package com.LogicProjector.generation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.analysis.AlgorithmRecognitionService;
import com.LogicProjector.analysis.DetectedAlgorithm;
import com.LogicProjector.analysis.RecognitionResult;
import com.LogicProjector.analysis.UnsupportedAlgorithmException;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.systemlog.SystemLogService;
import com.LogicProjector.visualization.NarrationResult;
import com.LogicProjector.visualization.NarrationService;
import com.LogicProjector.visualization.VisualizationPayload;
import com.LogicProjector.visualization.VisualizationStateExtractorFactory;
import com.LogicProjector.visualization.VisualizationStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GenerationTaskProcessor {

    private static final int TASK_ERROR_MESSAGE_MAX_LENGTH = 255;
    private static final String GENERATION_UNSUPPORTED = "GENERATION_UNSUPPORTED";
    private static final String GENERATION_DISPATCH_FAILED = "GENERATION_DISPATCH_FAILED";
    private static final String GENERATION_DEAD_LETTER = "GENERATION_DEAD_LETTER";

    private final GenerationTaskRepository generationTaskRepository;
    private final AlgorithmRecognitionService algorithmRecognitionService;
    private final VisualizationStateExtractorFactory visualizationStateExtractorFactory;
    private final NarrationService narrationService;
    private final BillingService billingService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;

    public GenerationTaskProcessor(GenerationTaskRepository generationTaskRepository,
            AlgorithmRecognitionService algorithmRecognitionService,
            VisualizationStateExtractorFactory visualizationStateExtractorFactory,
            NarrationService narrationService,
            BillingService billingService,
            SystemLogService systemLogService,
            ObjectMapper objectMapper,
            UserAccountRepository userAccountRepository) {
        this.generationTaskRepository = generationTaskRepository;
        this.algorithmRecognitionService = algorithmRecognitionService;
        this.visualizationStateExtractorFactory = visualizationStateExtractorFactory;
        this.narrationService = narrationService;
        this.billingService = billingService;
        this.systemLogService = systemLogService;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
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

        try {
            RecognitionResult recognition = algorithmRecognitionService.recognize(task.getSourceCode());
            List<Integer> sampleInput = sampleInputFor(recognition.algorithm());
            VisualizationPayload payload = visualizationStateExtractorFactory.forAlgorithm(recognition.algorithm())
                    .extract(recognition.algorithm().name(), sampleInput, task.getSourceCode());
            NarrationResult narration = narrationService.createNarration(recognition.algorithm(), payload, task.getSourceCode());
            VisualizationPayload narratedPayload = applyStepNarrations(payload, narration.stepNarrations());
            JsonNode payloadJson = objectMapper.valueToTree(narratedPayload);

            task.complete(recognition.algorithm().name(), recognition.confidence(), payloadJson, narration.summary());
            UserAccount user = userAccountRepository.findByIdForUpdate(task.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + task.getUser().getId()));
            billingService.chargeForCompletedGeneration(user, task);
            systemLogService.info(task.getUser().getId(), task.getId(), "generation", "Generation processing completed");
        } catch (UnsupportedAlgorithmException exception) {
            task.fail(summarizeErrorMessage(exception.getMessage(), GENERATION_UNSUPPORTED));
            systemLogService.error(task.getUser().getId(), task.getId(), "generation", "Generation rejected as unsupported", exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDispatch(Long generationTaskId, String errorMessage) {
        GenerationTask task = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + generationTaskId));

        if (task.getStatus() == GenerationTaskStatus.COMPLETED || task.getStatus() == GenerationTaskStatus.FAILED) {
            return;
        }

        task.fail(summarizeErrorMessage(errorMessage, GENERATION_DISPATCH_FAILED));
        systemLogService.error(task.getUser().getId(), task.getId(), "generation", "Generation dispatch failed", errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedFromDeadLetter(Long generationTaskId, String errorMessage) {
        GenerationTask task = generationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + generationTaskId));

        if (task.getStatus() == GenerationTaskStatus.COMPLETED || task.getStatus() == GenerationTaskStatus.FAILED) {
            return;
        }

        task.fail(summarizeErrorMessage(errorMessage, GENERATION_DEAD_LETTER));
        systemLogService.error(task.getUser().getId(), task.getId(), "generation", "Generation moved to dead-letter queue", errorMessage);
    }

    private String summarizeErrorMessage(String errorMessage, String fallback) {
        if (errorMessage == null) {
            return fallback;
        }

        String normalized = errorMessage.replace('\r', '\n');
        int newlineIndex = normalized.indexOf('\n');
        String summary = newlineIndex >= 0 ? normalized.substring(0, newlineIndex) : normalized;
        summary = summary.trim();

        if (summary.isEmpty()) {
            return fallback;
        }

        if (summary.length() <= TASK_ERROR_MESSAGE_MAX_LENGTH) {
            return summary;
        }

        return summary.substring(0, TASK_ERROR_MESSAGE_MAX_LENGTH - 3) + "...";
    }

    private List<Integer> sampleInputFor(DetectedAlgorithm algorithm) {
        return switch (algorithm) {
            case BUBBLE_SORT, SELECTION_SORT, INSERTION_SORT, QUICK_SORT, MERGE_SORT ->
                List.of(5, 1, 4, 2, 8);
            case BINARY_SEARCH ->
                List.of(1, 3, 5, 7, 9, 11);
            default ->
                List.of(3, 1, 2);
        };
    }

    private VisualizationPayload applyStepNarrations(VisualizationPayload payload, List<String> stepNarrations) {
        if (stepNarrations == null || stepNarrations.size() != payload.steps().size()) {
            return payload;
        }

        List<VisualizationStep> narratedSteps = new java.util.ArrayList<>();
        for (int index = 0; index < payload.steps().size(); index++) {
            VisualizationStep step = payload.steps().get(index);
            narratedSteps.add(new VisualizationStep(
                    step.title(),
                    stepNarrations.get(index),
                    step.arrayState(),
                    step.activeIndices(),
                    step.highlightedLines()));
        }
        return new VisualizationPayload(payload.algorithm(), List.copyOf(narratedSteps));
    }
}
