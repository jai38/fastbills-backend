package com.fastbills.api.tax;

import java.math.BigDecimal;

public record TaxSlabSummary(
        BigDecimal taxRatePercent,
        BigDecimal taxableValue,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal cessAmount,
        BigDecimal totalTax
) {}
