package com.fastbills.api.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank(message = "Legal name is required")
        @Size(max = 255, message = "Legal name cannot exceed 255 characters")
        String legalName,

        @Size(max = 255, message = "Trade name cannot exceed 255 characters")
        String tradeName,

        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
        String gstin,

        @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
        String pan,

        Boolean isRegistered,

        String billingAddressLine1,
        String billingAddressLine2,
        String billingCity,

        @NotBlank(message = "Billing state code is required")
        @Pattern(regexp = "^[0-9]{2}$", message = "Billing state code must be exactly 2 digits")
        String billingStateCode,

        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Billing pincode must be 6 digits")
        String billingPincode,

        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,

        @Pattern(regexp = "^$|^[0-9]{2}$", message = "Shipping state code must be 2 digits")
        String shippingStateCode,

        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Shipping pincode must be 6 digits")
        String shippingPincode,

        String contactPerson,
        String phone,
        String email,

        @Pattern(regexp = "^$|^[0-9]{2}$", message = "Default place of supply state code must be 2 digits")
        String defaultPlaceOfSupplyStateCode,

        Integer creditDays,
        String notes
) {}
