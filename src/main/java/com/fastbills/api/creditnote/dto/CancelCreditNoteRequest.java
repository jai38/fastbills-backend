package com.fastbills.api.creditnote.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelCreditNoteRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {
}
