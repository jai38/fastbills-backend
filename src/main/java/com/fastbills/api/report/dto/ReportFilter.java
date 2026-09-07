package com.fastbills.api.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReportFilter(
        LocalDate fromDate,
        LocalDate toDate,
        UUID customerId,
        String groupBy
) {}
