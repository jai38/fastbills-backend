package com.fastbills.api.tax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GstTaxEngineTest {

    @Test
    @DisplayName("Determine supply type: Intra-state for identical state codes")
    void testDetermineSupplyType_IntraState() {
        SupplyType supplyType = GstTaxEngine.determineSupplyType("27", "27");
        assertThat(supplyType).isEqualTo(SupplyType.INTRA_STATE);
    }

    @Test
    @DisplayName("Determine supply type: Inter-state for different state codes")
    void testDetermineSupplyType_InterState() {
        SupplyType supplyType = GstTaxEngine.determineSupplyType("27", "29");
        assertThat(supplyType).isEqualTo(SupplyType.INTER_STATE);
    }

    @Test
    @DisplayName("Determine supply type: Rejects export state code 96 in v1")
    void testDetermineSupplyType_ExportRejection() {
        assertThatThrownBy(() -> GstTaxEngine.determineSupplyType("27", "96"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Export supply type is out of scope for version 1");
    }

    @Test
    @DisplayName("Worked Example A (Intra-state, Maharashtra to Maharashtra): Exact match with DOMAIN.md 5.3")
    void testWorkedExampleA_IntraState() {
        // Line 1: Fabrication work, 10 qty, 1500.00 rate, 5% disc, 18% GST
        TaxLineInput line1 = new TaxLineInput(
                1,
                "Fabrication work",
                "9988",
                true,
                new BigDecimal("10.000"),
                "HRS",
                new BigDecimal("1500.00"),
                new BigDecimal("5.00"),
                null,
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        // Line 2: Mounting bracket, 2 qty, 3200.55 rate, 0 disc, 12% GST
        TaxLineInput line2 = new TaxLineInput(
                2,
                "Mounting bracket",
                "73269099",
                false,
                new BigDecimal("2.000"),
                "NOS",
                new BigDecimal("3200.55"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("12.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                "27",
                "27",
                true,
                false,
                List.of(line1, line2)
        );

        assertThat(calc.supplyType()).isEqualTo(SupplyType.INTRA_STATE);
        assertThat(calc.isReverseCharge()).isFalse();

        // Line 1 verification
        TaxLineCalculation calcLine1 = calc.lines().get(0);
        assertThat(calcLine1.grossAmount()).isEqualByComparingTo("15000.00");
        assertThat(calcLine1.discountAmount()).isEqualByComparingTo("750.00");
        assertThat(calcLine1.taxableValue()).isEqualByComparingTo("14250.00");
        assertThat(calcLine1.cgstRatePercent()).isEqualByComparingTo("9.00");
        assertThat(calcLine1.sgstRatePercent()).isEqualByComparingTo("9.00");
        assertThat(calcLine1.cgstAmount()).isEqualByComparingTo("1282.50");
        assertThat(calcLine1.sgstAmount()).isEqualByComparingTo("1282.50");
        assertThat(calcLine1.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine1.lineTotal()).isEqualByComparingTo("16815.00");

        // Line 2 verification
        TaxLineCalculation calcLine2 = calc.lines().get(1);
        assertThat(calcLine2.grossAmount()).isEqualByComparingTo("6401.10");
        assertThat(calcLine2.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine2.taxableValue()).isEqualByComparingTo("6401.10");
        assertThat(calcLine2.cgstRatePercent()).isEqualByComparingTo("6.00");
        assertThat(calcLine2.sgstRatePercent()).isEqualByComparingTo("6.00");
        // 6401.10 * 6 / 100 = 384.066 -> 384.07
        assertThat(calcLine2.cgstAmount()).isEqualByComparingTo("384.07");
        assertThat(calcLine2.sgstAmount()).isEqualByComparingTo("384.07");
        assertThat(calcLine2.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine2.lineTotal()).isEqualByComparingTo("7169.24");

        // Invoice totals
        assertThat(calc.taxableValue()).isEqualByComparingTo("20651.10");
        assertThat(calc.totalCgst()).isEqualByComparingTo("1666.57");
        assertThat(calc.totalSgst()).isEqualByComparingTo("1666.57");
        assertThat(calc.totalIgst()).isEqualByComparingTo("0.00");
        assertThat(calc.totalTax()).isEqualByComparingTo("3333.14");
        assertThat(calc.rawTotal()).isEqualByComparingTo("23984.24");
        assertThat(calc.grandTotal()).isEqualByComparingTo("23984.00");
        assertThat(calc.roundOff()).isEqualByComparingTo("-0.24");
        assertThat(calc.amountInWords()).isEqualTo("Twenty Three Thousand Nine Hundred Eighty Four Rupees Only");

        // Slab summary verification
        assertThat(calc.slabSummaries()).hasSize(2);

        TaxSlabSummary slab12 = calc.slabSummaries().stream()
                .filter(s -> s.taxRatePercent().compareTo(new BigDecimal("12.00")) == 0)
                .findFirst().orElseThrow();
        assertThat(slab12.taxableValue()).isEqualByComparingTo("6401.10");
        assertThat(slab12.cgstAmount()).isEqualByComparingTo("384.07");
        assertThat(slab12.sgstAmount()).isEqualByComparingTo("384.07");
        assertThat(slab12.igstAmount()).isEqualByComparingTo("0.00");

        TaxSlabSummary slab18 = calc.slabSummaries().stream()
                .filter(s -> s.taxRatePercent().compareTo(new BigDecimal("18.00")) == 0)
                .findFirst().orElseThrow();
        assertThat(slab18.taxableValue()).isEqualByComparingTo("14250.00");
        assertThat(slab18.cgstAmount()).isEqualByComparingTo("1282.50");
        assertThat(slab18.sgstAmount()).isEqualByComparingTo("1282.50");
        assertThat(slab18.igstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Worked Example B (Inter-state, Maharashtra to Karnataka): Exact match with DOMAIN.md 5.4")
    void testWorkedExampleB_InterState() {
        TaxLineInput line1 = new TaxLineInput(
                1,
                "Fabrication work",
                "9988",
                true,
                new BigDecimal("10.000"),
                "HRS",
                new BigDecimal("1500.00"),
                new BigDecimal("5.00"),
                null,
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        TaxLineInput line2 = new TaxLineInput(
                2,
                "Mounting bracket",
                "73269099",
                false,
                new BigDecimal("2.000"),
                "NOS",
                new BigDecimal("3200.55"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("12.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                "27",
                "29",
                true,
                false,
                List.of(line1, line2)
        );

        assertThat(calc.supplyType()).isEqualTo(SupplyType.INTER_STATE);

        // Line 1 verification
        TaxLineCalculation calcLine1 = calc.lines().get(0);
        assertThat(calcLine1.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine1.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine1.igstAmount()).isEqualByComparingTo("2565.00");

        // Line 2 verification: 6401.10 * 12 / 100 = 768.132 -> 768.13
        TaxLineCalculation calcLine2 = calc.lines().get(1);
        assertThat(calcLine2.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine2.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(calcLine2.igstAmount()).isEqualByComparingTo("768.13");

        // Invoice totals
        assertThat(calc.taxableValue()).isEqualByComparingTo("20651.10");
        assertThat(calc.totalCgst()).isEqualByComparingTo("0.00");
        assertThat(calc.totalSgst()).isEqualByComparingTo("0.00");
        // Example B total tax is 3333.13 (one paise below Example A 3333.14)
        assertThat(calc.totalIgst()).isEqualByComparingTo("3333.13");
        assertThat(calc.totalTax()).isEqualByComparingTo("3333.13");
        assertThat(calc.rawTotal()).isEqualByComparingTo("23984.23");
        assertThat(calc.grandTotal()).isEqualByComparingTo("23984.00");
        assertThat(calc.roundOff()).isEqualByComparingTo("-0.23");
        assertThat(calc.amountInWords()).isEqualTo("Twenty Three Thousand Nine Hundred Eighty Four Rupees Only");
    }

    @Test
    @DisplayName("Unregistered supplier: No tax applied to lines or totals")
    void testUnregisteredSupplier_NoTax() {
        TaxLineInput line = new TaxLineInput(
                1,
                "Consultancy",
                "998311",
                true,
                BigDecimal.ONE,
                "UNT",
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                "27",
                "27",
                false, // Supplier not registered
                false,
                List.of(line)
        );

        assertThat(calc.totalTax()).isEqualByComparingTo("0.00");
        assertThat(calc.totalCgst()).isEqualByComparingTo("0.00");
        assertThat(calc.totalSgst()).isEqualByComparingTo("0.00");
        assertThat(calc.taxableValue()).isEqualByComparingTo("5000.00");
        assertThat(calc.grandTotal()).isEqualByComparingTo("5000.00");
        assertThat(calc.roundOff()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Exempt / Nil Rated categories produce zero tax even when registered")
    void testExemptAndNilRatedCategories() {
        TaxLineInput exemptLine = new TaxLineInput(
                1,
                "Organic Vegetables",
                "0701",
                false,
                new BigDecimal("5.000"),
                "KGS",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.EXEMPT
        );

        TaxLineInput nilLine = new TaxLineInput(
                2,
                "Salt",
                "2501",
                false,
                new BigDecimal("2.000"),
                "NOS",
                new BigDecimal("20.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null,
                null,
                TaxCategory.NIL_RATED
        );

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                "27",
                "27",
                true,
                false,
                List.of(exemptLine, nilLine)
        );

        assertThat(calc.taxableValue()).isEqualByComparingTo("540.00");
        assertThat(calc.totalTax()).isEqualByComparingTo("0.00");
        assertThat(calc.grandTotal()).isEqualByComparingTo("540.00");
    }

    @Test
    @DisplayName("Direct discount amount takes precedence over percentage")
    void testDirectDiscountAmountPrecedence() {
        TaxLineInput line = new TaxLineInput(
                1,
                "Custom Hardware",
                "8471",
                false,
                new BigDecimal("2.000"),
                "NOS",
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"), // 50% = 1000.00
                new BigDecimal("300.00"), // direct amount 300.00 should take precedence
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        TaxLineCalculation calc = GstTaxEngine.calculateLine(line, SupplyType.INTRA_STATE, true);
        assertThat(calc.grossAmount()).isEqualByComparingTo("2000.00");
        assertThat(calc.discountAmount()).isEqualByComparingTo("300.00");
        assertThat(calc.taxableValue()).isEqualByComparingTo("1700.00");
        assertThat(calc.cgstAmount()).isEqualByComparingTo("153.00"); // 1700 * 9%
        assertThat(calc.sgstAmount()).isEqualByComparingTo("153.00");
    }

    @Test
    @DisplayName("Discount exceeding gross amount throws exception")
    void testDiscountExceedingGross_ThrowsException() {
        TaxLineInput line = new TaxLineInput(
                1,
                "Defective Item",
                "8471",
                false,
                BigDecimal.ONE,
                "NOS",
                new BigDecimal("100.00"),
                null,
                new BigDecimal("150.00"),
                new BigDecimal("18.00"),
                null,
                null,
                TaxCategory.TAXABLE
        );

        assertThatThrownBy(() -> GstTaxEngine.calculateLine(line, SupplyType.INTRA_STATE, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount amount (150.00) cannot exceed gross amount (100.00)");
    }

    @Test
    @DisplayName("Indian number to words conversion")
    void testIndianNumberToWords() {
        assertThat(IndianNumberToWords.convert(BigDecimal.ZERO)).isEqualTo("Zero Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("1.00"))).isEqualTo("One Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("15.00"))).isEqualTo("Fifteen Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("23984.00")))
                .isEqualTo("Twenty Three Thousand Nine Hundred Eighty Four Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("100000.00"))).isEqualTo("One Lakh Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("10000000.00"))).isEqualTo("One Crore Rupees Only");
        assertThat(IndianNumberToWords.convert(new BigDecimal("23984.50")))
                .isEqualTo("Twenty Three Thousand Nine Hundred Eighty Four Rupees and Fifty Paise Only");
    }
}
