package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ActivationRequest(@Schema(example = "false") @NotNull Boolean isActive) {
}
