package com.LogicProjector.generation;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.LogicProjector.queue.RabbitTaskQueues;
import com.LogicProjector.queue.TaskEnvelope;

@Component
public class GenerationTaskListener {

    private final GenerationTaskProcessor generationTaskProcessor;

    public GenerationTaskListener(GenerationTaskProcessor generationTaskProcessor) {
        this.generationTaskProcessor = generationTaskProcessor;
    }

    @RabbitListener(queues = RabbitTaskQueues.GENERATION_QUEUE, containerFactory = "generationListenerContainerFactory")
    public void handle(TaskEnvelope envelope) {
        generationTaskProcessor.process(envelope.taskId());
    }
}
