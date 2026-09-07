package com.fastbills.api.series;

import java.util.UUID;

public record AllocatedNumber(
        UUID seriesId,
        int sequenceNumber,
        String formattedNumber,
        int financialYear,
        String financialYearShort
) {}
