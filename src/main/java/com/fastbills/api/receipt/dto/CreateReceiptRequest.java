package com.fastbills.api.receipt.dto;

import com.fastbills.api.receipt.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReceiptRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment date is required")
        LocalDate paymentDate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Size(max = 100, message = "Reference number must not exceed 100 characters")
        String referenceNumber,

        String notes
) {
}
