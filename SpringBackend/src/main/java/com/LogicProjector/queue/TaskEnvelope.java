package com.LogicProjector.queue;

import java.time.Instant;

public record TaskEnvelope(
        String taskType,
        Long taskId,
        Long userId,
        Instant createdAt,
        String traceId
) {
}
