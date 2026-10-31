package com.epam.gym.service;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;

import java.util.List;

public interface TraineeService {
    Trainee create(Trainee trainee);

    Trainee selectByUsername(Credentials credentials, String username);

    Trainee updateProfile(Credentials credentials, Trainee changes);

    void setActive(Credentials credentials, boolean active);

    void delete(Credentials credentials);

    List<Trainer> getUnassignedTrainers(Credentials credentials);

    List<Trainer> updateTrainers(Credentials credentials, List<String> trainerUsernames);
}
