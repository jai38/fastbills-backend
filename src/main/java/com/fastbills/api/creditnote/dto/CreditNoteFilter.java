package com.fastbills.api.creditnote.dto;

import com.fastbills.api.creditnote.CreditNoteState;

import java.time.LocalDate;
import java.util.UUID;

public record CreditNoteFilter(
        CreditNoteState state,
        UUID originalInvoiceId,
        UUID customerId,
        LocalDate fromDate,
        LocalDate toDate,
        String search
) {
}
