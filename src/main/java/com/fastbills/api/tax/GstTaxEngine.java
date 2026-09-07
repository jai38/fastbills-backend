package com.fastbills.api.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public final class GstTaxEngine {

    public static final int SCALE_CURRENCY = 2;
    public static final int SCALE_QUANTITY = 3;
    public static final int SCALE_RATE = 2;

    public static final BigDecimal ZERO_CURRENCY = BigDecimal.ZERO.setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);
    public static final BigDecimal ZERO_QUANTITY = BigDecimal.ZERO.setScale(SCALE_QUANTITY, RoundingMode.HALF_UP);
    public static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(SCALE_RATE, RoundingMode.HALF_UP);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private GstTaxEngine() {}

    /**
     * Determines supply type strictly based on supplier state and place of supply state.
     */
    public static SupplyType determineSupplyType(String supplierStateCode, String placeOfSupplyStateCode) {
        if (supplierStateCode == null || supplierStateCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier state code is required to compute GST");
        }
        if (placeOfSupplyStateCode == null || placeOfSupplyStateCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Place of supply state code is required to compute GST");
        }

        String suppState = supplierStateCode.trim();
        String posState = placeOfSupplyStateCode.trim();

        if ("96".equals(posState)) {
            throw new IllegalArgumentException("Export supply type is out of scope for version 1");
        }

        if (suppState.equals(posState)) {
            return SupplyType.INTRA_STATE;
        } else {
            return SupplyType.INTER_STATE;
        }
    }

    /**
     * Computes line-level tax and totals.
     */
    public static TaxLineCalculation calculateLine(
            TaxLineInput input,
            SupplyType supplyType,
            boolean isSupplierRegistered
    ) {
        if (input == null) {
            throw new IllegalArgumentException("Tax line input cannot be null");
        }

        BigDecimal qty = (input.quantity() != null ? input.quantity() : BigDecimal.ONE)
                .setScale(SCALE_QUANTITY, RoundingMode.HALF_UP);
        BigDecimal unitPrice = (input.unitPrice() != null ? input.unitPrice() : BigDecimal.ZERO)
                .setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        BigDecimal grossAmount = qty.multiply(unitPrice).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        // Compute discount
        BigDecimal discountPercent = input.discountPercent() != null
                ? input.discountPercent().setScale(SCALE_RATE, RoundingMode.HALF_UP)
                : ZERO_RATE;

        BigDecimal discountAmount;
        if (input.discountAmount() != null && input.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = input.discountAmount().setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);
        } else if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = grossAmount.multiply(discountPercent).divide(ONE_HUNDRED, SCALE_CURRENCY, RoundingMode.HALF_UP);
        } else {
            discountAmount = ZERO_CURRENCY;
        }

        if (discountAmount.compareTo(grossAmount) > 0) {
            throw new IllegalArgumentException("Line " + input.lineNumber() + ": Discount amount (" + discountAmount + ") cannot exceed gross amount (" + grossAmount + ")");
        }

        BigDecimal taxableValue = grossAmount.subtract(discountAmount).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        TaxCategory category = input.taxCategory() != null ? input.taxCategory() : TaxCategory.TAXABLE;

        BigDecimal taxRatePercent = input.taxRatePercent() != null
                ? input.taxRatePercent().setScale(SCALE_RATE, RoundingMode.HALF_UP)
                : ZERO_RATE;

        BigDecimal cgstRate = ZERO_RATE;
        BigDecimal cgstAmount = ZERO_CURRENCY;
        BigDecimal sgstRate = ZERO_RATE;
        BigDecimal sgstAmount = ZERO_CURRENCY;
        BigDecimal igstRate = ZERO_RATE;
        BigDecimal igstAmount = ZERO_CURRENCY;
        BigDecimal cessAmount = ZERO_CURRENCY;

        // Tax applies only if supplier is registered and line is TAXABLE
        if (isSupplierRegistered && category == TaxCategory.TAXABLE && taxRatePercent.compareTo(BigDecimal.ZERO) > 0) {
            if (supplyType == SupplyType.INTRA_STATE) {
                cgstRate = taxRatePercent.divide(TWO, SCALE_RATE, RoundingMode.HALF_UP);
                sgstRate = cgstRate;
                cgstAmount = taxableValue.multiply(cgstRate).divide(ONE_HUNDRED, SCALE_CURRENCY, RoundingMode.HALF_UP);
                sgstAmount = taxableValue.multiply(sgstRate).divide(ONE_HUNDRED, SCALE_CURRENCY, RoundingMode.HALF_UP);
            } else if (supplyType == SupplyType.INTER_STATE) {
                igstRate = taxRatePercent;
                igstAmount = taxableValue.multiply(igstRate).divide(ONE_HUNDRED, SCALE_CURRENCY, RoundingMode.HALF_UP);
            }

            // Cess calculation if provided
            if (input.cessRatePercent() != null && input.cessRatePercent().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cessRate = input.cessRatePercent().setScale(SCALE_RATE, RoundingMode.HALF_UP);
                cessAmount = cessAmount.add(
                        taxableValue.multiply(cessRate).divide(ONE_HUNDRED, SCALE_CURRENCY, RoundingMode.HALF_UP)
                );
            }
            if (input.cessSpecificAmount() != null && input.cessSpecificAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cessPerUnit = input.cessSpecificAmount().setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);
                cessAmount = cessAmount.add(
                        qty.multiply(cessPerUnit).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)
                );
            }
        }

        BigDecimal lineTotal = taxableValue.add(cgstAmount).add(sgstAmount).add(igstAmount).add(cessAmount)
                .setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        return new TaxLineCalculation(
                input.lineNumber(),
                input.itemDescription(),
                input.hsnOrSacCode(),
                input.isService(),
                qty,
                input.unitOfMeasure(),
                unitPrice,
                grossAmount,
                discountPercent,
                discountAmount,
                taxableValue,
                category,
                taxRatePercent,
                cgstRate,
                cgstAmount,
                sgstRate,
                sgstAmount,
                igstRate,
                igstAmount,
                cessAmount,
                lineTotal
        );
    }

    /**
     * Computes full invoice tax, totals, rate slab breakdown, and rupee round-off.
     */
    public static TaxInvoiceCalculation calculateInvoice(
            String supplierStateCode,
            String placeOfSupplyStateCode,
            boolean isSupplierRegistered,
            boolean isReverseCharge,
            List<TaxLineInput> lineInputs
    ) {
        if (lineInputs == null || lineInputs.isEmpty()) {
            throw new IllegalArgumentException("At least one invoice line is required to compute invoice tax");
        }

        SupplyType supplyType = determineSupplyType(supplierStateCode, placeOfSupplyStateCode);

        List<TaxLineCalculation> calculatedLines = new ArrayList<>(lineInputs.size());
        for (TaxLineInput lineInput : lineInputs) {
            calculatedLines.add(calculateLine(lineInput, supplyType, isSupplierRegistered));
        }

        BigDecimal subtotalBeforeDiscount = ZERO_CURRENCY;
        BigDecimal totalDiscount = ZERO_CURRENCY;
        BigDecimal taxableValue = ZERO_CURRENCY;
        BigDecimal totalCgst = ZERO_CURRENCY;
        BigDecimal totalSgst = ZERO_CURRENCY;
        BigDecimal totalIgst = ZERO_CURRENCY;
        BigDecimal totalCess = ZERO_CURRENCY;

        // Group by tax rate for slab summary
        Map<BigDecimal, List<TaxLineCalculation>> linesByRate = new TreeMap<>();

        for (TaxLineCalculation line : calculatedLines) {
            subtotalBeforeDiscount = subtotalBeforeDiscount.add(line.grossAmount());
            totalDiscount = totalDiscount.add(line.discountAmount());
            taxableValue = taxableValue.add(line.taxableValue());
            totalCgst = totalCgst.add(line.cgstAmount());
            totalSgst = totalSgst.add(line.sgstAmount());
            totalIgst = totalIgst.add(line.igstAmount());
            totalCess = totalCess.add(line.cessAmount());

            linesByRate.computeIfAbsent(line.taxRatePercent(), k -> new ArrayList<>()).add(line);
        }

        List<TaxSlabSummary> slabSummaries = new ArrayList<>();
        for (Map.Entry<BigDecimal, List<TaxLineCalculation>> entry : linesByRate.entrySet()) {
            BigDecimal rate = entry.getKey();
            List<TaxLineCalculation> rateLines = entry.getValue();

            BigDecimal slabTaxable = ZERO_CURRENCY;
            BigDecimal slabCgst = ZERO_CURRENCY;
            BigDecimal slabSgst = ZERO_CURRENCY;
            BigDecimal slabIgst = ZERO_CURRENCY;
            BigDecimal slabCess = ZERO_CURRENCY;

            for (TaxLineCalculation l : rateLines) {
                slabTaxable = slabTaxable.add(l.taxableValue());
                slabCgst = slabCgst.add(l.cgstAmount());
                slabSgst = slabSgst.add(l.sgstAmount());
                slabIgst = slabIgst.add(l.igstAmount());
                slabCess = slabCess.add(l.cessAmount());
            }

            BigDecimal slabTotalTax = slabCgst.add(slabSgst).add(slabIgst).add(slabCess);
            slabSummaries.add(new TaxSlabSummary(rate, slabTaxable, slabCgst, slabSgst, slabIgst, slabCess, slabTotalTax));
        }

        BigDecimal totalTax = totalCgst.add(totalSgst).add(totalIgst).add(totalCess);
        BigDecimal rawTotal = taxableValue.add(totalTax).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        // Grand total is rounded to the nearest whole rupee
        BigDecimal grandTotal = rawTotal.setScale(0, RoundingMode.HALF_UP).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);
        BigDecimal roundOff = grandTotal.subtract(rawTotal).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP);

        String amountInWords = IndianNumberToWords.convert(grandTotal);

        return new TaxInvoiceCalculation(
                supplyType,
                isReverseCharge,
                isSupplierRegistered,
                supplierStateCode.trim(),
                placeOfSupplyStateCode.trim(),
                Collections.unmodifiableList(calculatedLines),
                Collections.unmodifiableList(slabSummaries),
                subtotalBeforeDiscount,
                totalDiscount,
                taxableValue,
                totalCgst,
                totalSgst,
                totalIgst,
                totalCess,
                totalTax,
                rawTotal,
                grandTotal,
                roundOff,
                amountInWords
        );
    }
}
