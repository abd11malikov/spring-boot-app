package com.epam.trainerworkload.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A trainer and the summary duration of their trainings per year and month.
 * <p>
 * Not thread-safe: the repository applies changes atomically per trainer and hands out copies.
 */
public class TrainerWorkload {

    private final String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private final List<YearSummary> years = new ArrayList<>();

    public TrainerWorkload(String username, String firstName, String lastName, boolean active) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    /**
     * Updates the trainer's name and status to the latest values sent with a training.
     */
    public void updateTrainer(String firstName, String lastName, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public void addTraining(LocalDate date, int durationMinutes) {
        getOrCreateYear(date.getYear()).getOrCreateMonth(date.getMonthValue()).add(durationMinutes);
    }

    /**
     * Takes a cancelled training away from its month. A month that drops to zero (or below, if more was
     * removed than ever added) is removed, and so is a year without months.
     *
     * @return {@code false} if there was no workload in that month to remove from
     */
    public boolean removeTraining(LocalDate date, int durationMinutes) {
        Optional<YearSummary> year = findYear(date.getYear());
        Optional<MonthSummary> month = year.flatMap(found -> found.findMonth(date.getMonthValue()));
        if (month.isEmpty()) {
            return false;
        }
        month.get().add(-durationMinutes);
        if (month.get().getTrainingSummaryDuration() <= 0) {
            year.get().removeMonth(month.get());
            if (year.get().isEmpty()) {
                years.remove(year.get());
            }
        }
        return true;
    }

    public long getMonthlyDuration(int year, int month) {
        return findYear(year)
                .flatMap(found -> found.findMonth(month))
                .map(MonthSummary::getTrainingSummaryDuration)
                .orElse(0L);
    }

    public Optional<YearSummary> findYear(int year) {
        return years.stream().filter(summary -> summary.getYear() == year).findFirst();
    }

    public TrainerWorkload copy() {
        TrainerWorkload copy = new TrainerWorkload(username, firstName, lastName, active);
        years.forEach(year -> copy.years.add(year.copy()));
        return copy;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }

    public List<YearSummary> getYears() {
        return List.copyOf(years);
    }

    private YearSummary getOrCreateYear(int year) {
        return findYear(year).orElseGet(() -> {
            YearSummary created = new YearSummary(year);
            years.add(created);
            years.sort(Comparator.comparingInt(YearSummary::getYear));
            return created;
        });
    }
}
