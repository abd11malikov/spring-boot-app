package com.epam.trainerworkload.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerWorkloadTest {

    private final TrainerWorkload workload = new TrainerWorkload("Nodira.Karimova", "Nodira", "Karimova", true);

    @Test
    void addTraining_sumsDurationsOfTheSameMonth() {
        // when
        workload.addTraining(LocalDate.of(2026, 11, 3), 60);
        workload.addTraining(LocalDate.of(2026, 11, 20), 45);

        // then
        assertEquals(105, workload.getMonthlyDuration(2026, 11));
    }

    @Test
    void addTraining_keepsYearsAndMonthsInCalendarOrder() {
        // when
        workload.addTraining(LocalDate.of(2027, 1, 5), 30);
        workload.addTraining(LocalDate.of(2026, 12, 5), 30);
        workload.addTraining(LocalDate.of(2026, 3, 5), 30);

        // then
        assertEquals(List.of(2026, 2027), workload.getYears().stream().map(YearSummary::getYear).toList());
        assertEquals(List.of(3, 12), workload.findYear(2026).orElseThrow().getMonths().stream()
                .map(MonthSummary::getMonth).toList());
    }

    @Test
    void removeTraining_subtractsFromTheMonth() {
        // given
        workload.addTraining(LocalDate.of(2026, 11, 3), 60);
        workload.addTraining(LocalDate.of(2026, 11, 4), 45);

        // when
        boolean removed = workload.removeTraining(LocalDate.of(2026, 11, 3), 60);

        // then
        assertTrue(removed);
        assertEquals(45, workload.getMonthlyDuration(2026, 11));
    }

    @Test
    void removeTraining_lastMinutes_removesMonthAndEmptyYear() {
        // given
        workload.addTraining(LocalDate.of(2026, 11, 3), 60);
        workload.addTraining(LocalDate.of(2027, 2, 3), 30);

        // when
        workload.removeTraining(LocalDate.of(2026, 11, 3), 60);

        // then
        assertTrue(workload.findYear(2026).isEmpty());
        assertEquals(30, workload.getMonthlyDuration(2027, 2));
    }

    @Test
    void removeTraining_moreThanRecorded_neverGoesNegative() {
        // given
        workload.addTraining(LocalDate.of(2026, 11, 3), 30);

        // when
        workload.removeTraining(LocalDate.of(2026, 11, 3), 90);

        // then
        assertEquals(0, workload.getMonthlyDuration(2026, 11));
        assertTrue(workload.getYears().isEmpty());
    }

    @Test
    void removeTraining_monthWithoutWorkload_changesNothing() {
        // given
        workload.addTraining(LocalDate.of(2026, 11, 3), 30);

        // when
        boolean removed = workload.removeTraining(LocalDate.of(2026, 10, 3), 30);

        // then
        assertFalse(removed);
        assertEquals(30, workload.getMonthlyDuration(2026, 11));
    }

    @Test
    void getMonthlyDuration_unknownMonth_isZero() {
        // when / then
        assertEquals(0, workload.getMonthlyDuration(2030, 1));
    }

    @Test
    void updateTrainer_replacesNameAndStatus() {
        // when
        workload.updateTrainer("Nodira", "Aliyeva", false);

        // then
        assertEquals("Aliyeva", workload.getLastName());
        assertFalse(workload.isActive());
    }

    @Test
    void copy_isIndependentOfTheOriginal() {
        // given
        workload.addTraining(LocalDate.of(2026, 11, 3), 30);
        TrainerWorkload copy = workload.copy();

        // when
        copy.addTraining(LocalDate.of(2026, 11, 4), 30);
        copy.updateTrainer("Changed", "Name", false);

        // then
        assertEquals(30, workload.getMonthlyDuration(2026, 11));
        assertEquals("Nodira", workload.getFirstName());
        assertEquals(60, copy.getMonthlyDuration(2026, 11));
    }
}
