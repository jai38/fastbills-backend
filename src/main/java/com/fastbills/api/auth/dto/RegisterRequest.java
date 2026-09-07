package com.fastbills.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Organisation legal name is required")
        @Size(max = 255, message = "Legal name must not exceed 255 characters")
        String legalName,

        @Size(max = 255, message = "Trade name must not exceed 255 characters")
        String tradeName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
        String gstin,

        @Pattern(regexp = "^[0-9]{2}$", message = "State code must be exactly 2 digits")
        String stateCode,

        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
        String pan
) {}
