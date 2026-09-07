package com.fastbills.api.customer.dto;

import com.fastbills.api.customer.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID organisationId,
        String legalName,
        String tradeName,
        String gstin,
        String pan,
        Boolean isRegistered,
        String billingAddressLine1,
        String billingAddressLine2,
        String billingCity,
        String billingStateCode,
        String billingPincode,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingStateCode,
        String shippingPincode,
        String contactPerson,
        String phone,
        String email,
        String defaultPlaceOfSupplyStateCode,
        Integer creditDays,
        String notes,
        Boolean isArchived,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse fromEntity(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getOrganisationId(),
                c.getLegalName(),
                c.getTradeName(),
                c.getGstin(),
                c.getPan(),
                c.getIsRegistered(),
                c.getBillingAddressLine1(),
                c.getBillingAddressLine2(),
                c.getBillingCity(),
                c.getBillingStateCode(),
                c.getBillingPincode(),
                c.getShippingAddressLine1(),
                c.getShippingAddressLine2(),
                c.getShippingCity(),
                c.getShippingStateCode(),
                c.getShippingPincode(),
                c.getContactPerson(),
                c.getPhone(),
                c.getEmail(),
                c.getDefaultPlaceOfSupplyStateCode(),
                c.getCreditDays(),
                c.getNotes(),
                c.getIsArchived(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
