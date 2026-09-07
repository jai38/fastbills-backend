package com.fastbills.api.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelInvoiceRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(min = 5, message = "Cancellation reason must be at least 5 characters")
        String reason
) {}
