package com.fastbills.api.report.dto;

import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.tax.SupplyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceRegisterItem(
        UUID id,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        UUID customerId,
        String customerLegalName,
        String customerGstin,
        String placeOfSupply,
        SupplyType supplyType,
        InvoiceState state,
        BigDecimal taxableValue,
        BigDecimal totalCgst,
        BigDecimal totalSgst,
        BigDecimal totalIgst,
        BigDecimal totalCess,
        BigDecimal totalTax,
        BigDecimal roundOff,
        BigDecimal grandTotal,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount,
        String paymentStatus,
        String ageingBucket
) {}
