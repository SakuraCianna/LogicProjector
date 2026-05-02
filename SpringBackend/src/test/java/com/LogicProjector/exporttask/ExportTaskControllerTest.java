package com.LogicProjector.exporttask;

import static org.mockito.ArgumentMatchers.anyLong;
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

import com.LogicProjector.auth.AuthenticatedUser;
import com.LogicProjector.auth.JwtService;
import com.LogicProjector.auth.SecurityConfig;
import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskListItemResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;

@WebMvcTest(ExportTaskController.class)
@Import(SecurityConfig.class)
class ExportTaskControllerTest {

    private static final String TEST_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportTaskService exportTaskService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        given(jwtService.parseToken(TEST_TOKEN)).willReturn(new AuthenticatedUser(1L, "teacher"));
    }

    @Test
    void shouldCreateExportTask() throws Exception {
        given(exportTaskService.createExportTask(42L, 1L)).willReturn(new CreateExportTaskResponse(
                101L, 42L, "PENDING", 0, 18
        ));

        mockMvc.perform(post("/api/generation-tasks/42/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnExportTaskStatus() throws Exception {
        given(exportTaskService.getExportTask(anyLong(), anyLong())).willReturn(new ExportTaskResponse(
                101L, 42L, "COMPLETED", 100, "/api/export-tasks/101/download", "/files/101.srt", "/files/101.mp3", null,
                18, 1231, "2026-04-11T16:00:00Z", "2026-04-11T16:00:30Z"
        ));

        mockMvc.perform(get("/api/export-tasks/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.videoUrl").value("/api/export-tasks/101/download"));
    }

    @Test
    void shouldReturn422ForInsufficientCredits() throws Exception {
        given(exportTaskService.createExportTask(42L, 1L)).willThrow(new ExportTaskException("INSUFFICIENT_CREDITS"));

        mockMvc.perform(post("/api/generation-tasks/42/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("INSUFFICIENT_CREDITS"));
    }

    @Test
    void shouldReturnRecentExportTasks() throws Exception {
        given(exportTaskService.getRecentExportTasks(1L)).willReturn(List.of(
                new ExportTaskListItemResponse(
                        101L,
                        42L,
                        "COMPLETED",
                        "QUICK_SORT",
                        "2026-04-22T10:00:00Z",
                        "2026-04-22T10:02:00Z")
        ));

        mockMvc.perform(get("/api/export-tasks/recent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].generationTaskId").value(42))
                .andExpect(jsonPath("$[0].detectedAlgorithm").value("QUICK_SORT"));
    }
}
