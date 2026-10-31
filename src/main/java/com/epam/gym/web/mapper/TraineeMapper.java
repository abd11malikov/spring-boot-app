package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.User;
import com.epam.gym.web.dto.request.TraineeRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TraineeProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class TraineeMapper {

    private final SummaryMapper summaryMapper;

    public TraineeMapper(SummaryMapper summaryMapper) {
        this.summaryMapper = summaryMapper;
    }

    public Trainee toEntity(TraineeRegistrationRequest request) {
        Trainee trainee = new Trainee(new User(request.firstName(), request.lastName()));
        trainee.setDateOfBirth(request.dateOfBirth());
        trainee.setAddress(request.address());
        return trainee;
    }

    public Trainee toEntity(UpdateTraineeRequest request) {
        User user = new User(request.firstName(), request.lastName());
        user.setActive(request.isActive());
        Trainee trainee = new Trainee(user);
        trainee.setDateOfBirth(request.dateOfBirth());
        trainee.setAddress(request.address());
        return trainee;
    }

    public RegistrationResponse toRegistrationResponse(Trainee trainee) {
        return new RegistrationResponse(trainee.getUser().getUsername(), trainee.getUser().getPassword());
    }

    public TraineeProfileResponse toProfileResponse(Trainee trainee) {
        User user = trainee.getUser();
        return new TraineeProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainee.getDateOfBirth(), trainee.getAddress(), user.isActive(),
                summaryMapper.toTrainerSummaries(trainee.getTrainers()));
    }
}
