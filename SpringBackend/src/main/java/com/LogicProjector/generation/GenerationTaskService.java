package com.LogicProjector.generation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.account.UserAccountRepository;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
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

    public GenerationTaskService(UserAccountRepository userAccountRepository,
            GenerationTaskRepository generationTaskRepository,
            TaskMessagePublisher taskMessagePublisher,
            ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.taskMessagePublisher = taskMessagePublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationTaskResponse createTask(CreateGenerationTaskRequest request) {
        UserAccount user = userAccountRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));

        GenerationTask task = generationTaskRepository.save(GenerationTask.pending(user, request.sourceCode(), request.language()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskMessagePublisher.publishGenerationTask(task.getId(), user.getId());
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
                0);
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
}
