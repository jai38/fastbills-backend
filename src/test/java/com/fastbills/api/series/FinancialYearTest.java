package com.fastbills.api.series;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialYearTest {

    @Test
    @DisplayName("Financial year boundary: March 31 belongs to previous starting year")
    void testMarch31Boundary() {
        LocalDate march31 = LocalDate.of(2025, 3, 31);
        int fy = FinancialYear.fromDate(march31);
        assertThat(fy).isEqualTo(2024);
        assertThat(FinancialYear.formatShort(fy)).isEqualTo("24-25");
        assertThat(FinancialYear.formatShort(march31)).isEqualTo("24-25");
    }

    @Test
    @DisplayName("Financial year boundary: April 1 rolls over to new starting year")
    void testApril1Boundary() {
        LocalDate april1 = LocalDate.of(2025, 4, 1);
        int fy = FinancialYear.fromDate(april1);
        assertThat(fy).isEqualTo(2025);
        assertThat(FinancialYear.formatShort(fy)).isEqualTo("25-26");
        assertThat(FinancialYear.formatShort(april1)).isEqualTo("25-26");
    }

    @Test
    @DisplayName("Mid-year dates: February 15 2027 belongs to FY 2026-27 (DOMAIN.md line 382)")
    void testFebruaryDate() {
        LocalDate feb15 = LocalDate.of(2027, 2, 15);
        int fy = FinancialYear.fromDate(feb15);
        assertThat(fy).isEqualTo(2026);
        assertThat(FinancialYear.formatShort(fy)).isEqualTo("26-27");
    }

    @Test
    @DisplayName("December dates: December 31 2024 belongs to FY 2024-25")
    void testDecemberDate() {
        LocalDate dec31 = LocalDate.of(2024, 12, 31);
        int fy = FinancialYear.fromDate(dec31);
        assertThat(fy).isEqualTo(2024);
        assertThat(FinancialYear.formatShort(fy)).isEqualTo("24-25");
    }

    @Test
    @DisplayName("Century boundary formatting: FY 2099 rolls over to 99-00")
    void testCenturyRollover() {
        assertThat(FinancialYear.formatShort(2099)).isEqualTo("99-00");
        assertThat(FinancialYear.formatShort(1999)).isEqualTo("99-00");
    }

    @Test
    @DisplayName("Start and end date boundaries for FY")
    void testStartAndEndDates() {
        assertThat(FinancialYear.getStartDate(2024)).isEqualTo(LocalDate.of(2024, 4, 1));
        assertThat(FinancialYear.getEndDate(2024)).isEqualTo(LocalDate.of(2025, 3, 31));
    }

    @Test
    @DisplayName("Null date throws IllegalArgumentException")
    void testNullDateThrows() {
        assertThatThrownBy(() -> FinancialYear.fromDate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
