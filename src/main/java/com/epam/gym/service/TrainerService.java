package com.epam.gym.service;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainer;

public interface TrainerService {
    Trainer create(Trainer trainer, Long specializationId);

    Trainer selectByUsername(Credentials credentials, String username);

    Trainer updateProfile(Credentials credentials, Trainer changes);

    void setActive(Credentials credentials, boolean active);
}
