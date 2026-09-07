package com.fastbills.api.report.export;

import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportService {

    public byte[] exportInvoiceRegister(List<InvoiceRegisterItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Invoice Number,Date,Due Date,Customer Name,Customer GSTIN,Place of Supply,Supply Type,State,Taxable Value,CGST,SGST,IGST,Cess,Total Tax,Round Off,Grand Total,Received Amount,Balance Amount,Payment Status,Ageing Bucket");

            for (InvoiceRegisterItem item : items) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escape(item.invoiceNumber()),
                        item.invoiceDate() != null ? item.invoiceDate() : "",
                        item.dueDate() != null ? item.dueDate() : "",
                        escape(item.customerLegalName()),
                        escape(item.customerGstin()),
                        escape(item.placeOfSupply()),
                        item.supplyType() != null ? item.supplyType().name() : "",
                        item.state() != null ? item.state().name() : "",
                        formatNumber(item.taxableValue()),
                        formatNumber(item.totalCgst()),
                        formatNumber(item.totalSgst()),
                        formatNumber(item.totalIgst()),
                        formatNumber(item.totalCess()),
                        formatNumber(item.totalTax()),
                        formatNumber(item.roundOff()),
                        formatNumber(item.grandTotal()),
                        formatNumber(item.receivedAmount()),
                        formatNumber(item.balanceAmount()),
                        escape(item.paymentStatus()),
                        escape(item.ageingBucket())
                );
            }
        }
        return out.toByteArray();
    }

    public byte[] exportRevenueSummary(List<RevenueSummaryItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Group / Period,Invoice Count,Taxable Value,CGST,SGST,IGST,Cess,Total Tax,Grand Total,Received Amount,Balance Amount");

            for (RevenueSummaryItem item : items) {
                writer.printf("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escape(item.groupLabel()),
                        item.invoiceCount(),
                        formatNumber(item.taxableValue()),
                        formatNumber(item.totalCgst()),
                        formatNumber(item.totalSgst()),
                        formatNumber(item.totalIgst()),
                        formatNumber(item.totalCess()),
                        formatNumber(item.totalTax()),
                        formatNumber(item.grandTotal()),
                        formatNumber(item.receivedAmount()),
                        formatNumber(item.balanceAmount())
                );
            }
        }
        return out.toByteArray();
    }

    public byte[] exportTaxSummary(List<TaxSummaryItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Tax Rate / Category,Line Count,Taxable Value,CGST,SGST,IGST,Cess,Total Tax");

            for (TaxSummaryItem item : items) {
                writer.printf("%s,%d,%s,%s,%s,%s,%s,%s%n",
                        escape(item.label()),
                        item.lineCount(),
                        formatNumber(item.taxableValue()),
                        formatNumber(item.cgstAmount()),
                        formatNumber(item.sgstAmount()),
                        formatNumber(item.igstAmount()),
                        formatNumber(item.cessAmount()),
                        formatNumber(item.totalTax())
                );
            }
        }
        return out.toByteArray();
    }

    public byte[] exportReceivablesAgeing(List<ReceivablesAgeingItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Customer Name,Customer GSTIN,Total Outstanding,Current,1-30 Days,31-45 Days (MSME Alert),46-60 Days,61-90 Days,90+ Days,Oldest Due Date");

            for (ReceivablesAgeingItem item : items) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escape(item.customerLegalName()),
                        escape(item.customerGstin()),
                        formatNumber(item.totalOutstanding()),
                        formatNumber(item.current()),
                        formatNumber(item.days1To30()),
                        formatNumber(item.days31To45()),
                        formatNumber(item.days46To60()),
                        formatNumber(item.days61To90()),
                        formatNumber(item.over90()),
                        item.oldestDueDate() != null ? item.oldestDueDate() : ""
                );
            }
        }
        return out.toByteArray();
    }

    public byte[] exportReceiptsRegister(List<ReceiptResponse> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Receipt ID,Payment Date,Invoice Number,Customer Name,Payment Method,Reference Number,Amount,Status,Reversal Reason,Notes");

            for (ReceiptResponse item : items) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        item.id(),
                        item.paymentDate() != null ? item.paymentDate() : "",
                        escape(item.invoiceNumber()),
                        escape(item.customerLegalName()),
                        item.paymentMethod(),
                        escape(item.referenceNumber()),
                        formatNumber(item.amount()),
                        item.isReversed() ? "REVERSED" : "ACTIVE",
                        escape(item.reversalReason()),
                        escape(item.notes())
                );
            }
        }
        return out.toByteArray();
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String formatNumber(BigDecimal val) {
        return val != null ? val.toPlainString() : "0.00";
    }
}
