package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.repository.InMemoryTrainerWorkloadRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerWorkloadServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();
    private final TrainerWorkloadServiceImpl service = new TrainerWorkloadServiceImpl(repository);

    @Test
    void add_firstTraining_createsTrainerWorkload() {
        // when
        service.apply(update(ActionType.ADD, 60, true));

        // then
        TrainerWorkload workload = service.getWorkload("Nodira.Karimova");
        assertEquals("Nodira", workload.getFirstName());
        assertEquals("Karimova", workload.getLastName());
        assertTrue(workload.isActive());
        assertEquals(60, workload.getMonthlyDuration(2026, 11));
    }

    @Test
    void add_laterTraining_updatesTrainerStatusAndSums() {
        // given
        service.apply(update(ActionType.ADD, 60, true));

        // when
        service.apply(update(ActionType.ADD, 30, false));

        // then
        TrainerWorkload workload = service.getWorkload("Nodira.Karimova");
        assertFalse(workload.isActive());
        assertEquals(90, workload.getMonthlyDuration(2026, 11));
    }

    @Test
    void delete_cancelledTraining_subtractsDuration() {
        // given
        service.apply(update(ActionType.ADD, 60, true));
        service.apply(update(ActionType.ADD, 30, true));

        // when
        service.apply(update(ActionType.DELETE, 60, true));

        // then
        assertEquals(30, service.getMonthlyWorkload("Nodira.Karimova", 2026, 11).trainingSummaryDuration());
    }

    @Test
    void delete_unknownTrainer_storesNothing() {
        // when
        service.apply(update(ActionType.DELETE, 60, true));

        // then
        assertTrue(repository.findByUsername("Nodira.Karimova").isEmpty());
    }

    @Test
    void delete_monthWithoutWorkload_keepsOtherMonths() {
        // given
        service.apply(update(ActionType.ADD, 60, true));

        // when
        service.apply(new TrainerWorkloadUpdate("Nodira.Karimova", "Nodira", "Karimova", true,
                LocalDate.of(2026, 1, 5), 60, ActionType.DELETE));

        // then
        assertEquals(60, service.getMonthlyWorkload("Nodira.Karimova", 2026, 11).trainingSummaryDuration());
    }

    @Test
    void getMonthlyWorkload_monthWithoutTrainings_isZero() {
        // given
        service.apply(update(ActionType.ADD, 60, true));

        // when
        MonthlyWorkload result = service.getMonthlyWorkload("Nodira.Karimova", 2026, 12);

        // then
        assertEquals(new MonthlyWorkload("Nodira.Karimova", 2026, 12, 0), result);
    }

    @Test
    void getWorkload_unknownTrainer_throws() {
        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> service.getWorkload("Nobody.Here"));

        // then
        assertEquals("No workload for trainer Nobody.Here", exception.getMessage());
    }

    private TrainerWorkloadUpdate update(ActionType action, int minutes, boolean active) {
        return new TrainerWorkloadUpdate("Nodira.Karimova", "Nodira", "Karimova", active, DATE, minutes, action);
    }
}
