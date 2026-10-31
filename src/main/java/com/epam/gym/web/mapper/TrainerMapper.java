package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainer;
import com.epam.gym.model.User;
import com.epam.gym.web.dto.request.TrainerRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTrainerRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TrainerProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class TrainerMapper {

    private final SummaryMapper summaryMapper;
    private final TrainingTypeMapper trainingTypeMapper;

    public TrainerMapper(SummaryMapper summaryMapper, TrainingTypeMapper trainingTypeMapper) {
        this.summaryMapper = summaryMapper;
        this.trainingTypeMapper = trainingTypeMapper;
    }

    public Trainer toEntity(TrainerRegistrationRequest request) {
        Trainer trainer = new Trainer();
        trainer.setUser(new User(request.firstName(), request.lastName()));
        return trainer;
    }

    public Trainer toEntity(UpdateTrainerRequest request) {
        User user = new User(request.firstName(), request.lastName());
        user.setActive(request.isActive());
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        return trainer;
    }

    public RegistrationResponse toRegistrationResponse(Trainer trainer) {
        return new RegistrationResponse(trainer.getUser().getUsername(), trainer.getUser().getPassword());
    }

    public TrainerProfileResponse toProfileResponse(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainingTypeMapper.toResponse(trainer.getSpecialization()), user.isActive(),
                summaryMapper.toTraineeSummaries(trainer.getTrainees()));
    }
}
