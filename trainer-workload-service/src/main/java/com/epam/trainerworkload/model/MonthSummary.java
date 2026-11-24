package com.epam.trainerworkload.model;

/**
 * Total training minutes of one trainer in one month.
 */
public class MonthSummary {

    private final int month;
    private long trainingSummaryDuration;

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

    MonthSummary copy() {
        return new MonthSummary(month, trainingSummaryDuration);
    }
}
