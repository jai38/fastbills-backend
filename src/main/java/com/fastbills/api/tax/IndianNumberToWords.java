package com.fastbills.api.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class IndianNumberToWords {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private IndianNumberToWords() {}

    public static String convert(BigDecimal amount) {
        if (amount == null) {
            return "Zero Rupees Only";
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = normalized.longValue();
        long paise = normalized.remainder(BigDecimal.ONE).movePointRight(2).abs().longValue();

        if (rupees == 0 && paise == 0) {
            return "Zero Rupees Only";
        }

        StringBuilder words = new StringBuilder();

        if (rupees > 0) {
            words.append(convertNumber(rupees)).append(" Rupees");
        }

        if (paise > 0) {
            if (rupees > 0) {
                words.append(" and ");
            }
            words.append(convertNumber(paise)).append(" Paise");
        }

        words.append(" Only");
        return words.toString();
    }

    private static String convertNumber(long n) {
        if (n == 0) {
            return "Zero";
        }

        StringBuilder result = new StringBuilder();

        if (n >= 10000000) { // Crores (1,00,00,000)
            long crores = n / 10000000;
            result.append(convertNumber(crores)).append(" Crore ");
            n %= 10000000;
        }

        if (n >= 100000) { // Lakhs (1,00,000)
            long lakhs = n / 100000;
            result.append(convertNumber(lakhs)).append(" Lakh ");
            n %= 100000;
        }

        if (n >= 1000) { // Thousands (1,000)
            long thousands = n / 1000;
            result.append(convertNumber(thousands)).append(" Thousand ");
            n %= 1000;
        }

        if (n >= 100) { // Hundreds (100)
            long hundreds = n / 100;
            result.append(UNITS[(int) hundreds]).append(" Hundred ");
            n %= 100;
        }

        if (n > 0) {
            if (n < 20) {
                result.append(UNITS[(int) n]).append(" ");
            } else {
                long tens = n / 10;
                long units = n % 10;
                result.append(TENS[(int) tens]).append(" ");
                if (units > 0) {
                    result.append(UNITS[(int) units]).append(" ");
                }
            }
        }

        return result.toString().trim();
    }
}
