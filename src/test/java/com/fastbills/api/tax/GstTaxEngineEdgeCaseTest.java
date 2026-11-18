package com.fastbills.api.tax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases from the GST test checklist (A3-A7, A9): rounding boundaries,
 * round-off sign, zero rate, discount forms, quantity precision, large amounts.
 */
class GstTaxEngineEdgeCaseTest {

    private static TaxLineInput line(String qty, String price, String discPct, String discAmt,
                                     String rate, TaxCategory category) {
        return new TaxLineInput(
                1, "Item", "9988", true,
                new BigDecimal(qty), "NOS", new BigDecimal(price),
                discPct == null ? null : new BigDecimal(discPct),
                discAmt == null ? null : new BigDecimal(discAmt),
                new BigDecimal(rate), null, null, category);
    }

    private static TaxInvoiceCalculation calc(boolean registered, String pos, TaxLineInput... lines) {
        return GstTaxEngine.calculateInvoice("27", pos, registered, false, List.of(lines));
    }

    @Test
    @DisplayName("A3: exact half-cent tie rounds HALF_UP (0.25 @ 18% IGST = 0.045 -> 0.05)")
    void halfUpTie() {
        TaxInvoiceCalculation c = calc(true, "29", line("1", "0.25", null, null, "18.00", TaxCategory.TAXABLE));
        assertThat(c.lines().get(0).igstAmount()).isEqualByComparingTo("0.05");
    }

    @Test
    @DisplayName("A4: round-off at .49 / .50 / .51 is signed and rounds half up")
    void roundOffSign() {
        TaxInvoiceCalculation down = calc(false, "27", line("1", "100.49", null, null, "0.00", TaxCategory.TAXABLE));
        assertThat(down.grandTotal()).isEqualByComparingTo("100.00");
        assertThat(down.roundOff()).isEqualByComparingTo("-0.49");

        TaxInvoiceCalculation tie = calc(false, "27", line("1", "100.50", null, null, "0.00", TaxCategory.TAXABLE));
        assertThat(tie.grandTotal()).isEqualByComparingTo("101.00");
        assertThat(tie.roundOff()).isEqualByComparingTo("0.50");

        TaxInvoiceCalculation up = calc(false, "27", line("1", "100.51", null, null, "0.00", TaxCategory.TAXABLE));
        assertThat(up.grandTotal()).isEqualByComparingTo("101.00");
        assertThat(up.roundOff()).isEqualByComparingTo("0.49");
    }

    @Test
    @DisplayName("A5: 0% TAXABLE line produces no tax")
    void zeroRateTaxable() {
        TaxInvoiceCalculation c = calc(true, "27", line("2", "500.00", null, null, "0.00", TaxCategory.TAXABLE));
        assertThat(c.totalTax()).isEqualByComparingTo("0.00");
        assertThat(c.grandTotal()).isEqualByComparingTo("1000.00");
        assertThat(c.roundOff()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("A6: discount as 10% or as 100.00 on 1000.00 gives the same taxable value and totals")
    void discountPercentEqualsAmount() {
        TaxInvoiceCalculation pct = calc(true, "27", line("1", "1000.00", "10.00", null, "18.00", TaxCategory.TAXABLE));
        TaxInvoiceCalculation amt = calc(true, "27", line("1", "1000.00", null, "100.00", "18.00", TaxCategory.TAXABLE));
        assertThat(pct.taxableValue()).isEqualByComparingTo("900.00");
        assertThat(amt.taxableValue()).isEqualByComparingTo(pct.taxableValue());
        assertThat(amt.rawTotal()).isEqualByComparingTo(pct.rawTotal());
        assertThat(pct.rawTotal()).isEqualByComparingTo("1062.00");
    }

    @Test
    @DisplayName("A7: 3-decimal quantity x 2-decimal price has no drift (0.125 x 999.99 = 125.00)")
    void quantityPrecision() {
        TaxInvoiceCalculation c = calc(false, "27", line("0.125", "999.99", null, null, "0.00", TaxCategory.TAXABLE));
        // 124.99875 -> 125.00
        assertThat(c.lines().get(0).grossAmount()).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("A9: amounts above 1 crore keep full precision")
    void largeAmounts() {
        TaxInvoiceCalculation c = calc(true, "29", line("1", "12345678901.23", null, null, "18.00", TaxCategory.TAXABLE));
        // 12345678901.23 * 18% = 2222222202.2214 -> 2222222202.22
        assertThat(c.totalIgst()).isEqualByComparingTo("2222222202.22");
        assertThat(c.rawTotal()).isEqualByComparingTo("14567901103.45");
        assertThat(c.grandTotal()).isEqualByComparingTo("14567901103.00");
        assertThat(c.roundOff()).isEqualByComparingTo("-0.45");
    }
}
