package com.LogicProjector.exporttask;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.LogicProjector.queue.RabbitTaskQueues;
import com.LogicProjector.queue.TaskEnvelope;

@Component
public class ExportTaskDeadLetterListener {

    private final ExportTaskProcessor exportTaskProcessor;

    public ExportTaskDeadLetterListener(ExportTaskProcessor exportTaskProcessor) {
        this.exportTaskProcessor = exportTaskProcessor;
    }

    @RabbitListener(queues = RabbitTaskQueues.EXPORT_DLQ, containerFactory = "exportListenerContainerFactory")
    public void handle(TaskEnvelope envelope, Message message) {
        Object exceptionMessage = message.getMessageProperties().getHeaders().get("x-exception-message");
        String errorMessage = exceptionMessage == null ? "EXPORT_RETRY_EXHAUSTED" : exceptionMessage.toString();
        exportTaskProcessor.markFailedFromDeadLetter(envelope.taskId(), errorMessage);
    }
}
