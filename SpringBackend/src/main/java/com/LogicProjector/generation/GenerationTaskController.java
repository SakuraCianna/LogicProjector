package com.LogicProjector.generation;

import java.util.Map;

import org.springframework.http.HttpStatus;
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
import com.LogicProjector.generation.api.CreateGenerationTaskRequest;
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
    public GenerationTaskResponse create(@Valid @RequestBody CreateGenerationTaskRequest request) {
        return generationTaskService.createTask(request);
    }

    @GetMapping("/{taskId}")
    public GenerationTaskResponse get(@PathVariable Long taskId) {
        return generationTaskService.getTask(taskId);
    }

    @ExceptionHandler(UnsupportedAlgorithmException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleUnsupported(UnsupportedAlgorithmException exception) {
        return Map.of("message", exception.getMessage());
    }
}
