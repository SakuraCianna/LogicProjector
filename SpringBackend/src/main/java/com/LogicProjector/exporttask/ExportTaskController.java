package com.LogicProjector.exporttask;

import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.LogicProjector.exporttask.api.CreateExportTaskResponse;
import com.LogicProjector.exporttask.api.ExportFailureResponse;
import com.LogicProjector.exporttask.api.ExportTaskListItemResponse;
import com.LogicProjector.exporttask.api.ExportTaskResponse;
import com.LogicProjector.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api")
public class ExportTaskController {

    private final ExportTaskService exportTaskService;

    public ExportTaskController(ExportTaskService exportTaskService) {
        this.exportTaskService = exportTaskService;
    }

    @PostMapping("/generation-tasks/{taskId}/exports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateExportTaskResponse create(@PathVariable Long taskId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return exportTaskService.createExportTask(taskId, user.userId());
    }

    @GetMapping("/export-tasks/{exportTaskId}")
    public ExportTaskResponse get(@PathVariable Long exportTaskId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return exportTaskService.getExportTask(exportTaskId, user.userId());
    }

    @GetMapping("/export-tasks/recent")
    public List<ExportTaskListItemResponse> recent(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return exportTaskService.getRecentExportTasks(user.userId());
    }

    @GetMapping("/export-tasks/{exportTaskId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long exportTaskId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return exportTaskService.download(exportTaskId, user.userId());
    }

    @ExceptionHandler(ExportTaskException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ExportFailureResponse handleExportFailure(ExportTaskException exception) {
        return new ExportFailureResponse(exception.getMessage());
    }
}
