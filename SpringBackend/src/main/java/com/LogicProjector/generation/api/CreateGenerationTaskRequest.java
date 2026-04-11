package com.LogicProjector.generation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGenerationTaskRequest(
        @NotNull Long userId,
        @NotBlank String sourceCode,
        @NotBlank String language
) {
}
