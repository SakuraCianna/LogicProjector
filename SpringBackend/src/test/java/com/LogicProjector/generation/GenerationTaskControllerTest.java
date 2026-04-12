package com.LogicProjector.generation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.LogicProjector.analysis.UnsupportedAlgorithmException;
import com.LogicProjector.generation.api.GenerationTaskResponse;

@WebMvcTest(GenerationTaskController.class)
class GenerationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerationTaskService generationTaskService;

    @Test
    void shouldCreatePendingTask() throws Exception {
        GenerationTaskResponse response = new GenerationTaskResponse(
                42L, "PENDING", "java", null, null, 0.0, null, null, 0
        );

        given(generationTaskService.createTask(any())).willReturn(response);

        mockMvc.perform(post("/api/generation-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"sourceCode":"class Demo {}","language":"java"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn422ForUnsupportedAlgorithm() throws Exception {
        given(generationTaskService.createTask(any()))
                .willThrow(new UnsupportedAlgorithmException(
                        "Unsupported algorithm or low confidence: 0.41"));

        mockMvc.perform(post("/api/generation-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"sourceCode":"class Knapsack {}","language":"java"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported algorithm")));
    }
}
