package com.fastbills.api.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class IndianCurrencyFormatter {

    private IndianCurrencyFormatter() {}

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        boolean isNegative = scaled.signum() < 0;
        scaled = scaled.abs();

        String str = scaled.toPlainString();
        int dotIndex = str.indexOf('.');
        String integerPart = dotIndex > 0 ? str.substring(0, dotIndex) : str;
        String decimalPart = dotIndex > 0 ? str.substring(dotIndex + 1) : "00";

        if (integerPart.length() <= 3) {
            return (isNegative ? "-" : "") + integerPart + "." + decimalPart;
        }

        String last3 = integerPart.substring(integerPart.length() - 3);
        String remaining = integerPart.substring(0, integerPart.length() - 3);

        StringBuilder sb = new StringBuilder();
        int len = remaining.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 2 == 0) {
                sb.append(',');
            }
            sb.append(remaining.charAt(i));
        }

        return (isNegative ? "-" : "") + sb.toString() + "," + last3 + "." + decimalPart;
    }

    public static String formatWithSymbol(BigDecimal amount) {
        return "₹ " + format(amount);
    }
}
