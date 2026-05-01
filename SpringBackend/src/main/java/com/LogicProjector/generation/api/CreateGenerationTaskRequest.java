package com.LogicProjector.generation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGenerationTaskRequest(
        @NotBlank @Size(max = 20000) String sourceCode,
        @NotBlank String language
) {
}
