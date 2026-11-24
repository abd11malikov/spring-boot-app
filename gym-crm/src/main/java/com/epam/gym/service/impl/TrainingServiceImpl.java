package com.epam.gym.service.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dto.TrainingRequest;
import com.epam.gym.exception.AccessDeniedException;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.monitoring.metrics.GymMetrics;
import com.epam.gym.service.TrainingService;
import com.epam.gym.workload.ActionType;
import com.epam.gym.workload.TrainerWorkloadPublisher;
import com.epam.gym.workload.TrainerWorkloadMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static com.epam.gym.util.ValidationUtils.requireNotBlank;
import static com.epam.gym.util.ValidationUtils.requireNotNull;
import static com.epam.gym.util.ValidationUtils.requirePositive;

@Service
@Transactional
public class TrainingServiceImpl implements TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingServiceImpl.class);

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final TrainerWorkloadPublisher trainerWorkloadPublisher;
    private final GymMetrics metrics;

    public TrainingServiceImpl(TrainingDao trainingDao,
                               TraineeDao traineeDao,
                               TrainerDao trainerDao,
                               TrainerWorkloadPublisher trainerWorkloadPublisher,
                               GymMetrics metrics) {
        this.trainingDao = trainingDao;
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.trainerWorkloadPublisher = trainerWorkloadPublisher;
        this.metrics = metrics;
    }

    @Override
    public Training addTraining(String username, TrainingRequest request) {
        validate(request);
        if (!username.equals(request.traineeUsername()) && !username.equals(request.trainerUsername())) {
            throw new AccessDeniedException("Only the trainee or the trainer can add this training");
        }

        Trainee trainee = traineeDao.findByUsername(request.traineeUsername())
                .orElseThrow(() -> new NoSuchElementException("Trainee " + request.traineeUsername() + " not found"));
        Trainer trainer = trainerDao.findByUsername(request.trainerUsername())
                .orElseThrow(() -> new NoSuchElementException("Trainer " + request.trainerUsername() + " not found"));

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingType(trainer.getSpecialization());
        training.setTrainingName(request.trainingName());
        training.setTrainingDate(request.trainingDate());
        training.setTrainingDuration(request.trainingDuration());
        trainingDao.create(training);

        trainee.getTrainings().add(training);
        trainee.getTrainers().add(trainer);
        // Sent only if this transaction commits; if it can't be queued, the exception rolls the training back
        trainerWorkloadPublisher.publish(TrainerWorkloadMessage.of(training, ActionType.ADD));
        metrics.trainingAdded();

        log.info("Added training '{}' for trainee {} with trainer {}",
                request.trainingName(), request.traineeUsername(), request.trainerUsername());
        return training;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                              String trainerUsername, String trainingTypeName) {
        if (traineeDao.findByUsername(username).isEmpty()) {
            throw new NoSuchElementException("Trainee " + username + " not found");
        }
        List<Training> trainings = metrics.timeTraineeTrainingSearch(() -> trainingDao.findTraineeTrainings(
                username, fromDate, toDate, trainerUsername, trainingTypeName));
        log.info("Found {} trainings for trainee {}", trainings.size(), username);
        return trainings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                              String traineeUsername) {
        if (trainerDao.findByUsername(username).isEmpty()) {
            throw new NoSuchElementException("Trainer " + username + " not found");
        }
        List<Training> trainings = metrics.timeTrainerTrainingSearch(() -> trainingDao.findTrainerTrainings(
                username, fromDate, toDate, traineeUsername));
        log.info("Found {} trainings for trainer {}", trainings.size(), username);
        return trainings;
    }

    private void validate(TrainingRequest request) {
        requireNotNull(request, "Training request");
        requireNotBlank(request.traineeUsername(), "Trainee username");
        requireNotBlank(request.trainerUsername(), "Trainer username");
        requireNotBlank(request.trainingName(), "Training name");
        requireNotNull(request.trainingDate(), "Training date");
        requirePositive(request.trainingDuration(), "Training duration");
    }
}
