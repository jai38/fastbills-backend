package com.fastbills.api.creditnote.dto;

import com.fastbills.api.creditnote.CreditNoteReason;
import com.fastbills.api.creditnote.CreditNoteState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreditNoteSummaryResponse(
        UUID id,
        String creditNoteNumber,
        LocalDate creditNoteDate,
        CreditNoteReason reason,
        String reasonDescription,
        UUID originalInvoiceId,
        String originalInvoiceNumber,
        UUID customerId,
        String customerLegalName,
        BigDecimal taxableValue,
        BigDecimal totalTax,
        BigDecimal grandTotal,
        CreditNoteState state,
        Instant createdAt
) {
}
