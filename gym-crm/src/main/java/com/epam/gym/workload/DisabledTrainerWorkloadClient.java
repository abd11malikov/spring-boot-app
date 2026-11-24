package com.epam.gym.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used when {@code gym.workload.enabled=false}, to run Gym CRM on its own without the Trainer Workload service.
 */
public class DisabledTrainerWorkloadClient implements TrainerWorkloadClient {
    private static final Logger log = LoggerFactory.getLogger(DisabledTrainerWorkloadClient.class);

    @Override
    public void updateWorkload(TrainerWorkloadRequest request) {
        log.debug("Workload integration disabled: {} for trainer {} not sent", request.actionType(),
                request.trainerUsername());
    }
}
