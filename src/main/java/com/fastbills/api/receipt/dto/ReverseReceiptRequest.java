package com.fastbills.api.receipt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseReceiptRequest(
        @NotBlank(message = "Reversal reason is mandatory")
        @Size(max = 500, message = "Reversal reason must not exceed 500 characters")
        String reason
) {
}
