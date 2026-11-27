package com.epam.trainerworkload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Trainer's training summary: one MongoDB document per trainer, with the summary duration of their trainings per
 * year and month embedded.
 * <p>
 * Indexes: {@code username} is unique (every lookup is by username), and {@code firstName + lastName} supports
 * searching trainers by name. {@link Version} makes concurrent updates of one trainer fail instead of overwriting
 * each other.
 */
@Document(collection = "trainer_workloads")
@CompoundIndex(name = "trainer_name_idx", def = "{'firstName': 1, 'lastName': 1}")
public class TrainerWorkload {

    @Id
    private String id;

    @Version
    private Long version;

    @NotBlank
    @Indexed(name = "trainer_username_idx", unique = true)
    private String username;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    /** Trainer status: {@code true} when active. */
    @NotNull
    private Boolean status;

    @NotNull
    @Valid
    private List<YearSummary> years = new ArrayList<>();

    protected TrainerWorkload() {
        // for MongoDB mapping
    }

    public TrainerWorkload(String username, String firstName, String lastName, boolean active) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = active;
    }

    /**
     * Updates the trainer's name and status to the latest values sent with a training.
     */
    public void updateTrainer(String firstName, String lastName, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = active;
    }

    /**
     * Adds the training's duration to the summary of its year and month, creating them if needed.
     */
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

    public String getId() {
        return id;
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
        return Boolean.TRUE.equals(status);
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
