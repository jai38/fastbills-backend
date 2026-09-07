package com.fastbills.api.organisation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganisationProfileRequest(
        @NotBlank(message = "Legal name is required")
        @Size(max = 255, message = "Legal name cannot exceed 255 characters")
        String legalName,

        @Size(max = 255, message = "Trade name cannot exceed 255 characters")
        String tradeName,

        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
        String gstin,

        @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
        String pan,

        String addressLine1,
        String addressLine2,
        String city,

        @Pattern(regexp = "^$|^[0-9]{2}$", message = "State code must be 2 digits")
        String stateCode,

        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Pincode must be 6 digits")
        String pincode,

        String country,
        String phone,
        String email,

        @NotBlank(message = "Invoice prefix is required")
        @Size(min = 1, max = 6, message = "Invoice prefix must be 1 to 6 characters")
        @Pattern(regexp = "^[A-Za-z0-9/-]+$", message = "Invoice prefix may only contain alphanumeric characters, hyphens, and slashes")
        String invoicePrefix,

        String bankAccountName,
        String bankAccountNumber,
        String bankIfsc,
        String bankName,
        String bankBranch,
        String upiId,
        String defaultTermsText,

        @Min(value = 0, message = "Credit days cannot be negative")
        Integer defaultCreditDays
) {}
