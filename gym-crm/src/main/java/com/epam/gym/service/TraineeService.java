package com.epam.gym.service;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;

import java.util.List;

/**
 * Trainee operations. Except {@link #create}, every method acts on behalf of the already
 * authenticated user {@code username}.
 */
public interface TraineeService {

    /**
     * Registers the trainee with a generated username and password.
     *
     * @return the generated username and the plain password (only its hash is stored)
     */
    Credentials create(Trainee trainee);

    Trainee selectByUsername(String username);

    Trainee updateProfile(String username, Trainee changes);

    void setActive(String username, boolean active);

    void delete(String username);

    List<Trainer> getUnassignedTrainers(String username);

    List<Trainer> updateTrainers(String username, List<String> trainerUsernames);
}
