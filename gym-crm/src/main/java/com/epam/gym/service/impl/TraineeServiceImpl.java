package com.epam.gym.service.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.User;
import com.epam.gym.monitoring.metrics.GymMetrics;
import com.epam.gym.service.TraineeService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import com.epam.gym.workload.ActionType;
import com.epam.gym.workload.TrainerWorkloadClient;
import com.epam.gym.workload.TrainerWorkloadRequest;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.epam.gym.util.ValidationUtils.requireNotBlank;
import static com.epam.gym.util.ValidationUtils.requireNotNull;

@Service
@Transactional
public class TraineeServiceImpl implements TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeServiceImpl.class);

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TrainerWorkloadClient trainerWorkloadClient;
    private final GymMetrics metrics;

    public TraineeServiceImpl(TraineeDao traineeDao,
                              TrainerDao trainerDao,
                              UsernameGenerator usernameGenerator,
                              PasswordGenerator passwordGenerator,
                              PasswordEncoder passwordEncoder,
                              TrainerWorkloadClient trainerWorkloadClient,
                              GymMetrics metrics) {
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.trainerWorkloadClient = trainerWorkloadClient;
        this.metrics = metrics;
    }

    @Override
    public Credentials create(Trainee trainee) {
        validate(trainee);
        User user = trainee.getUser();
        String password = passwordGenerator.generate();
        user.setUsername(usernameGenerator.generate(user.getFirstName(), user.getLastName()));
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);

        traineeDao.create(trainee);
        metrics.traineeRegistered();
        log.info("Created trainee with username {}", user.getUsername());
        return new Credentials(user.getUsername(), password);
    }

    @Override
    @Transactional(readOnly = true)
    public Trainee selectByUsername(String username) {
        Trainee trainee = findTrainee(username);
        Hibernate.initialize(trainee.getTrainers());
        log.info("Selected trainee {}", username);
        return trainee;
    }

    @Override
    public Trainee updateProfile(String username, Trainee changes) {
        validate(changes);
        Trainee trainee = findTrainee(username);
        trainee.getUser().setFirstName(changes.getUser().getFirstName());
        trainee.getUser().setLastName(changes.getUser().getLastName());
        trainee.getUser().setActive(changes.getUser().isActive());
        trainee.setDateOfBirth(changes.getDateOfBirth());
        trainee.setAddress(changes.getAddress());
        Hibernate.initialize(trainee.getTrainers());
        log.info("Updated profile of trainee {}", username);
        return trainee;
    }

    @Override
    public void setActive(String username, boolean active) {
        Trainee trainee = findTrainee(username);
        if (trainee.getUser().isActive() == active) {
            throw new IllegalStateException("Trainee " + username + " is already "
                    + (active ? "active" : "inactive"));
        }
        trainee.getUser().setActive(active);
        log.info("Trainee {} is now {}", username, active ? "active" : "inactive");
    }

    /**
     * Deleting a trainee is the only way trainings are deleted (they cascade with the trainee), so each of
     * them is taken off its trainer's workload. Any failure rolls the whole deletion back.
     */
    @Override
    public void delete(String username) {
        Trainee trainee = findTrainee(username);
        List<Training> trainings = List.copyOf(trainee.getTrainings());
        traineeDao.delete(trainee);
        trainings.forEach(training ->
                trainerWorkloadClient.updateWorkload(TrainerWorkloadRequest.of(training, ActionType.DELETE)));
        log.info("Deleted trainee {} and removed {} trainings from trainer workloads", username, trainings.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(String username) {
        findTrainee(username);
        List<Trainer> trainers = trainerDao.findActiveNotAssignedToTrainee(username);
        log.info("Found {} active trainers not assigned to trainee {}", trainers.size(), username);
        return trainers;
    }

    @Override
    public List<Trainer> updateTrainers(String username, List<String> trainerUsernames) {
        requireNotNull(trainerUsernames, "Trainer usernames");
        Trainee trainee = findTrainee(username);

        Set<Trainer> trainers = new HashSet<>();
        for (String trainerUsername : trainerUsernames) {
            Trainer trainer = trainerDao.findByUsername(trainerUsername)
                    .orElseThrow(() -> new NoSuchElementException("Trainer " + trainerUsername + " not found"));
            trainers.add(trainer);
        }
        trainee.getTrainers().clear();
        trainee.getTrainers().addAll(trainers);

        log.info("Trainee {} now has {} trainers", username, trainers.size());
        return new ArrayList<>(trainee.getTrainers());
    }

    private Trainee findTrainee(String username) {
        return traineeDao.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Trainee " + username + " not found"));
    }

    private void validate(Trainee trainee) {
        requireNotNull(trainee, "Trainee");
        requireNotNull(trainee.getUser(), "Trainee user data");
        requireNotBlank(trainee.getUser().getFirstName(), "First name");
        requireNotBlank(trainee.getUser().getLastName(), "Last name");
    }
}
