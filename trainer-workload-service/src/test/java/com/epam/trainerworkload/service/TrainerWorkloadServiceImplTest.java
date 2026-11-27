package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceImplTest {

    private static final String USERNAME = "Nodira.Karimova";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    @Mock
    private TrainerWorkloadRepository repository;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    @Test
    void add_unknownTrainer_createsDocumentWithYearMonthAndDuration() {
        // given
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // when
        service.apply(update(ActionType.ADD, DATE, 60, true));

        // then
        TrainerWorkload saved = savedWorkload();
        assertEquals(USERNAME, saved.getUsername());
        assertEquals("Nodira", saved.getFirstName());
        assertEquals("Karimova", saved.getLastName());
        assertTrue(saved.isActive());
        assertEquals(2026, saved.getYears().get(0).getYear());
        assertEquals(11, saved.getYears().get(0).getMonths().get(0).getMonth());
        assertEquals(60, saved.getMonthlyDuration(2026, 11));
    }

    @Test
    void add_existingMonth_addsDurationToItsSummary() {
        // given
        TrainerWorkload existing = existing();
        existing.addTraining(DATE.plusDays(3), 45);
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        // when
        service.apply(update(ActionType.ADD, DATE, 60, true));

        // then
        assertSame(existing, savedWorkload());
        assertEquals(105, existing.getMonthlyDuration(2026, 11));
    }

    @Test
    void add_newMonthOfExistingTrainer_addsMonthAndKeepsOthers() {
        // given
        TrainerWorkload existing = existing();
        existing.addTraining(LocalDate.of(2026, 10, 1), 30);
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        // when
        service.apply(update(ActionType.ADD, DATE, 60, true));

        // then
        assertEquals(30, savedWorkload().getMonthlyDuration(2026, 10));
        assertEquals(60, existing.getMonthlyDuration(2026, 11));
    }

    @Test
    void add_updatesTrainerStatusToLatestValue() {
        // given
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing()));

        // when
        service.apply(update(ActionType.ADD, DATE, 60, false));

        // then
        assertFalse(savedWorkload().isActive());
    }

    @Test
    void delete_cancelledTraining_subtractsAndSaves() {
        // given
        TrainerWorkload existing = existing();
        existing.addTraining(DATE, 60);
        existing.addTraining(DATE, 30);
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        // when
        service.apply(update(ActionType.DELETE, DATE, 60, true));

        // then
        assertEquals(30, savedWorkload().getMonthlyDuration(2026, 11));
    }

    @Test
    void delete_monthWithoutWorkload_keepsOtherMonths() {
        // given
        TrainerWorkload existing = existing();
        existing.addTraining(DATE, 60);
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        // when
        service.apply(update(ActionType.DELETE, LocalDate.of(2026, 1, 5), 60, true));

        // then
        assertEquals(60, savedWorkload().getMonthlyDuration(2026, 11));
    }

    @Test
    void delete_unknownTrainer_savesNothing() {
        // given
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // when
        service.apply(update(ActionType.DELETE, DATE, 60, true));

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void getMonthlyWorkload_monthWithoutTrainings_isZero() {
        // given
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(existing()));

        // when
        MonthlyWorkload result = service.getMonthlyWorkload(USERNAME, 2026, 12);

        // then
        assertEquals(new MonthlyWorkload(USERNAME, 2026, 12, 0), result);
    }

    @Test
    void getWorkload_unknownTrainer_throws() {
        // given
        when(repository.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> service.getWorkload("Nobody.Here"));

        // then
        assertEquals("No workload for trainer Nobody.Here", exception.getMessage());
    }

    @Test
    void findByName_returnsRepositoryResult() {
        // given
        List<TrainerWorkload> trainers = List.of(existing());
        when(repository.findByFirstNameAndLastName("Nodira", "Karimova")).thenReturn(trainers);

        // when / then
        assertEquals(trainers, service.findByName("Nodira", "Karimova"));
    }

    private TrainerWorkload savedWorkload() {
        ArgumentCaptor<TrainerWorkload> saved = ArgumentCaptor.forClass(TrainerWorkload.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    private static TrainerWorkload existing() {
        return new TrainerWorkload(USERNAME, "Nodira", "Karimova", true);
    }

    private static TrainerWorkloadUpdate update(ActionType action, LocalDate date, int minutes, boolean active) {
        return new TrainerWorkloadUpdate(USERNAME, "Nodira", "Karimova", active, date, minutes, action);
    }
}
