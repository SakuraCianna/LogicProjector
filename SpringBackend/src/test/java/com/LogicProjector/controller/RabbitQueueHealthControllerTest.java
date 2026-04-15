package com.LogicProjector.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RabbitQueueHealthController.class)
class RabbitQueueHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitQueueHealthService rabbitQueueHealthService;

    @Test
    void shouldReturnQueueDepthsForMainAndDeadLetterQueues() throws Exception {
        given(rabbitQueueHealthService.getQueueStates()).willReturn(List.of(
                new RabbitQueueState("pas.generation.queue", 2, 1),
                new RabbitQueueState("pas.generation.dlq", 0, 0),
                new RabbitQueueState("pas.export.queue", 1, 1),
                new RabbitQueueState("pas.export.dlq", 0, 0)
        ));

        mockMvc.perform(get("/health/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queues[0].name").value("pas.generation.queue"))
                .andExpect(jsonPath("$.queues[0].messages").value(2))
                .andExpect(jsonPath("$.queues[2].consumers").value(1));
    }
}
