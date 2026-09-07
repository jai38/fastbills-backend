package com.fastbills.api.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardMetricsResponse(
        BigDecimal totalOutstanding,
        BigDecimal totalOverdue,
        BigDecimal crossed45Days,
        BigDecimal receivedThisMonth,
        int invoicesRaisedThisMonthCount,
        BigDecimal invoicesRaisedThisMonthTotal,
        AgeingSummary ageingSummary,
        List<TopUnpaidInvoiceItem> topUnpaidInvoices
) {
    public record AgeingSummary(
            BigDecimal current,
            BigDecimal days1To30,
            BigDecimal days31To45,
            BigDecimal days46To60,
            BigDecimal days61To90,
            BigDecimal over90
    ) {}

    public record TopUnpaidInvoiceItem(
            UUID id,
            String invoiceNumber,
            UUID customerId,
            String customerLegalName,
            LocalDate invoiceDate,
            LocalDate dueDate,
            long daysOverdue,
            BigDecimal grandTotal,
            BigDecimal balanceAmount,
            String paymentStatus,
            String ageingBucket
    ) {}
}
