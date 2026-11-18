package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.User;
import com.epam.gym.monitoring.metrics.GymMetrics;
import com.epam.gym.service.TrainerService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import static com.epam.gym.util.ValidationUtils.requireNotBlank;
import static com.epam.gym.util.ValidationUtils.requireNotNull;

@Service
@Transactional
public class TrainerServiceImpl implements TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final GymMetrics metrics;

    public TrainerServiceImpl(TrainerDao trainerDao,
                              TrainingTypeDao trainingTypeDao,
                              UsernameGenerator usernameGenerator,
                              PasswordGenerator passwordGenerator,
                              PasswordEncoder passwordEncoder,
                              GymMetrics metrics) {
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.metrics = metrics;
    }

    @Override
    public Credentials create(Trainer trainer, Long specializationId) {
        validateNames(trainer);
        requireNotNull(specializationId, "Specialization");
        TrainingType specialization = trainingTypeDao.findById(specializationId)
                .orElseThrow(() -> new NoSuchElementException("Training type with id " + specializationId + " not found"));
        trainer.setSpecialization(specialization);

        User user = trainer.getUser();
        String password = passwordGenerator.generate();
        user.setUsername(usernameGenerator.generate(user.getFirstName(), user.getLastName()));
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);

        trainerDao.create(trainer);
        metrics.trainerRegistered();
        log.info("Created trainer with username {}", user.getUsername());
        return new Credentials(user.getUsername(), password);
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer selectByUsername(String username) {
        Trainer trainer = findTrainer(username);
        Hibernate.initialize(trainer.getTrainees());
        log.info("Selected trainer {}", username);
        return trainer;
    }

    @Override
    public Trainer updateProfile(String username, Trainer changes) {
        validateNames(changes);
        Trainer trainer = findTrainer(username);
        trainer.getUser().setFirstName(changes.getUser().getFirstName());
        trainer.getUser().setLastName(changes.getUser().getLastName());
        trainer.getUser().setActive(changes.getUser().isActive());
        Hibernate.initialize(trainer.getTrainees());
        log.info("Updated profile of trainer {}", username);
        return trainer;
    }

    @Override
    public void setActive(String username, boolean active) {
        Trainer trainer = findTrainer(username);
        if (trainer.getUser().isActive() == active) {
            throw new IllegalStateException("Trainer " + username + " is already "
                    + (active ? "active" : "inactive"));
        }
        trainer.getUser().setActive(active);
        log.info("Trainer {} is now {}", username, active ? "active" : "inactive");
    }

    private Trainer findTrainer(String username) {
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Trainer " + username + " not found"));
    }

    private void validateNames(Trainer trainer) {
        requireNotNull(trainer, "Trainer");
        requireNotNull(trainer.getUser(), "Trainer user data");
        requireNotBlank(trainer.getUser().getFirstName(), "First name");
        requireNotBlank(trainer.getUser().getLastName(), "Last name");
    }
}
