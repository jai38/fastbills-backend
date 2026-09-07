package com.fastbills.api.report;

import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.PaymentMethod;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import com.fastbills.api.report.export.CsvExportService;
import com.fastbills.api.report.export.ExcelExportService;
import com.fastbills.api.tax.SupplyType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExportServiceTest {

    private final CsvExportService csvExportService = new CsvExportService();
    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    @DisplayName("CSV and Excel export generate valid binary outputs for Invoice Register")
    void testExportInvoiceRegister() throws Exception {
        InvoiceRegisterItem item = new InvoiceRegisterItem(
                UUID.randomUUID(),
                "INV-2026-001",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 15),
                UUID.randomUUID(),
                "Acme Corp, \"Limited\"",
                "27AABCU9603R1ZM",
                "27 - Maharashtra",
                SupplyType.INTRA_STATE,
                InvoiceState.ISSUED,
                new BigDecimal("1000.00"),
                new BigDecimal("90.00"),
                new BigDecimal("90.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("180.00"),
                BigDecimal.ZERO,
                new BigDecimal("1180.00"),
                BigDecimal.ZERO,
                new BigDecimal("1180.00"),
                "UNPAID",
                "CURRENT"
        );

        List<InvoiceRegisterItem> items = List.of(item);

        // CSV test
        byte[] csv = csvExportService.exportInvoiceRegister(items);
        String csvContent = new String(csv, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("Invoice Number,Date,Due Date");
        assertThat(csvContent).contains("INV-2026-001");
        assertThat(csvContent).contains("\"Acme Corp, \"\"Limited\"\"\"");

        // Excel test
        byte[] xlsx = excelExportService.exportInvoiceRegister(items);
        assertThat(xlsx).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Invoice Register")).isNotNull();
            assertThat(wb.getSheet("Invoice Register").getRow(1).getCell(0).getStringCellValue()).isEqualTo("INV-2026-001");
        }
    }

    @Test
    @DisplayName("CSV and Excel export generate valid binary outputs for Revenue Summary")
    void testExportRevenueSummary() throws Exception {
        RevenueSummaryItem item = new RevenueSummaryItem(
                "2026-09",
                "September 2026",
                5,
                new BigDecimal("50000.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("4500.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                new BigDecimal("59000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("39000.00")
        );

        List<RevenueSummaryItem> items = List.of(item);

        byte[] csv = csvExportService.exportRevenueSummary(items);
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("September 2026");

        byte[] xlsx = excelExportService.exportRevenueSummary(items);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Revenue Summary")).isNotNull();
            assertThat(wb.getSheet("Revenue Summary").getRow(1).getCell(0).getStringCellValue()).isEqualTo("September 2026");
        }
    }

    @Test
    @DisplayName("CSV and Excel export generate valid binary outputs for Tax Summary")
    void testExportTaxSummary() throws Exception {
        TaxSummaryItem item = new TaxSummaryItem(
                new BigDecimal("18.00"),
                "TAXABLE",
                "18.00% GST",
                10,
                new BigDecimal("10000.00"),
                new BigDecimal("900.00"),
                new BigDecimal("900.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1800.00")
        );

        List<TaxSummaryItem> items = List.of(item);

        byte[] csv = csvExportService.exportTaxSummary(items);
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("18.00% GST");

        byte[] xlsx = excelExportService.exportTaxSummary(items);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Tax Summary by Slab")).isNotNull();
            assertThat(wb.getSheet("Tax Summary by Slab").getRow(1).getCell(0).getStringCellValue()).isEqualTo("18.00% GST");
        }
    }

    @Test
    @DisplayName("CSV and Excel export generate valid binary outputs for Receivables Ageing")
    void testExportReceivablesAgeing() throws Exception {
        ReceivablesAgeingItem item = new ReceivablesAgeingItem(
                UUID.randomUUID(),
                "Apex Enterprises",
                "27BBBCU9603R1ZM",
                new BigDecimal("15000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1)
        );

        List<ReceivablesAgeingItem> items = List.of(item);

        byte[] csv = csvExportService.exportReceivablesAgeing(items);
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Apex Enterprises");

        byte[] xlsx = excelExportService.exportReceivablesAgeing(items);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Receivables Ageing")).isNotNull();
            assertThat(wb.getSheet("Receivables Ageing").getRow(1).getCell(0).getStringCellValue()).isEqualTo("Apex Enterprises");
        }
    }

    @Test
    @DisplayName("CSV and Excel export generate valid binary outputs for Receipts Register")
    void testExportReceiptsRegister() throws Exception {
        ReceiptResponse item = new ReceiptResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INV-2026-001",
                UUID.randomUUID(),
                "Zenith Corp",
                new BigDecimal("2500.00"),
                LocalDate.of(2026, 9, 5),
                PaymentMethod.UPI,
                "UPI-999888",
                "Partial payment received",
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<ReceiptResponse> items = List.of(item);

        byte[] csv = csvExportService.exportReceiptsRegister(items);
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("UPI-999888");

        byte[] xlsx = excelExportService.exportReceiptsRegister(items);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheet("Receipts Register")).isNotNull();
            assertThat(wb.getSheet("Receipts Register").getRow(1).getCell(2).getStringCellValue()).isEqualTo("INV-2026-001");
        }
    }
}
