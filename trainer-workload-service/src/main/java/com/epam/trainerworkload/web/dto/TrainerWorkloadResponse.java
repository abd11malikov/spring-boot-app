package com.epam.trainerworkload.web.dto;

import com.epam.trainerworkload.model.MonthSummary;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.model.YearSummary;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TrainerWorkloadResponse(String trainerUsername,
                                      String trainerFirstName,
                                      String trainerLastName,
                                      @JsonProperty("isActive") boolean isActive,
                                      List<Year> years) {

    public static TrainerWorkloadResponse from(TrainerWorkload workload) {
        return new TrainerWorkloadResponse(workload.getUsername(), workload.getFirstName(), workload.getLastName(),
                workload.isActive(), workload.getYears().stream().map(Year::from).toList());
    }

    public record Year(int year, List<Month> months) {
        static Year from(YearSummary summary) {
            return new Year(summary.getYear(), summary.getMonths().stream().map(Month::from).toList());
        }
    }

    /**
     * @param trainingSummaryDuration minutes
     */
    public record Month(int month, long trainingSummaryDuration) {
        static Month from(MonthSummary summary) {
            return new Month(summary.getMonth(), summary.getTrainingSummaryDuration());
        }
    }
}
