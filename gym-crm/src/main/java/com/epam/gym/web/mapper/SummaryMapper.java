package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.User;
import com.epam.gym.web.dto.response.TraineeSummaryResponse;
import com.epam.gym.web.dto.response.TrainerSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class SummaryMapper {

    private final TrainingTypeMapper trainingTypeMapper;

    public SummaryMapper(TrainingTypeMapper trainingTypeMapper) {
        this.trainingTypeMapper = trainingTypeMapper;
    }

    public TrainerSummaryResponse toTrainerSummary(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerSummaryResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainingTypeMapper.toResponse(trainer.getSpecialization()));
    }

    public TraineeSummaryResponse toTraineeSummary(Trainee trainee) {
        User user = trainee.getUser();
        return new TraineeSummaryResponse(user.getUsername(), user.getFirstName(), user.getLastName());
    }

    public List<TrainerSummaryResponse> toTrainerSummaries(Collection<Trainer> trainers) {
        return trainers.stream()
                .map(this::toTrainerSummary)
                .sorted(Comparator.comparing(TrainerSummaryResponse::username))
                .toList();
    }

    public List<TraineeSummaryResponse> toTraineeSummaries(Collection<Trainee> trainees) {
        return trainees.stream()
                .map(this::toTraineeSummary)
                .sorted(Comparator.comparing(TraineeSummaryResponse::username))
                .toList();
    }
}
