package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainerRegistrationRequest(
        @Schema(example = "Nodira") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Karimova") @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "Id of a training type", example = "2") @NotNull Long specializationId) {
}
