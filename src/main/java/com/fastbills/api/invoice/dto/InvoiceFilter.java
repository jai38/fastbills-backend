package com.fastbills.api.invoice.dto;

import com.fastbills.api.invoice.InvoiceState;

import java.time.LocalDate;
import java.util.UUID;

public record InvoiceFilter(
        InvoiceState state,
        UUID customerId,
        LocalDate fromDate,
        LocalDate toDate,
        String search
) {}
