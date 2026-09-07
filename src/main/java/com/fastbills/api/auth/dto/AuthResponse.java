package com.fastbills.api.auth.dto;

import com.fastbills.api.user.UserRole;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user,
        OrganisationDto organisation
) {
    public record UserDto(
            UUID id,
            String email,
            String fullName,
            UserRole role
    ) {}

    public record OrganisationDto(
            UUID id,
            String legalName,
            String tradeName,
            String gstin,
            String stateCode
    ) {}
}
