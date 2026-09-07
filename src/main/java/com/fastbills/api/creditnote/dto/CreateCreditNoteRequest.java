package com.fastbills.api.creditnote.dto;

import com.fastbills.api.creditnote.CreditNoteReason;
import com.fastbills.api.tax.TaxCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateCreditNoteRequest(
        @NotNull(message = "Credit note date is required")
        LocalDate creditNoteDate,

        @NotNull(message = "Reason for credit note is required")
        CreditNoteReason reason,

        String reasonNotes,

        @NotEmpty(message = "At least one credit note line is required")
        @Valid
        List<CreditNoteLineRequest> lines
) {
    public record CreditNoteLineRequest(
            UUID originalInvoiceLineId,

            @NotNull(message = "Item description is required")
            String itemDescription,

            String hsnSacCode,

            @NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be positive")
            BigDecimal quantity,

            @NotNull(message = "Unit price is required")
            @Positive(message = "Unit price must be positive")
            BigDecimal unitPrice,

            TaxCategory taxCategory,

            BigDecimal taxRatePercent,

            BigDecimal cessRatePercent
    ) {}
}
