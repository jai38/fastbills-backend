package com.fastbills.api.tax;

import java.math.BigDecimal;

public record TaxLineCalculation(
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
        BigDecimal cessAmount,
        BigDecimal lineTotal
) {}
