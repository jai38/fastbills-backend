package com.fastbills.api.invoice.dto;

import com.fastbills.api.tax.TaxCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceLineRequest(
        Integer lineNumber,
        @NotBlank(message = "Item description is required")
        String itemDescription,
        String hsnOrSacCode,
        Boolean isService,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
        BigDecimal quantity,
        String unitOfMeasure,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        TaxCategory taxCategory,
        @NotNull(message = "Tax rate is required")
        @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
        BigDecimal taxRatePercent
) {}
