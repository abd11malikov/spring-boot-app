package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.repository.TrainerWorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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

    @Override
    public List<TrainerWorkload> findByName(String firstName, String lastName) {
        List<TrainerWorkload> workloads = repository.findByFirstNameAndLastName(firstName, lastName);
        log.debug("Found {} trainers named {} {}", workloads.size(), firstName, lastName);
        return workloads;
    }

    /**
     * Finds the trainer's document by username, or starts a new one; adds the training duration to the summary of
     * the training's year and month (creating them if needed); saves the document.
     */
    private void add(TrainerWorkloadUpdate update) {
        TrainerWorkload workload = repository.findByUsername(update.trainerUsername())
                .orElseGet(() -> {
                    log.info("No workload document for trainer {} yet, creating one", update.trainerUsername());
                    return new TrainerWorkload(update.trainerUsername(), update.trainerFirstName(),
                            update.trainerLastName(), update.active());
                });
        workload.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
        workload.addTraining(update.trainingDate(), update.trainingDuration());
        repository.save(workload);
        log.info("Added {} minutes on {} to trainer {}: {} minutes that month", update.trainingDuration(),
                update.trainingDate(), update.trainerUsername(),
                workload.getMonthlyDuration(update.trainingDate().getYear(), update.trainingDate().getMonthValue()));
    }

    private void delete(TrainerWorkloadUpdate update) {
        Optional<TrainerWorkload> found = repository.findByUsername(update.trainerUsername());
        if (found.isEmpty()) {
            log.warn("Nothing to remove for trainer {}: no workload document", update.trainerUsername());
            return;
        }
        TrainerWorkload workload = found.get();
        workload.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
        boolean removed = workload.removeTraining(update.trainingDate(), update.trainingDuration());
        repository.save(workload);
        if (removed) {
            log.info("Removed {} minutes on {} from trainer {}", update.trainingDuration(), update.trainingDate(),
                    update.trainerUsername());
        } else {
            log.warn("Nothing to remove for trainer {} in {}: no workload recorded for that month",
                    update.trainerUsername(), update.trainingDate().withDayOfMonth(1));
        }
    }
}
