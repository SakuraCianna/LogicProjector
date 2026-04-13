package com.LogicProjector.queue;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TaskMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String generationRoutingKey;
    private final String exportRoutingKey;

    public TaskMessagePublisher(RabbitTemplate rabbitTemplate,
            @Value("${pas.queue.exchange}") String exchangeName,
            @Value("${pas.queue.generation-routing-key}") String generationRoutingKey,
            @Value("${pas.queue.export-routing-key}") String exportRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.generationRoutingKey = generationRoutingKey;
        this.exportRoutingKey = exportRoutingKey;
    }

    public void publishGenerationTask(Long taskId, Long userId) {
        rabbitTemplate.convertAndSend(exchangeName, generationRoutingKey,
                new TaskEnvelope("GENERATION", taskId, userId, Instant.now(), UUID.randomUUID().toString()));
    }

    public void publishExportTask(Long taskId, Long userId) {
        rabbitTemplate.convertAndSend(exchangeName, exportRoutingKey,
                new TaskEnvelope("EXPORT", taskId, userId, Instant.now(), UUID.randomUUID().toString()));
    }
}
