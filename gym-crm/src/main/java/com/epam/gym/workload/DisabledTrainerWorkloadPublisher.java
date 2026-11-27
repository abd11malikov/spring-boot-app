package com.epam.gym.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used when {@code gym.workload.enabled=false}, to run Gym CRM on its own without the Trainer Workload service.
 */
public class DisabledTrainerWorkloadPublisher implements TrainerWorkloadPublisher {
    private static final Logger log = LoggerFactory.getLogger(DisabledTrainerWorkloadPublisher.class);

    @Override
    public void publish(TrainerWorkloadMessage message) {
        log.debug("Workload integration disabled: {} for trainer {} not sent", message.actionType(),
                message.trainerUsername());
    }
}
