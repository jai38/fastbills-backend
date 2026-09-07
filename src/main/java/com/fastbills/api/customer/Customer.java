package com.fastbills.api.customer;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "is_registered", nullable = false)
    private Boolean isRegistered = false;

    @Column(name = "billing_address_line1", columnDefinition = "TEXT")
    private String billingAddressLine1;

    @Column(name = "billing_address_line2", columnDefinition = "TEXT")
    private String billingAddressLine2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state_code", nullable = false, length = 2)
    private String billingStateCode;

    @Column(name = "billing_pincode", length = 6)
    private String billingPincode;

    @Column(name = "shipping_address_line1", columnDefinition = "TEXT")
    private String shippingAddressLine1;

    @Column(name = "shipping_address_line2", columnDefinition = "TEXT")
    private String shippingAddressLine2;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_state_code", length = 2)
    private String shippingStateCode;

    @Column(name = "shipping_pincode", length = 6)
    private String shippingPincode;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "default_place_of_supply_state_code", length = 2)
    private String defaultPlaceOfSupplyStateCode;

    @Column(name = "credit_days", nullable = false)
    private Integer creditDays = 30;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Customer() {}

    public Customer(UUID organisationId, String legalName, String billingStateCode) {
        this.organisationId = organisationId;
        this.legalName = legalName;
        this.billingStateCode = billingStateCode;
        this.defaultPlaceOfSupplyStateCode = billingStateCode;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrganisationId() { return organisationId; }
    public void setOrganisationId(UUID organisationId) { this.organisationId = organisationId; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public Boolean getIsRegistered() { return isRegistered; }
    public void setIsRegistered(Boolean isRegistered) { this.isRegistered = isRegistered; }

    public String getBillingAddressLine1() { return billingAddressLine1; }
    public void setBillingAddressLine1(String billingAddressLine1) { this.billingAddressLine1 = billingAddressLine1; }

    public String getBillingAddressLine2() { return billingAddressLine2; }
    public void setBillingAddressLine2(String billingAddressLine2) { this.billingAddressLine2 = billingAddressLine2; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingStateCode() { return billingStateCode; }
    public void setBillingStateCode(String billingStateCode) { this.billingStateCode = billingStateCode; }

    public String getBillingPincode() { return billingPincode; }
    public void setBillingPincode(String billingPincode) { this.billingPincode = billingPincode; }

    public String getShippingAddressLine1() { return shippingAddressLine1; }
    public void setShippingAddressLine1(String shippingAddressLine1) { this.shippingAddressLine1 = shippingAddressLine1; }

    public String getShippingAddressLine2() { return shippingAddressLine2; }
    public void setShippingAddressLine2(String shippingAddressLine2) { this.shippingAddressLine2 = shippingAddressLine2; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingStateCode() { return shippingStateCode; }
    public void setShippingStateCode(String shippingStateCode) { this.shippingStateCode = shippingStateCode; }

    public String getShippingPincode() { return shippingPincode; }
    public void setShippingPincode(String shippingPincode) { this.shippingPincode = shippingPincode; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDefaultPlaceOfSupplyStateCode() { return defaultPlaceOfSupplyStateCode; }
    public void setDefaultPlaceOfSupplyStateCode(String defaultPlaceOfSupplyStateCode) { this.defaultPlaceOfSupplyStateCode = defaultPlaceOfSupplyStateCode; }

    public Integer getCreditDays() { return creditDays; }
    public void setCreditDays(Integer creditDays) { this.creditDays = creditDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
