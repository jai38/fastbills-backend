package com.fastbills.api.report;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.receipt.PaymentMethod;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import com.fastbills.api.report.export.CsvExportService;
import com.fastbills.api.report.export.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Statutory GST and management reports with CSV and Excel export")
public class ReportController {

    private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";
    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final ExcelExportService excelExportService;

    public ReportController(
            ReportService reportService,
            CsvExportService csvExportService,
            ExcelExportService excelExportService
    ) {
        this.reportService = reportService;
        this.csvExportService = csvExportService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/invoice-register")
    @Operation(summary = "Invoice Register", description = "Returns all issued/cancelled invoices with derived payment status and balance")
    public ResponseEntity<?> getInvoiceRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String export,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportFilter filter = new ReportFilter(fromDate, toDate, customerId, null);
        List<InvoiceRegisterItem> items = reportService.getInvoiceRegister(user.organisationId(), filter);

        if ("csv".equalsIgnoreCase(export)) {
            byte[] data = csvExportService.exportInvoiceRegister(items);
            return createExportResponse(data, "invoice-register.csv", CSV_MEDIA_TYPE);
        } else if ("xlsx".equalsIgnoreCase(export)) {
            byte[] data = excelExportService.exportInvoiceRegister(items);
            return createExportResponse(data, "invoice-register.xlsx", XLSX_MEDIA_TYPE);
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/revenue-summary")
    @Operation(summary = "Revenue Summary", description = "Revenue aggregated by month or customer")
    public ResponseEntity<?> getRevenueSummary(
            @RequestParam(required = false, defaultValue = "month") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String export,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportFilter filter = new ReportFilter(fromDate, toDate, customerId, groupBy);
        List<RevenueSummaryItem> items = reportService.getRevenueSummary(user.organisationId(), filter);

        if ("csv".equalsIgnoreCase(export)) {
            byte[] data = csvExportService.exportRevenueSummary(items);
            return createExportResponse(data, "revenue-summary.csv", CSV_MEDIA_TYPE);
        } else if ("xlsx".equalsIgnoreCase(export)) {
            byte[] data = excelExportService.exportRevenueSummary(items);
            return createExportResponse(data, "revenue-summary.xlsx", XLSX_MEDIA_TYPE);
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/tax-summary")
    @Operation(summary = "Tax Summary by Rate Slab", description = "GSTR-1 style tax breakdown by rate slab")
    public ResponseEntity<?> getTaxSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String export,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportFilter filter = new ReportFilter(fromDate, toDate, customerId, null);
        List<TaxSummaryItem> items = reportService.getTaxSummaryByRateSlab(user.organisationId(), filter);

        if ("csv".equalsIgnoreCase(export)) {
            byte[] data = csvExportService.exportTaxSummary(items);
            return createExportResponse(data, "tax-summary.csv", CSV_MEDIA_TYPE);
        } else if ("xlsx".equalsIgnoreCase(export)) {
            byte[] data = excelExportService.exportTaxSummary(items);
            return createExportResponse(data, "tax-summary.xlsx", XLSX_MEDIA_TYPE);
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/receivables-ageing")
    @Operation(summary = "Receivables Ageing", description = "Outstanding balances across standard ageing buckets per customer")
    public ResponseEntity<?> getReceivablesAgeing(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String export,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportFilter filter = new ReportFilter(null, null, customerId, null);
        List<ReceivablesAgeingItem> items = reportService.getReceivablesAgeing(user.organisationId(), filter);

        if ("csv".equalsIgnoreCase(export)) {
            byte[] data = csvExportService.exportReceivablesAgeing(items);
            return createExportResponse(data, "receivables-ageing.csv", CSV_MEDIA_TYPE);
        } else if ("xlsx".equalsIgnoreCase(export)) {
            byte[] data = excelExportService.exportReceivablesAgeing(items);
            return createExportResponse(data, "receivables-ageing.xlsx", XLSX_MEDIA_TYPE);
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/receipts-register")
    @Operation(summary = "Receipts Register", description = "Payment receipts report with reversal status and method")
    public ResponseEntity<?> getReceiptsRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Boolean isReversed,
            @RequestParam(required = false) String export,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReportFilter filter = new ReportFilter(fromDate, toDate, customerId, null);
        List<ReceiptResponse> items = reportService.getReceiptsRegister(user.organisationId(), filter, paymentMethod, isReversed);

        if ("csv".equalsIgnoreCase(export)) {
            byte[] data = csvExportService.exportReceiptsRegister(items);
            return createExportResponse(data, "receipts-register.csv", CSV_MEDIA_TYPE);
        } else if ("xlsx".equalsIgnoreCase(export)) {
            byte[] data = excelExportService.exportReceiptsRegister(items);
            return createExportResponse(data, "receipts-register.xlsx", XLSX_MEDIA_TYPE);
        }
        return ResponseEntity.ok(items);
    }

    private ResponseEntity<byte[]> createExportResponse(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
