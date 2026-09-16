package com.fastbills.api.hsn.dto;

import com.fastbills.api.hsn.HsnItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HsnItemResponse(
        UUID id,
        UUID organisationId,
        String code,
        String name,
        String description,
        BigDecimal taxRatePercent,
        boolean isService,
        boolean isDefault,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static HsnItemResponse fromEntity(HsnItem item) {
        return new HsnItemResponse(
                item.getId(),
                item.getOrganisationId(),
                item.getCode(),
                item.getName(),
                item.getDescription(),
                item.getTaxRatePercent(),
                Boolean.TRUE.equals(item.getIsService()),
                Boolean.TRUE.equals(item.getIsDefault()),
                Boolean.TRUE.equals(item.getIsActive()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
