package com.epam.gym.web.mapper;

import com.epam.gym.dto.TrainingRequest;
import com.epam.gym.model.Training;
import com.epam.gym.web.dto.request.AddTrainingRequest;
import com.epam.gym.web.dto.response.TraineeTrainingResponse;
import com.epam.gym.web.dto.response.TrainerTrainingResponse;
import org.springframework.stereotype.Component;

@Component
public class TrainingMapper {

    public TrainingRequest toServiceRequest(AddTrainingRequest request) {
        return new TrainingRequest(request.traineeUsername(), request.trainerUsername(), request.trainingName(),
                request.trainingDate(), request.trainingDuration());
    }

    public TraineeTrainingResponse toTraineeTrainingResponse(Training training) {
        return new TraineeTrainingResponse(training.getTrainingName(), training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(), training.getTrainingDuration(),
                training.getTrainer().getUser().getUsername());
    }

    public TrainerTrainingResponse toTrainerTrainingResponse(Training training) {
        return new TrainerTrainingResponse(training.getTrainingName(), training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(), training.getTrainingDuration(),
                training.getTrainee().getUser().getUsername());
    }
}
