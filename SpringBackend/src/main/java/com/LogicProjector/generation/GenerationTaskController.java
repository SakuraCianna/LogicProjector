package com.LogicProjector.generation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.LogicProjector.auth.AuthenticatedUsers;
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
import com.LogicProjector.generation.api.GenerationTaskListItemResponse;
import com.LogicProjector.generation.api.GenerationTaskResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/generation-tasks")
public class GenerationTaskController {

    private final GenerationTaskService generationTaskService;

    public GenerationTaskController(GenerationTaskService generationTaskService) {
        this.generationTaskService = generationTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationTaskResponse create(@Valid @RequestBody CreateGenerationTaskRequest request, Authentication authentication) {
        return generationTaskService.createTask(request, AuthenticatedUsers.current(authentication).userId());
    }

    @GetMapping("/{taskId}")
    public GenerationTaskResponse get(@PathVariable Long taskId, Authentication authentication) {
        return generationTaskService.getTask(taskId, AuthenticatedUsers.current(authentication).userId());
    }

    @GetMapping("/recent")
    public List<GenerationTaskListItemResponse> recent(Authentication authentication) {
        return generationTaskService.getRecentTasks(AuthenticatedUsers.current(authentication).userId());
    }
}
