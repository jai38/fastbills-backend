package com.fastbills.api.receipt.dto;

import com.fastbills.api.receipt.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID organisationId,
        UUID invoiceId,
        String invoiceNumber,
        UUID customerId,
        String customerLegalName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String referenceNumber,
        String notes,
        Boolean isReversed,
        String reversalReason,
        Instant reversedAt,
        UUID reversedByUserId,
        UUID reversedByReceiptId,
        UUID createdByUserId,
        Instant createdAt
) {
}
