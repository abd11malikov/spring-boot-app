package com.epam.trainerworkload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The months of one year in which a trainer has trainings, kept in calendar order (embedded in
 * {@link TrainerWorkload}).
 */
public class YearSummary {

    @Min(1900)
    @Max(9999)
    private int year;

    @NotNull
    @Valid
    private List<MonthSummary> months = new ArrayList<>();

    protected YearSummary() {
        // for MongoDB mapping
    }

    public YearSummary(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    public List<MonthSummary> getMonths() {
        return List.copyOf(months);
    }

    public Optional<MonthSummary> findMonth(int month) {
        return months.stream().filter(summary -> summary.getMonth() == month).findFirst();
    }

    MonthSummary getOrCreateMonth(int month) {
        return findMonth(month).orElseGet(() -> {
            MonthSummary created = new MonthSummary(month, 0);
            months.add(created);
            months.sort(Comparator.comparingInt(MonthSummary::getMonth));
            return created;
        });
    }

    void removeMonth(MonthSummary month) {
        months.remove(month);
    }

    boolean isEmpty() {
        return months.isEmpty();
    }
}
