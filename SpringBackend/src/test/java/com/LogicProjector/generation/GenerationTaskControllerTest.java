package com.LogicProjector.generation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.LogicProjector.analysis.UnsupportedAlgorithmException;
import com.LogicProjector.auth.AuthenticatedUser;
import com.LogicProjector.auth.JwtService;
import com.LogicProjector.auth.SecurityConfig;
import com.LogicProjector.generation.api.GenerationTaskListItemResponse;
import com.LogicProjector.generation.api.GenerationTaskResponse;

@WebMvcTest(GenerationTaskController.class)
@Import(SecurityConfig.class)
class GenerationTaskControllerTest {

    private static final String TEST_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerationTaskService generationTaskService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(new AuthenticatedUser(1L, "teacher"));
    }

    @Test
    void shouldCreatePendingTask() throws Exception {
        GenerationTaskResponse response = new GenerationTaskResponse(
                42L, "PENDING", "java", null, null, 0.0, null, null, 0, "class Demo {}"
        );

        given(generationTaskService.createTask(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/generation-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceCode":"class Demo {}","language":"java"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn422ForUnsupportedAlgorithm() throws Exception {
        given(generationTaskService.createTask(any(), any()))
                .willThrow(new UnsupportedAlgorithmException(
                        "Unsupported algorithm or low confidence: 0.41"));

        mockMvc.perform(post("/api/generation-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceCode":"class Knapsack {}","language":"java"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported algorithm")));
    }

    @Test
    void shouldReturnRecentGenerationTasks() throws Exception {
        given(generationTaskService.getRecentTasks(1L)).willReturn(List.of(
                new GenerationTaskListItemResponse(
                        42L,
                        "COMPLETED",
                        "QUICK_SORT",
                        "Quick sort picks a pivot.",
                        "public class QuickSort {",
                        "2026-04-22T10:00:00Z",
                        "2026-04-22T10:01:00Z")
        ));

        mockMvc.perform(get("/api/generation-tasks/recent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].detectedAlgorithm").value("QUICK_SORT"))
                .andExpect(jsonPath("$[0].sourcePreview").value("public class QuickSort {"));
    }

    @Test
    void shouldReturn422ForInvalidTaskReference() throws Exception {
        given(generationTaskService.getTask(999L, 1L)).willThrow(new IllegalArgumentException("Task not owned by user: 999"));

        mockMvc.perform(get("/api/generation-tasks/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Task not owned by user: 999"));
    }

    @Test
    void shouldRejectOversizedSourceCode() throws Exception {
        String oversizedSource = "a".repeat(21000);

        mockMvc.perform(post("/api/generation-tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceCode":"%s","language":"java"}
                                """.formatted(oversizedSource)))
                .andExpect(status().isBadRequest());
    }
}
