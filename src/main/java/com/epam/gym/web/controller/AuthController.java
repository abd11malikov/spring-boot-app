package com.epam.gym.web.controller;

import com.epam.gym.security.JwtService;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.web.config.OpenApiConfig;
import com.epam.gym.web.dto.request.ChangePasswordRequest;
import com.epam.gym.web.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.epam.gym.web.auth.AccessGuard.requireOwner;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, logout and password change")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthController(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    /**
     * Spring Security has already checked the username and password from the Basic Authorization
     * header (see {@code SecurityConfig#loginFilterChain}) when this method runs.
     */
    @GetMapping("/login")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    @Operation(summary = "Login",
            description = "Checks the username and password sent in the Basic Authorization header and returns "
                    + "a bearer token. After 3 unsuccessful logins in a row the user is blocked for 5 minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials are valid, token issued"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid credentials"),
            @ApiResponse(responseCode = "429", description = "User is blocked after too many unsuccessful logins")
    })
    public TokenResponse login(Authentication authentication) {
        return TokenResponse.bearer(jwtService.generateToken(authentication.getName()),
                jwtService.getExpiration().toSeconds());
    }

    @PutMapping("/password")
    @Operation(summary = "Change login password",
            description = "Only for the logged-in user's own account; the old password is checked again")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "A required field is missing"),
            @ApiResponse(responseCode = "401", description = "Missing token, or wrong old password"),
            @ApiResponse(responseCode = "403", description = "Not your account")
    })
    public void changePassword(@AuthenticationPrincipal String currentUser,
                               @Valid @RequestBody ChangePasswordRequest request) {
        requireOwner(currentUser, request.username());
        authenticationService.changePassword(request.username(), request.oldPassword(), request.newPassword());
    }
}
