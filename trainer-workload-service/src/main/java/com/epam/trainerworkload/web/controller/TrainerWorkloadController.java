package com.epam.trainerworkload.web.controller;

import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.web.dto.MonthlyWorkloadResponse;
import com.epam.trainerworkload.web.dto.TrainerWorkloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainer-workloads")
@Tag(name = "Trainer workloads",
        description = "Monthly training minutes of each trainer. Updates arrive as messages from Gym CRM.")
public class TrainerWorkloadController {

    private final TrainerWorkloadService service;

    public TrainerWorkloadController(TrainerWorkloadService service) {
        this.service = service;
    }


    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer's workload by year and month")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workload returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token"),
            @ApiResponse(responseCode = "404", description = "No workload recorded for the trainer")
    })
    public TrainerWorkloadResponse getWorkload(@PathVariable String username) {
        return TrainerWorkloadResponse.from(service.getWorkload(username));
    }

    @GetMapping("/{username}/years/{year}/months/{month}")
    @Operation(summary = "Get a trainer's training minutes in one month", description = "0 for a month without trainings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly workload returned"),
            @ApiResponse(responseCode = "400", description = "Invalid year or month"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token"),
            @ApiResponse(responseCode = "404", description = "No workload recorded for the trainer")
    })
    public MonthlyWorkloadResponse getMonthlyWorkload(
            @PathVariable String username,
            @Parameter(example = "2026") @PathVariable @Min(1900) @Max(9999) int year,
            @Parameter(example = "11") @PathVariable @Min(1) @Max(12) int month) {
        return MonthlyWorkloadResponse.from(service.getMonthlyWorkload(username, year, month));
    }
}
