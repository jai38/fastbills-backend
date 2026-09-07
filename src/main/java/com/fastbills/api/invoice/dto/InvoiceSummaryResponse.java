package com.fastbills.api.invoice.dto;

import com.fastbills.api.invoice.InvoiceState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
        UUID id,
        String invoiceNumber,
        String previewInvoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        UUID customerId,
        String customerLegalName,
        BigDecimal grandTotal,
        BigDecimal totalReceived,
        BigDecimal balanceAmount,
        InvoiceState state,
        String paymentStatus,
        String ageingBucket,
        String collectionReference
) {}
