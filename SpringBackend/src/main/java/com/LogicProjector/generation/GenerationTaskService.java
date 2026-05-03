package com.LogicProjector.generation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.billing.BillingService;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
import com.LogicProjector.generation.api.GenerationTaskListItemResponse;
import com.LogicProjector.generation.api.GenerationTaskResponse;
import com.LogicProjector.queue.TaskMessagePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GenerationTaskService {

    private final UserAccountRepository userAccountRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final TaskMessagePublisher taskMessagePublisher;
    private final ObjectMapper objectMapper;
    private final BillingService billingService;
    private final GenerationTaskProcessor generationTaskProcessor;

    public GenerationTaskService(UserAccountRepository userAccountRepository,
            GenerationTaskRepository generationTaskRepository,
            TaskMessagePublisher taskMessagePublisher,
            ObjectMapper objectMapper,
            BillingService billingService,
            GenerationTaskProcessor generationTaskProcessor) {
        this.userAccountRepository = userAccountRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.taskMessagePublisher = taskMessagePublisher;
        this.objectMapper = objectMapper;
        this.billingService = billingService;
        this.generationTaskProcessor = generationTaskProcessor;
    }

    @Transactional
    public GenerationTaskResponse createTask(CreateGenerationTaskRequest request, Long userId) {
        if (!List.of("java", "c", "cpp").contains(request.language().toLowerCase())) {
            throw new IllegalArgumentException("UNSUPPORTED_LANGUAGE");
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getCreditsBalance() < billingService.generationCharge()) {
            throw new IllegalArgumentException("INSUFFICIENT_CREDITS");
        }

        GenerationTask task = generationTaskRepository.save(GenerationTask.pending(user, request.sourceCode(), request.language()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    taskMessagePublisher.publishGenerationTask(task.getId(), user.getId());
                } catch (RuntimeException exception) {
                    generationTaskProcessor.markFailedFromDispatch(task.getId(), "GENERATION_DISPATCH_FAILED");
                }
            }
        });

        return new GenerationTaskResponse(
                task.getId(),
                task.getStatus().name(),
                task.getLanguage(),
                null,
                null,
                0.0,
                null,
                task.getErrorMessage(),
                0,
                task.getSourceCode());
    }

    @Transactional(readOnly = true)
    public GenerationTaskResponse getTask(Long taskId, Long userId) {
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!task.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Task not owned by user: " + taskId);
        }

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
                task.getStatus() == GenerationTaskStatus.COMPLETED ? billingService.generationCharge() : 0,
                task.getSourceCode());
    }

    @Transactional(readOnly = true)
    public List<GenerationTaskListItemResponse> getRecentTasks(Long userId) {
        return generationTaskRepository.findTop8ByUser_IdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(GenerationTaskListItemResponse::from)
                .toList();
    }
}
