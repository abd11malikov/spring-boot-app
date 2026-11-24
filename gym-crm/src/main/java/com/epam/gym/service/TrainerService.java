package com.epam.gym.service;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainer;

/**
 * Trainer operations. Except {@link #create}, every method acts on behalf of the already
 * authenticated user {@code username}.
 */
public interface TrainerService {

    /**
     * Registers the trainer with a generated username and password.
     *
     * @return the generated username and the plain password (only its hash is stored)
     */
    Credentials create(Trainer trainer, Long specializationId);

    Trainer selectByUsername(String username);

    Trainer updateProfile(String username, Trainer changes);

    void setActive(String username, boolean active);
}
