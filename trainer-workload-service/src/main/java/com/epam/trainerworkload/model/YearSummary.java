package com.epam.trainerworkload.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The months of one year in which a trainer has trainings, kept in calendar order.
 */
public class YearSummary {

    private final int year;
    private final List<MonthSummary> months = new ArrayList<>();

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

    YearSummary copy() {
        YearSummary copy = new YearSummary(year);
        months.forEach(month -> copy.months.add(month.copy()));
        return copy;
    }
}
