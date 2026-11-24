package com.epam.trainerworkload.web.dto;

import com.epam.trainerworkload.service.MonthlyWorkload;

/**
 * @param trainingSummaryDuration minutes
 */
public record MonthlyWorkloadResponse(String trainerUsername, int year, int month, long trainingSummaryDuration) {

    public static MonthlyWorkloadResponse from(MonthlyWorkload workload) {
        return new MonthlyWorkloadResponse(workload.trainerUsername(), workload.year(), workload.month(),
                workload.trainingSummaryDuration());
    }
}
