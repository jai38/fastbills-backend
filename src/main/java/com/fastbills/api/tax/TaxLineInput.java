package com.fastbills.api.tax;

import java.math.BigDecimal;

public record TaxLineInput(
        int lineNumber,
        String itemDescription,
        String hsnOrSacCode,
        boolean isService,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal taxRatePercent,
        BigDecimal cessRatePercent,
        BigDecimal cessSpecificAmount,
        TaxCategory taxCategory
) {
    public TaxLineInput(
            int lineNumber,
            String itemDescription,
            String hsnOrSacCode,
            boolean isService,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRatePercent
    ) {
        this(
                lineNumber,
                itemDescription,
                hsnOrSacCode,
                isService,
                quantity,
                unitOfMeasure,
                unitPrice,
                discountPercent,
                null,
                taxRatePercent,
                null,
                null,
                TaxCategory.TAXABLE
        );
    }
}
