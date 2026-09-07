package com.fastbills.api.creditnote;

import com.fastbills.api.invoice.Invoice;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "credit_notes")
public class CreditNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_invoice_id", nullable = false)
    private Invoice originalInvoice;

    @Column(name = "credit_note_number", length = 16, nullable = false)
    private String creditNoteNumber;

    @Column(name = "financial_year", nullable = false)
    private Integer financialYear;

    @Column(name = "credit_note_date", nullable = false)
    private LocalDate creditNoteDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private CreditNoteReason reason;

    @Column(name = "reason_notes", columnDefinition = "TEXT")
    private String reasonNotes;

    @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "total_cgst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(name = "total_sgst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(name = "total_igst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Column(name = "total_cess", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCess = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "round_off", precision = 5, scale = 2, nullable = false)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    private CreditNoteState state = CreditNoteState.ISSUED;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CreditNoteLine> lines = new ArrayList<>();

    public CreditNote() {}

    public void addLine(CreditNoteLine line) {
        lines.add(line);
        line.setCreditNote(this);
    }

    public void removeLine(CreditNoteLine line) {
        lines.remove(line);
        line.setCreditNote(null);
    }

    // Getters and Setters

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

    public Invoice getOriginalInvoice() {
        return originalInvoice;
    }

    public void setOriginalInvoice(Invoice originalInvoice) {
        this.originalInvoice = originalInvoice;
    }

    public String getCreditNoteNumber() {
        return creditNoteNumber;
    }

    public void setCreditNoteNumber(String creditNoteNumber) {
        this.creditNoteNumber = creditNoteNumber;
    }

    public Integer getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Integer financialYear) {
        this.financialYear = financialYear;
    }

    public LocalDate getCreditNoteDate() {
        return creditNoteDate;
    }

    public void setCreditNoteDate(LocalDate creditNoteDate) {
        this.creditNoteDate = creditNoteDate;
    }

    public CreditNoteReason getReason() {
        return reason;
    }

    public void setReason(CreditNoteReason reason) {
        this.reason = reason;
    }

    public String getReasonNotes() {
        return reasonNotes;
    }

    public void setReasonNotes(String reasonNotes) {
        this.reasonNotes = reasonNotes;
    }

    public BigDecimal getTaxableValue() {
        return taxableValue;
    }

    public void setTaxableValue(BigDecimal taxableValue) {
        this.taxableValue = taxableValue;
    }

    public BigDecimal getTotalCgst() {
        return totalCgst;
    }

    public void setTotalCgst(BigDecimal totalCgst) {
        this.totalCgst = totalCgst;
    }

    public BigDecimal getTotalSgst() {
        return totalSgst;
    }

    public void setTotalSgst(BigDecimal totalSgst) {
        this.totalSgst = totalSgst;
    }

    public BigDecimal getTotalIgst() {
        return totalIgst;
    }

    public void setTotalIgst(BigDecimal totalIgst) {
        this.totalIgst = totalIgst;
    }

    public BigDecimal getTotalCess() {
        return totalCess;
    }

    public void setTotalCess(BigDecimal totalCess) {
        this.totalCess = totalCess;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getRoundOff() {
        return roundOff;
    }

    public void setRoundOff(BigDecimal roundOff) {
        this.roundOff = roundOff;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public CreditNoteState getState() {
        return state;
    }

    public void setState(CreditNoteState state) {
        this.state = state;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancelledByUserId() {
        return cancelledByUserId;
    }

    public void setCancelledByUserId(UUID cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CreditNoteLine> getLines() {
        return lines;
    }

    public void setLines(List<CreditNoteLine> lines) {
        this.lines = lines;
    }
}
