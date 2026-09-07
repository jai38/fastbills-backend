package com.fastbills.api.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        ErrorCode code,
        String message,
        Instant timestamp,
        List<FieldErrorDetail> fieldErrors
) {
    public ApiError(ErrorCode code, String message) {
        this(code, message, Instant.now(), null);
    }

    public ApiError(ErrorCode code, String message, List<FieldErrorDetail> fieldErrors) {
        this(code, message, Instant.now(), fieldErrors);
    }

    public record FieldErrorDetail(String field, String message) {}
}
