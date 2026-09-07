package com.fastbills.api.auth;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.LoginRequest;
import com.fastbills.api.auth.dto.RefreshTokenRequest;
import com.fastbills.api.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, and token refresh")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register organisation and initial owner", description = "Creates a new tenant organisation and its first owner account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and issues access/refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new access token and rotated refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Returns identity and organisation details for the authenticated user")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(Map.of(
                "userId", user.userId(),
                "organisationId", user.organisationId(),
                "email", user.email(),
                "fullName", user.fullName(),
                "role", user.role()
        ));
    }
}
