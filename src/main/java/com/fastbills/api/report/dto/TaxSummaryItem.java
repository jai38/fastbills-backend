package com.fastbills.api.report.dto;

import java.math.BigDecimal;

public record TaxSummaryItem(
        BigDecimal taxRatePercent,
        String taxCategory,
        String label,
        int lineCount,
        BigDecimal taxableValue,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal cessAmount,
        BigDecimal totalTax
) {}
