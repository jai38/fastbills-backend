package com.fastbills.api.creditnote.dto;

import com.fastbills.api.creditnote.CreditNoteReason;
import com.fastbills.api.creditnote.CreditNoteState;
import com.fastbills.api.tax.TaxCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditNoteResponse(
        UUID id,
        String creditNoteNumber,
        Integer financialYear,
        LocalDate creditNoteDate,
        CreditNoteReason reason,
        String reasonDescription,
        String reasonNotes,
        UUID originalInvoiceId,
        String originalInvoiceNumber,
        LocalDate originalInvoiceDate,
        UUID customerId,
        String customerLegalName,
        String customerGstin,
        BigDecimal taxableValue,
        BigDecimal totalCgst,
        BigDecimal totalSgst,
        BigDecimal totalIgst,
        BigDecimal totalCess,
        BigDecimal totalTax,
        BigDecimal roundOff,
        BigDecimal grandTotal,
        CreditNoteState state,
        String cancellationReason,
        Instant cancelledAt,
        Instant createdAt,
        List<CreditNoteLineResponse> lines
) {
    public record CreditNoteLineResponse(
            UUID id,
            UUID originalInvoiceLineId,
            String itemDescription,
            String hsnSacCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxableValue,
            TaxCategory taxCategory,
            BigDecimal taxRatePercent,
            BigDecimal cgstRatePercent,
            BigDecimal cgstAmount,
            BigDecimal sgstRatePercent,
            BigDecimal sgstAmount,
            BigDecimal igstRatePercent,
            BigDecimal igstAmount,
            BigDecimal cessRatePercent,
            BigDecimal cessAmount,
            BigDecimal totalTaxAmount,
            BigDecimal lineTotal,
            Integer sortOrder
    ) {}
}
