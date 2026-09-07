package com.fastbills.api.receipt.dto;

import com.fastbills.api.receipt.PaymentMethod;

import java.time.LocalDate;
import java.util.UUID;

public record ReceiptFilter(
        UUID invoiceId,
        UUID customerId,
        LocalDate fromDate,
        LocalDate toDate,
        PaymentMethod paymentMethod,
        Boolean isReversed
) {
}
