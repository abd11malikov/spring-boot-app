package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.security.JwtService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.ActivationRequest;
import com.epam.gym.web.dto.request.TrainerRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTrainerRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TrainerProfileResponse;
import com.epam.gym.web.dto.response.TrainerTrainingResponse;
import com.epam.gym.web.mapper.TrainerMapper;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/trainers")
@Tag(name = "Trainers", description = "Trainer registration, profile and trainings")
public class TrainerController {

    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final TrainerMapper trainerMapper;
    private final TrainingMapper trainingMapper;
    private final JwtService jwtService;

    public TrainerController(TrainerService trainerService,
                             TrainingService trainingService,
                             TrainerMapper trainerMapper,
                             TrainingMapper trainingMapper,
                             JwtService jwtService) {
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.trainerMapper = trainerMapper;
        this.trainingMapper = trainingMapper;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a trainer", description = "Generates the username and password and returns them with a bearer token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer registered"),
            @ApiResponse(responseCode = "400", description = "A required field is missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Specialization not found")
    })
    public RegistrationResponse register(@Valid @RequestBody TrainerRegistrationRequest request) {
        Credentials credentials = trainerService.create(trainerMapper.toEntity(request), request.specializationId());
        return new RegistrationResponse(credentials.username(), credentials.password(),
                jwtService.generateToken(credentials.username()));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainer profile", description = "Available to any logged-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked token"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        return trainerMapper.toProfileResponse(trainerService.selectByUsername(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainer profile",
            description = "The username and specialization cannot be changed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "A required field is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked token"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public TrainerProfileResponse updateProfile(@AuthenticationPrincipal String currentUser,
                                                @PathVariable String username,
                                                @Valid @RequestBody UpdateTrainerRequest request) {
        requireOwner(currentUser, username);
        return trainerMapper.toProfileResponse(
                trainerService.updateProfile(currentUser, trainerMapper.toEntity(request)));
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or deactivate trainer",
            description = "Not idempotent: setting the status the trainer already has returns 409")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "isActive is missing"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked token"),
            @ApiResponse(responseCode = "403", description = "Not your profile"),
            @ApiResponse(responseCode = "409", description = "Trainer already has this status")
    })
    public void setStatus(@AuthenticationPrincipal String currentUser,
                          @PathVariable String username,
                          @Valid @RequestBody ActivationRequest request) {
        requireOwner(currentUser, username);
        trainerService.setActive(currentUser, request.isActive());
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainer trainings", description = "All filters are optional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired or revoked token"),
            @ApiResponse(responseCode = "403", description = "Not your profile")
    })
    public List<TrainerTrainingResponse> getTrainings(
            @AuthenticationPrincipal String currentUser,
            @PathVariable String username,
            @Parameter(example = "2026-01-01") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(example = "2026-12-31") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Trainee username") @RequestParam(required = false) String traineeName) {
        requireOwner(currentUser, username);
        return trainingService.getTrainerTrainings(currentUser, periodFrom, periodTo, traineeName)
                .stream()
                .map(trainingMapper::toTrainerTrainingResponse)
                .toList();
    }
}
