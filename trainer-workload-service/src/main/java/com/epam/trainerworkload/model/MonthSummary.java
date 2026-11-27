package com.epam.trainerworkload.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Total training minutes of one trainer in one month (embedded in {@link TrainerWorkload}).
 */
public class MonthSummary {

    @Min(1)
    @Max(12)
    private int month;

    @PositiveOrZero
    private long trainingSummaryDuration;

    protected MonthSummary() {
        // for MongoDB mapping
    }

    public MonthSummary(int month, long trainingSummaryDuration) {
        this.month = month;
        this.trainingSummaryDuration = trainingSummaryDuration;
    }

    public int getMonth() {
        return month;
    }

    public long getTrainingSummaryDuration() {
        return trainingSummaryDuration;
    }

    void add(long minutes) {
        trainingSummaryDuration += minutes;
    }
}
