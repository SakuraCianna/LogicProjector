package com.LogicProjector.generation;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.LogicProjector.analysis.UnsupportedAlgorithmException;
import com.LogicProjector.auth.AuthenticatedUser;
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
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return generationTaskService.createTask(request, user.userId());
    }

    @GetMapping("/{taskId}")
    public GenerationTaskResponse get(@PathVariable Long taskId, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return generationTaskService.getTask(taskId, user.userId());
    }

    @GetMapping("/recent")
    public List<GenerationTaskListItemResponse> recent(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return generationTaskService.getRecentTasks(user.userId());
    }

    @ExceptionHandler(UnsupportedAlgorithmException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleUnsupported(UnsupportedAlgorithmException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
