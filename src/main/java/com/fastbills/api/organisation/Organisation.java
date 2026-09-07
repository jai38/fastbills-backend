package com.fastbills.api.organisation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisations")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "country", nullable = false, length = 50)
    private String country = "India";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "logo_object_key", length = 500)
    private String logoObjectKey;

    @Column(name = "invoice_prefix", nullable = false, length = 6)
    private String invoicePrefix = "INV";

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "default_terms_text", columnDefinition = "TEXT")
    private String defaultTermsText;

    @Column(name = "default_credit_days", nullable = false)
    private Integer defaultCreditDays = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Organisation() {}

    public Organisation(String legalName) {
        this.legalName = legalName;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogoObjectKey() { return logoObjectKey; }
    public void setLogoObjectKey(String logoObjectKey) { this.logoObjectKey = logoObjectKey; }

    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getDefaultTermsText() { return defaultTermsText; }
    public void setDefaultTermsText(String defaultTermsText) { this.defaultTermsText = defaultTermsText; }

    public Integer getDefaultCreditDays() { return defaultCreditDays; }
    public void setDefaultCreditDays(Integer defaultCreditDays) { this.defaultCreditDays = defaultCreditDays; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
