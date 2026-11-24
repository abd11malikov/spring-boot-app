package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateTraineeTrainersRequest(
        @Schema(example = "[\"Nodira.Karimova\"]") @NotNull List<@NotBlank String> trainerUsernames) {
}
