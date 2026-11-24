package com.epam.trainerworkload.service;

/**
 * @param trainingSummaryDuration total minutes of the trainer's trainings in that month
 */
public record MonthlyWorkload(String trainerUsername, int year, int month, long trainingSummaryDuration) {
}
