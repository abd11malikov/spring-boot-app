package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.repository.TrainerWorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadServiceImpl.class);

    private final TrainerWorkloadRepository repository;

    public TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Override
    public void apply(TrainerWorkloadUpdate update) {
        if (update.actionType() == ActionType.ADD) {
            add(update);
        } else {
            delete(update);
        }
    }

    @Override
    public TrainerWorkload getWorkload(String username) {
        TrainerWorkload workload = repository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("No workload for trainer " + username));
        log.debug("Found workload of trainer {} for {} years", username, workload.getYears().size());
        return workload;
    }

    @Override
    public MonthlyWorkload getMonthlyWorkload(String username, int year, int month) {
        long minutes = getWorkload(username).getMonthlyDuration(year, month);
        log.debug("Trainer {} has {} training minutes in {}-{}", username, minutes, year, month);
        return new MonthlyWorkload(username, year, month, minutes);
    }

    private void add(TrainerWorkloadUpdate update) {
        repository.update(update.trainerUsername(), current -> {
            TrainerWorkload workload = current != null ? current : new TrainerWorkload(update.trainerUsername(),
                    update.trainerFirstName(), update.trainerLastName(), update.active());
            workload.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
            workload.addTraining(update.trainingDate(), update.trainingDuration());
            return workload;
        });
        log.info("Added {} minutes on {} to trainer {}", update.trainingDuration(), update.trainingDate(),
                update.trainerUsername());
    }

    private void delete(TrainerWorkloadUpdate update) {
        AtomicBoolean removed = new AtomicBoolean();
        repository.update(update.trainerUsername(), current -> {
            if (current == null) {
                return null;
            }
            current.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
            removed.set(current.removeTraining(update.trainingDate(), update.trainingDuration()));
            return current;
        });
        if (removed.get()) {
            log.info("Removed {} minutes on {} from trainer {}", update.trainingDuration(), update.trainingDate(),
                    update.trainerUsername());
        } else {
            log.warn("Nothing to remove for trainer {} in {}: no workload recorded for that month",
                    update.trainerUsername(), update.trainingDate().withDayOfMonth(1));
        }
    }
}
