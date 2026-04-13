package com.LogicProjector.exporttask;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.LogicProjector.queue.RabbitTaskQueues;
import com.LogicProjector.queue.TaskEnvelope;

@Component
public class ExportTaskListener {

    private final ExportTaskProcessor exportTaskProcessor;

    public ExportTaskListener(ExportTaskProcessor exportTaskProcessor) {
        this.exportTaskProcessor = exportTaskProcessor;
    }

    @RabbitListener(queues = RabbitTaskQueues.EXPORT_QUEUE, containerFactory = "exportListenerContainerFactory")
    public void handle(TaskEnvelope envelope) {
        exportTaskProcessor.process(envelope.taskId());
    }
}
