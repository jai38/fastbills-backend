package com.fastbills.api.series;

import java.time.LocalDate;
import java.time.Month;

/**
 * Indian Financial Year utility.
 * The Indian financial year runs from 1 April to 31 March.
 * Stored as the starting calendar year integer (e.g. 2024 for FY 2024-25).
 * Formatted short as "YY-YY" (e.g. "24-25").
 */
public final class FinancialYear {

    private FinancialYear() {}

    /**
     * Determines the starting calendar year of the Indian Financial Year for the given date.
     * For example:
     * - 2024-04-01 -> 2024 (FY 2024-25)
     * - 2025-03-31 -> 2024 (FY 2024-25)
     * - 2025-04-01 -> 2025 (FY 2025-26)
     * - 2027-02-15 -> 2026 (FY 2026-27)
     */
    public static int fromDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required to compute Financial Year");
        }
        int year = date.getYear();
        if (date.getMonthValue() < Month.APRIL.getValue()) {
            return year - 1;
        }
        return year;
    }

    /**
     * Formats the starting financial year as short "YY-YY" (e.g. 2024 -> "24-25", 2026 -> "26-27").
     */
    public static String formatShort(int startYear) {
        int startTwoDigits = Math.abs(startYear) % 100;
        int endTwoDigits = Math.abs(startYear + 1) % 100;
        return String.format("%02d-%02d", startTwoDigits, endTwoDigits);
    }

    /**
     * Convenience method returning short "YY-YY" for the given date.
     */
    public static String formatShort(LocalDate date) {
        return formatShort(fromDate(date));
    }

    public static LocalDate getStartDate(int startYear) {
        return LocalDate.of(startYear, Month.APRIL, 1);
    }

    public static LocalDate getEndDate(int startYear) {
        return LocalDate.of(startYear + 1, Month.MARCH, 31);
    }
}
