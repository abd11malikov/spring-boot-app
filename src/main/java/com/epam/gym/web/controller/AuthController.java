package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.web.dto.request.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and password change")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/login")
    @Operation(summary = "Login", description = "Checks the username and password sent in the Basic Authorization header")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials are valid"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials")
    })
    public void login(Credentials credentials) {
        authenticationService.authenticateUser(credentials);
    }

    @PutMapping("/password")
    @SecurityRequirements
    @Operation(summary = "Change login password", description = "Authenticates with the old password from the body")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "A required field is missing"),
            @ApiResponse(responseCode = "401", description = "Wrong username or old password")
    })
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(
                new Credentials(request.username(), request.oldPassword()), request.newPassword());
    }
}
