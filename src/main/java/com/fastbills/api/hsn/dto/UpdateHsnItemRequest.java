package com.fastbills.api.hsn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateHsnItemRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Tax rate percent is required")
        BigDecimal taxRatePercent,

        Boolean isService,

        Boolean isDefault,

        Boolean isActive
) {}
