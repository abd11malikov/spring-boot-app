package com.epam.gym.web.controller;

import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.AddTrainingRequest;
import com.epam.gym.web.mapper.TrainingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainings")
@Tag(name = "Trainings", description = "Adding trainings (no update or delete via REST)")
public class TrainingController {

    private final TrainingService trainingService;
    private final TrainingMapper trainingMapper;

    public TrainingController(TrainingService trainingService, TrainingMapper trainingMapper) {
        this.trainingService = trainingService;
        this.trainingMapper = trainingMapper;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "Only the trainee or the trainer of the training can add it. "
                    + "The training type is the trainer's specialization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added"),
            @ApiResponse(responseCode = "400", description = "A required field is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked token"),
            @ApiResponse(responseCode = "403", description = "Caller is neither the trainee nor the trainer"),
            @ApiResponse(responseCode = "404", description = "Trainee or trainer not found"),
            @ApiResponse(responseCode = "503", description = "Workload update could not be queued, nothing saved")
    })
    public void addTraining(@AuthenticationPrincipal String currentUser,
                            @Valid @RequestBody AddTrainingRequest request) {
        trainingService.addTraining(currentUser, trainingMapper.toServiceRequest(request));
    }
}
