package com.epam.gym.web.dto.response;

public record TrainerSummaryResponse(String username,
                                     String firstName,
                                     String lastName,
                                     TrainingTypeResponse specialization) {
}
