package com.epam.gym.web.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TraineeProfileResponse(String username,
                                     String firstName,
                                     String lastName,
                                     LocalDate dateOfBirth,
                                     String address,
                                     Boolean isActive,
                                     List<TrainerSummaryResponse> trainers) {
}
