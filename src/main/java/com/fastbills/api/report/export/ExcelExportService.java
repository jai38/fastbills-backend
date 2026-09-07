package com.fastbills.api.report.export;

import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportInvoiceRegister(List<InvoiceRegisterItem> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoice Register");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            String[] headers = {
                    "Invoice Number", "Date", "Due Date", "Customer Name", "Customer GSTIN",
                    "Place of Supply", "Supply Type", "State", "Taxable Value", "CGST",
                    "SGST", "IGST", "Cess", "Total Tax", "Round Off", "Grand Total",
                    "Received Amount", "Balance Amount", "Payment Status", "Ageing Bucket"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (InvoiceRegisterItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.invoiceNumber());
                row.createCell(1).setCellValue(item.invoiceDate() != null ? item.invoiceDate().toString() : "");
                row.createCell(2).setCellValue(item.dueDate() != null ? item.dueDate().toString() : "");
                row.createCell(3).setCellValue(item.customerLegalName());
                row.createCell(4).setCellValue(item.customerGstin());
                row.createCell(5).setCellValue(item.placeOfSupply());
                row.createCell(6).setCellValue(item.supplyType() != null ? item.supplyType().name() : "");
                row.createCell(7).setCellValue(item.state() != null ? item.state().name() : "");

                setNumberCell(row.createCell(8), item.taxableValue(), numberStyle);
                setNumberCell(row.createCell(9), item.totalCgst(), numberStyle);
                setNumberCell(row.createCell(10), item.totalSgst(), numberStyle);
                setNumberCell(row.createCell(11), item.totalIgst(), numberStyle);
                setNumberCell(row.createCell(12), item.totalCess(), numberStyle);
                setNumberCell(row.createCell(13), item.totalTax(), numberStyle);
                setNumberCell(row.createCell(14), item.roundOff(), numberStyle);
                setNumberCell(row.createCell(15), item.grandTotal(), numberStyle);
                setNumberCell(row.createCell(16), item.receivedAmount(), numberStyle);
                setNumberCell(row.createCell(17), item.balanceAmount(), numberStyle);

                row.createCell(18).setCellValue(item.paymentStatus());
                row.createCell(19).setCellValue(item.ageingBucket());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export for Invoice Register", e);
        }
    }

    public byte[] exportRevenueSummary(List<RevenueSummaryItem> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Revenue Summary");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            String[] headers = {
                    "Group / Period", "Invoice Count", "Taxable Value", "CGST",
                    "SGST", "IGST", "Cess", "Total Tax", "Grand Total",
                    "Received Amount", "Balance Amount"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (RevenueSummaryItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.groupLabel());
                row.createCell(1).setCellValue(item.invoiceCount());

                setNumberCell(row.createCell(2), item.taxableValue(), numberStyle);
                setNumberCell(row.createCell(3), item.totalCgst(), numberStyle);
                setNumberCell(row.createCell(4), item.totalSgst(), numberStyle);
                setNumberCell(row.createCell(5), item.totalIgst(), numberStyle);
                setNumberCell(row.createCell(6), item.totalCess(), numberStyle);
                setNumberCell(row.createCell(7), item.totalTax(), numberStyle);
                setNumberCell(row.createCell(8), item.grandTotal(), numberStyle);
                setNumberCell(row.createCell(9), item.receivedAmount(), numberStyle);
                setNumberCell(row.createCell(10), item.balanceAmount(), numberStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export for Revenue Summary", e);
        }
    }

    public byte[] exportTaxSummary(List<TaxSummaryItem> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Tax Summary by Slab");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            String[] headers = {
                    "Tax Rate / Category", "Line Count", "Taxable Value",
                    "CGST", "SGST", "IGST", "Cess", "Total Tax"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (TaxSummaryItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.label());
                row.createCell(1).setCellValue(item.lineCount());

                setNumberCell(row.createCell(2), item.taxableValue(), numberStyle);
                setNumberCell(row.createCell(3), item.cgstAmount(), numberStyle);
                setNumberCell(row.createCell(4), item.sgstAmount(), numberStyle);
                setNumberCell(row.createCell(5), item.igstAmount(), numberStyle);
                setNumberCell(row.createCell(6), item.cessAmount(), numberStyle);
                setNumberCell(row.createCell(7), item.totalTax(), numberStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export for Tax Summary", e);
        }
    }

    public byte[] exportReceivablesAgeing(List<ReceivablesAgeingItem> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Receivables Ageing");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            String[] headers = {
                    "Customer Name", "Customer GSTIN", "Total Outstanding", "Current",
                    "1-30 Days", "31-45 Days (MSME Alert)", "46-60 Days", "61-90 Days",
                    "90+ Days", "Oldest Due Date"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ReceivablesAgeingItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.customerLegalName());
                row.createCell(1).setCellValue(item.customerGstin());

                setNumberCell(row.createCell(2), item.totalOutstanding(), numberStyle);
                setNumberCell(row.createCell(3), item.current(), numberStyle);
                setNumberCell(row.createCell(4), item.days1To30(), numberStyle);
                setNumberCell(row.createCell(5), item.days31To45(), numberStyle);
                setNumberCell(row.createCell(6), item.days46To60(), numberStyle);
                setNumberCell(row.createCell(7), item.days61To90(), numberStyle);
                setNumberCell(row.createCell(8), item.over90(), numberStyle);

                row.createCell(9).setCellValue(item.oldestDueDate() != null ? item.oldestDueDate().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export for Receivables Ageing", e);
        }
    }

    public byte[] exportReceiptsRegister(List<ReceiptResponse> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Receipts Register");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            String[] headers = {
                    "Receipt ID", "Payment Date", "Invoice Number", "Customer Name",
                    "Payment Method", "Reference Number", "Amount", "Status",
                    "Reversal Reason", "Notes"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ReceiptResponse item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.id().toString());
                row.createCell(1).setCellValue(item.paymentDate() != null ? item.paymentDate().toString() : "");
                row.createCell(2).setCellValue(item.invoiceNumber());
                row.createCell(3).setCellValue(item.customerLegalName());
                row.createCell(4).setCellValue(item.paymentMethod() != null ? item.paymentMethod().name() : "");
                row.createCell(5).setCellValue(item.referenceNumber() != null ? item.referenceNumber() : "");

                setNumberCell(row.createCell(6), item.amount(), numberStyle);

                row.createCell(7).setCellValue(item.isReversed() ? "REVERSED" : "ACTIVE");
                row.createCell(8).setCellValue(item.reversalReason() != null ? item.reversalReason() : "");
                row.createCell(9).setCellValue(item.notes() != null ? item.notes() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export for Receipts Register", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private void setNumberCell(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        } else {
            cell.setCellValue(0.00);
            cell.setCellStyle(style);
        }
    }
}
