package com.LogicProjector.queue;

public final class RabbitTaskQueues {

    public static final String GENERATION_QUEUE = "pas.generation.queue";
    public static final String EXPORT_QUEUE = "pas.export.queue";
    public static final String GENERATION_DLQ = "pas.generation.dlq";
    public static final String EXPORT_DLQ = "pas.export.dlq";

    private RabbitTaskQueues() {
    }
}
