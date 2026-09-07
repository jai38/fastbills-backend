package com.fastbills.api.invoice;

import com.fastbills.api.tax.SupplyType;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "invoice_number", length = 20)
    private String invoiceNumber;

    @Column(name = "financial_year")
    private Integer financialYear;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "place_of_supply_state_code", length = 2, nullable = false)
    private String placeOfSupplyStateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_type", length = 20, nullable = false)
    private SupplyType supplyType;

    @Column(name = "is_reverse_charge", nullable = false)
    private Boolean isReverseCharge = false;

    @Column(name = "delivery_or_acceptance_date")
    private LocalDate deliveryOrAcceptanceDate;

    @Column(name = "agreed_credit_days", nullable = false)
    private Integer agreedCreditDays = 30;

    @Column(name = "collection_reference", length = 50)
    private String collectionReference;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "po_date")
    private LocalDate poDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms_text", columnDefinition = "TEXT")
    private String termsText;

    @Column(name = "subtotal_before_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotalBeforeDiscount = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalDiscount = BigDecimal.ZERO.setScale(2);

    @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableValue = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_cgst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCgst = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_sgst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalSgst = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_igst", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalIgst = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_cess", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCess = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_tax", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalTax = BigDecimal.ZERO.setScale(2);

    @Column(name = "round_off", precision = 15, scale = 2, nullable = false)
    private BigDecimal roundOff = BigDecimal.ZERO.setScale(2);

    @Column(name = "grand_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "amount_in_words", columnDefinition = "TEXT")
    private String amountInWords;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    private InvoiceState state = InvoiceState.DRAFT;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by_user_id")
    private UUID issuedByUserId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "pdf_object_key", length = 500)
    private String pdfObjectKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    public Invoice() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
        line.setOrganisationId(this.organisationId);
    }

    public void removeLine(InvoiceLine line) {
        lines.remove(line);
        line.setInvoice(null);
    }

    public void clearLines() {
        for (InvoiceLine line : new ArrayList<>(lines)) {
            removeLine(line);
        }
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

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(UUID seriesId) {
        this.seriesId = seriesId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Integer getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Integer financialYear) {
        this.financialYear = financialYear;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPlaceOfSupplyStateCode() {
        return placeOfSupplyStateCode;
    }

    public void setPlaceOfSupplyStateCode(String placeOfSupplyStateCode) {
        this.placeOfSupplyStateCode = placeOfSupplyStateCode;
    }

    public SupplyType getSupplyType() {
        return supplyType;
    }

    public void setSupplyType(SupplyType supplyType) {
        this.supplyType = supplyType;
    }

    public Boolean getIsReverseCharge() {
        return isReverseCharge;
    }

    public void setIsReverseCharge(Boolean reverseCharge) {
        isReverseCharge = reverseCharge;
    }

    public LocalDate getDeliveryOrAcceptanceDate() {
        return deliveryOrAcceptanceDate;
    }

    public void setDeliveryOrAcceptanceDate(LocalDate deliveryOrAcceptanceDate) {
        this.deliveryOrAcceptanceDate = deliveryOrAcceptanceDate;
    }

    public Integer getAgreedCreditDays() {
        return agreedCreditDays;
    }

    public void setAgreedCreditDays(Integer agreedCreditDays) {
        this.agreedCreditDays = agreedCreditDays;
    }

    public String getCollectionReference() {
        return collectionReference;
    }

    public void setCollectionReference(String collectionReference) {
        this.collectionReference = collectionReference;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public LocalDate getPoDate() {
        return poDate;
    }

    public void setPoDate(LocalDate poDate) {
        this.poDate = poDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTermsText() {
        return termsText;
    }

    public void setTermsText(String termsText) {
        this.termsText = termsText;
    }

    public BigDecimal getSubtotalBeforeDiscount() {
        return subtotalBeforeDiscount;
    }

    public void setSubtotalBeforeDiscount(BigDecimal subtotalBeforeDiscount) {
        this.subtotalBeforeDiscount = subtotalBeforeDiscount;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
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

    public String getAmountInWords() {
        return amountInWords;
    }

    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }

    public InvoiceState getState() {
        return state;
    }

    public void setState(InvoiceState state) {
        this.state = state;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public UUID getIssuedByUserId() {
        return issuedByUserId;
    }

    public void setIssuedByUserId(UUID issuedByUserId) {
        this.issuedByUserId = issuedByUserId;
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

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getPdfObjectKey() {
        return pdfObjectKey;
    }

    public void setPdfObjectKey(String pdfObjectKey) {
        this.pdfObjectKey = pdfObjectKey;
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

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }
}
