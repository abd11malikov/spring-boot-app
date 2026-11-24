package com.epam.gym.web.dto.response;

import java.util.List;

public record TrainerProfileResponse(String username,
                                     String firstName,
                                     String lastName,
                                     TrainingTypeResponse specialization,
                                     Boolean isActive,
                                     List<TraineeSummaryResponse> trainees) {
}
