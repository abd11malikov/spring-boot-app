package com.epam.gym.dao;

import com.epam.gym.model.TrainingType;

import java.util.List;
import java.util.Optional;

public interface TrainingTypeDao {
    Optional<TrainingType> findById(Long id);

    Optional<TrainingType> findByName(String trainingTypeName);

    List<TrainingType> findAll();
}
