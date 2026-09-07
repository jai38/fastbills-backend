package com.fastbills.api.receipt;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20, nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed = false;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by_user_id")
    private UUID reversedByUserId;

    @Column(name = "reversed_by_receipt_id")
    private UUID reversedByReceiptId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Receipt() {
    }

    public Receipt(
            UUID organisationId,
            UUID invoiceId,
            UUID customerId,
            BigDecimal amount,
            LocalDate paymentDate,
            PaymentMethod paymentMethod,
            String referenceNumber,
            String notes,
            UUID createdByUserId
    ) {
        this.organisationId = organisationId;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
        this.createdByUserId = createdByUserId;
        this.isReversed = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(UUID organisationId) {
        this.organisationId = organisationId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getIsReversed() {
        return isReversed;
    }

    public void setIsReversed(Boolean reversed) {
        isReversed = reversed;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public void setReversedAt(Instant reversedAt) {
        this.reversedAt = reversedAt;
    }

    public UUID getReversedByUserId() {
        return reversedByUserId;
    }

    public void setReversedByUserId(UUID reversedByUserId) {
        this.reversedByUserId = reversedByUserId;
    }

    public UUID getReversedByReceiptId() {
        return reversedByReceiptId;
    }

    public void setReversedByReceiptId(UUID reversedByReceiptId) {
        this.reversedByReceiptId = reversedByReceiptId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
