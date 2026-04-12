package com.LogicProjector.exporttask;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;

@WebMvcTest(ExportTaskController.class)
class ExportTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportTaskService exportTaskService;

    @Test
    void shouldCreateExportTask() throws Exception {
        given(exportTaskService.createExportTask(42L, 1L)).willReturn(new CreateExportTaskResponse(
                101L, 42L, "PENDING", 0, 18
        ));

        mockMvc.perform(post("/api/generation-tasks/42/exports")
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

        mockMvc.perform(get("/api/export-tasks/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.videoUrl").value("/api/export-tasks/101/download"));
    }
}
