package com.fastbills.api.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivablesAgeingItem(
        UUID customerId,
        String customerLegalName,
        String customerGstin,
        BigDecimal totalOutstanding,
        BigDecimal current,
        BigDecimal days1To30,
        BigDecimal days31To45,
        BigDecimal days46To60,
        BigDecimal days61To90,
        BigDecimal over90,
        LocalDate oldestDueDate
) {}
