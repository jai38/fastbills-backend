package com.fastbills.api.hsn.dto;

import java.math.BigDecimal;

public record HsnLookupResponse(
        String code,
        String name,
        String description,
        BigDecimal defaultTaxRatePercent,
        boolean isService,
        boolean isValid
) {}
