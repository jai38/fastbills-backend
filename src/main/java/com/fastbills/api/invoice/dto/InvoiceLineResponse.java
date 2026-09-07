package com.fastbills.api.invoice.dto;

import com.fastbills.api.tax.TaxCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
        UUID id,
        int lineNumber,
        String itemDescription,
        String hsnOrSacCode,
        boolean isService,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal grossAmount,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
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
        BigDecimal lineTotal
) {}
