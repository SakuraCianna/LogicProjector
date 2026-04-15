package com.LogicProjector.controller;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import com.LogicProjector.queue.RabbitTaskQueues;

@Service
public class RabbitQueueHealthService {

    private final AmqpAdmin amqpAdmin;

    public RabbitQueueHealthService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public List<RabbitQueueState> getQueueStates() {
        return List.of(
                queueState(RabbitTaskQueues.GENERATION_QUEUE),
                queueState(RabbitTaskQueues.GENERATION_DLQ),
                queueState(RabbitTaskQueues.EXPORT_QUEUE),
                queueState(RabbitTaskQueues.EXPORT_DLQ)
        );
    }

    private RabbitQueueState queueState(String queueName) {
        Properties properties = amqpAdmin.getQueueProperties(queueName);
        if (properties == null) {
            return new RabbitQueueState(queueName, -1, -1);
        }

        int messages = readInt(properties, RabbitAdmin.QUEUE_MESSAGE_COUNT.toString());
        int consumers = readInt(properties, RabbitAdmin.QUEUE_CONSUMER_COUNT.toString());
        return new RabbitQueueState(queueName, messages, consumers);
    }

    private int readInt(Map<Object, Object> properties, String key) {
        Object value = properties.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
