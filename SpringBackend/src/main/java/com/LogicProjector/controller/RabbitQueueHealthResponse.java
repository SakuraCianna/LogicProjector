package com.LogicProjector.controller;

import java.util.List;

public record RabbitQueueHealthResponse(List<RabbitQueueState> queues) {
}
