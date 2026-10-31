package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.ActivationRequest;
import com.epam.gym.web.dto.request.TraineeRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.request.UpdateTraineeTrainersRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TraineeProfileResponse;
import com.epam.gym.web.dto.response.TraineeTrainingResponse;
import com.epam.gym.web.dto.response.TrainerSummaryResponse;
import com.epam.gym.web.mapper.SummaryMapper;
import com.epam.gym.web.mapper.TraineeMapper;
import com.epam.gym.web.mapper.TrainingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.epam.gym.web.auth.AccessGuard.requireOwner;

@RestController
@RequestMapping("/api/trainees")
@Tag(name = "Trainees", description = "Trainee registration, profile and trainings")
public class TraineeController {

    private final TraineeService traineeService;
    private final TrainingService trainingService;
    private final TraineeMapper traineeMapper;
    private final SummaryMapper summaryMapper;
    private final TrainingMapper trainingMapper;

    public TraineeController(TraineeService traineeService,
                             TrainingService trainingService,
                             TraineeMapper traineeMapper,
                             SummaryMapper summaryMapper,
                             TrainingMapper trainingMapper) {
        this.traineeService = traineeService;
        this.trainingService = trainingService;
        this.traineeMapper = traineeMapper;
        this.summaryMapper = summaryMapper;
        this.trainingMapper = trainingMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a trainee", description = "Generates and returns the username and password")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee registered"),
            @ApiResponse(responseCode = "400", description = "A required field is missing or invalid")
    })
    public RegistrationResponse register(@Valid @RequestBody TraineeRegistrationRequest request) {
        return traineeMapper.toRegistrationResponse(traineeService.create(traineeMapper.toEntity(request)));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainee profile", description = "Available to any logged-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public TraineeProfileResponse getProfile(Credentials credentials, @PathVariable String username) {
        return traineeMapper.toProfileResponse(traineeService.selectByUsername(credentials, username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainee profile", description = "The username cannot be changed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "A required field is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public TraineeProfileResponse updateProfile(Credentials credentials,
                                                @PathVariable String username,
                                                @Valid @RequestBody UpdateTraineeRequest request) {
        requireOwner(credentials, username);
        return traineeMapper.toProfileResponse(
                traineeService.updateProfile(credentials, traineeMapper.toEntity(request)));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete trainee profile", description = "Also deletes the trainee's trainings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public void delete(Credentials credentials, @PathVariable String username) {
        requireOwner(credentials, username);
        traineeService.delete(credentials);
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or deactivate trainee",
            description = "Not idempotent: setting the status the trainee already has returns 409")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "isActive is missing"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile"),
            @ApiResponse(responseCode = "409", description = "Trainee already has this status")
    })
    public void setStatus(Credentials credentials,
                          @PathVariable String username,
                          @Valid @RequestBody ActivationRequest request) {
        requireOwner(credentials, username);
        traineeService.setActive(credentials, request.isActive());
    }

    @GetMapping("/{username}/unassigned-trainers")
    @Operation(summary = "Get active trainers not assigned to the trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public List<TrainerSummaryResponse> getUnassignedTrainers(Credentials credentials,
                                                              @PathVariable String username) {
        requireOwner(credentials, username);
        return summaryMapper.toTrainerSummaries(traineeService.getUnassignedTrainers(credentials));
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Replace the trainee's trainers list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers list updated"),
            @ApiResponse(responseCode = "400", description = "Trainers list is missing or contains a blank username"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile"),
            @ApiResponse(responseCode = "404", description = "A trainer was not found")
    })
    public List<TrainerSummaryResponse> updateTrainers(Credentials credentials,
                                                       @PathVariable String username,
                                                       @Valid @RequestBody UpdateTraineeTrainersRequest request) {
        requireOwner(credentials, username);
        return summaryMapper.toTrainerSummaries(
                traineeService.updateTrainers(credentials, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainee trainings", description = "All filters are optional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public List<TraineeTrainingResponse> getTrainings(
            Credentials credentials,
            @PathVariable String username,
            @Parameter(example = "2026-01-01") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(example = "2026-12-31") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Trainer username") @RequestParam(required = false) String trainerName,
            @Parameter(example = "Yoga") @RequestParam(required = false) String trainingType) {
        requireOwner(credentials, username);
        return trainingService.getTraineeTrainings(credentials, periodFrom, periodTo, trainerName, trainingType)
                .stream()
                .map(trainingMapper::toTraineeTrainingResponse)
                .toList();
    }
}
