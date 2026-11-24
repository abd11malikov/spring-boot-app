package com.epam.gym.workload;

import com.epam.gym.exception.WorkloadServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Calls the Trainer Workload service, found through Eureka by its service name, behind a circuit breaker.
 * <p>
 * Every failure (connection refused, timeout, error status, no registered instance) or an open circuit ends in
 * {@link #unavailable}, which reports {@link WorkloadServiceUnavailableException}: the caller's transaction is
 * rolled back, so a training is never stored without its workload update.
 */
public class RestTrainerWorkloadClient implements TrainerWorkloadClient {
    private static final Logger log = LoggerFactory.getLogger(RestTrainerWorkloadClient.class);

    static final String CIRCUIT_BREAKER = "trainerWorkload";
    static final String WORKLOADS_PATH = "/api/trainer-workloads";

    private final RestClient restClient;

    public RestTrainerWorkloadClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "unavailable")
    public void updateWorkload(TrainerWorkloadRequest request) {
        log.debug("Sending {} of {} minutes on {} for trainer {} to the workload service", request.actionType(),
                request.trainingDuration(), request.trainingDate(), request.trainerUsername());
        restClient.post()
                .uri(WORKLOADS_PATH)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("Workload service accepted {} for trainer {} on {}", request.actionType(),
                request.trainerUsername(), request.trainingDate());
    }

    @SuppressWarnings("unused") // circuit breaker fallback, called reflectively
    private void unavailable(TrainerWorkloadRequest request, Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            log.warn("Workload circuit breaker is open: {} for trainer {} not sent", request.actionType(),
                    request.trainerUsername());
        } else {
            log.error("Workload service call failed for trainer {}: {}", request.trainerUsername(), cause.toString());
        }
        throw new WorkloadServiceUnavailableException(cause);
    }
}
