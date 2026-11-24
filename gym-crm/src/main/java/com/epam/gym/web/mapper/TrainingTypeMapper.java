package com.epam.gym.web.mapper;

import com.epam.gym.model.TrainingType;
import com.epam.gym.web.dto.response.TrainingTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class TrainingTypeMapper {

    public TrainingTypeResponse toResponse(TrainingType trainingType) {
        return new TrainingTypeResponse(trainingType.getId(), trainingType.getTrainingTypeName());
    }
}
