package com.fastbills.api.tax;

import java.math.BigDecimal;
import java.util.List;

public record TaxInvoiceCalculation(
        SupplyType supplyType,
        boolean isReverseCharge,
        boolean isSupplierRegistered,
        String supplierStateCode,
        String placeOfSupplyStateCode,
        List<TaxLineCalculation> lines,
        List<TaxSlabSummary> slabSummaries,
        BigDecimal subtotalBeforeDiscount,
        BigDecimal totalDiscount,
        BigDecimal taxableValue,
        BigDecimal totalCgst,
        BigDecimal totalSgst,
        BigDecimal totalIgst,
        BigDecimal totalCess,
        BigDecimal totalTax,
        BigDecimal rawTotal,
        BigDecimal grandTotal,
        BigDecimal roundOff,
        String amountInWords
) {}
