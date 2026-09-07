package com.fastbills.api.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateDraftInvoiceRequest(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        LocalDate deliveryOrAcceptanceDate,

        Integer agreedCreditDays,

        @Pattern(regexp = "^[0-9]{2}$", message = "Place of supply state code must be a 2-digit number")
        String placeOfSupplyStateCode,

        Boolean isReverseCharge,

        String poNumber,

        LocalDate poDate,

        String notes,

        String termsText,

        @Valid
        List<InvoiceLineRequest> lines
) {}
