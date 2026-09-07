package com.fastbills.api.report.dto;

import java.math.BigDecimal;

public record RevenueSummaryItem(
        String groupKey,
        String groupLabel,
        int invoiceCount,
        BigDecimal taxableValue,
        BigDecimal totalCgst,
        BigDecimal totalSgst,
        BigDecimal totalIgst,
        BigDecimal totalCess,
        BigDecimal totalTax,
        BigDecimal grandTotal,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount
) {}
