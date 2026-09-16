package com.fastbills.api.hsn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateHsnItemRequest(
        @NotBlank(message = "HSN/SAC code is required")
        @Pattern(regexp = "^[0-9]{2,8}$", message = "HSN/SAC code must be between 2 and 8 numeric digits")
        String code,

        String name,

        String description,

        @NotNull(message = "Tax rate percent is required")
        BigDecimal taxRatePercent,

        Boolean isService,

        Boolean isDefault
) {}
