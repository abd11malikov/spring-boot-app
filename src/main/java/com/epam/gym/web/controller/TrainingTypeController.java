package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.service.TrainingTypeService;
import com.epam.gym.web.dto.response.TrainingTypeResponse;
import com.epam.gym.web.mapper.TrainingTypeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training-types")
@Tag(name = "Training types", description = "Constant list of training types")
public class TrainingTypeController {

    private final TrainingTypeService trainingTypeService;
    private final TrainingTypeMapper trainingTypeMapper;

    public TrainingTypeController(TrainingTypeService trainingTypeService, TrainingTypeMapper trainingTypeMapper) {
        this.trainingTypeService = trainingTypeService;
        this.trainingTypeMapper = trainingTypeMapper;
    }

    @GetMapping
    @Operation(summary = "Get all training types")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training types returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials")
    })
    public List<TrainingTypeResponse> getTrainingTypes(Credentials credentials) {
        return trainingTypeService.findAll(credentials).stream()
                .map(trainingTypeMapper::toResponse)
                .toList();
    }
}
