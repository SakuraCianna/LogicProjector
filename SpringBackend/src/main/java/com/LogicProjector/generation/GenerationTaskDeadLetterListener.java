package com.LogicProjector.generation;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.LogicProjector.queue.RabbitTaskQueues;
import com.LogicProjector.queue.TaskEnvelope;

@Component
public class GenerationTaskDeadLetterListener {

    private final GenerationTaskProcessor generationTaskProcessor;

    public GenerationTaskDeadLetterListener(GenerationTaskProcessor generationTaskProcessor) {
        this.generationTaskProcessor = generationTaskProcessor;
    }

    @RabbitListener(queues = RabbitTaskQueues.GENERATION_DLQ, containerFactory = "generationDeadLetterListenerContainerFactory")
    public void handle(TaskEnvelope envelope, Message message) {
        Object exceptionMessage = message.getMessageProperties().getHeaders().get("x-exception-message");
        String errorMessage = exceptionMessage == null ? "GENERATION_RETRY_EXHAUSTED" : exceptionMessage.toString();
        generationTaskProcessor.markFailedFromDeadLetter(envelope.taskId(), errorMessage);
    }
}
