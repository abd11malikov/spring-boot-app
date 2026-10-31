package com.epam.gym.service;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.User;

public interface AuthenticationService {
    Trainee authenticateTrainee(Credentials credentials);

    Trainer authenticateTrainer(Credentials credentials);

    User authenticateUser(Credentials credentials);

    void changePassword(Credentials credentials, String newPassword);
}
