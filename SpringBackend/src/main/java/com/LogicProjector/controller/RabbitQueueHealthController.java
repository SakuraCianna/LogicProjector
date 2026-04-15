package com.LogicProjector.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health/queues")
public class RabbitQueueHealthController {

    private final RabbitQueueHealthService rabbitQueueHealthService;

    public RabbitQueueHealthController(RabbitQueueHealthService rabbitQueueHealthService) {
        this.rabbitQueueHealthService = rabbitQueueHealthService;
    }

    @GetMapping
    public RabbitQueueHealthResponse getQueueHealth() {
        return new RabbitQueueHealthResponse(rabbitQueueHealthService.getQueueStates());
    }
}
