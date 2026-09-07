package com.fastbills.api.creditnote;

import com.fastbills.api.tax.TaxCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "credit_note_lines")
public class CreditNoteLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @Column(name = "original_invoice_line_id")
    private UUID originalInvoiceLineId;

    @Column(name = "item_description", nullable = false, columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "hsn_sac_code", length = 10)
    private String hsnSacCode;

    @Column(name = "quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", length = 20, nullable = false)
    private TaxCategory taxCategory;

    @Column(name = "tax_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRatePercent;

    @Column(name = "cgst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal cgstRatePercent = BigDecimal.ZERO;

    @Column(name = "cgst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal sgstRatePercent = BigDecimal.ZERO;

    @Column(name = "sgst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal igstRatePercent = BigDecimal.ZERO;

    @Column(name = "igst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "cess_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal cessRatePercent = BigDecimal.ZERO;

    @Column(name = "cess_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "total_tax_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalTaxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public CreditNoteLine() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CreditNote getCreditNote() {
        return creditNote;
    }

    public void setCreditNote(CreditNote creditNote) {
        this.creditNote = creditNote;
    }

    public UUID getOriginalInvoiceLineId() {
        return originalInvoiceLineId;
    }

    public void setOriginalInvoiceLineId(UUID originalInvoiceLineId) {
        this.originalInvoiceLineId = originalInvoiceLineId;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getHsnSacCode() {
        return hsnSacCode;
    }

    public void setHsnSacCode(String hsnSacCode) {
        this.hsnSacCode = hsnSacCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxableValue() {
        return taxableValue;
    }

    public void setTaxableValue(BigDecimal taxableValue) {
        this.taxableValue = taxableValue;
    }

    public TaxCategory getTaxCategory() {
        return taxCategory;
    }

    public void setTaxCategory(TaxCategory taxCategory) {
        this.taxCategory = taxCategory;
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(BigDecimal taxRatePercent) {
        this.taxRatePercent = taxRatePercent;
    }

    public BigDecimal getCgstRatePercent() {
        return cgstRatePercent;
    }

    public void setCgstRatePercent(BigDecimal cgstRatePercent) {
        this.cgstRatePercent = cgstRatePercent;
    }

    public BigDecimal getCgstAmount() {
        return cgstAmount;
    }

    public void setCgstAmount(BigDecimal cgstAmount) {
        this.cgstAmount = cgstAmount;
    }

    public BigDecimal getSgstRatePercent() {
        return sgstRatePercent;
    }

    public void setSgstRatePercent(BigDecimal sgstRatePercent) {
        this.sgstRatePercent = sgstRatePercent;
    }

    public BigDecimal getSgstAmount() {
        return sgstAmount;
    }

    public void setSgstAmount(BigDecimal sgstAmount) {
        this.sgstAmount = sgstAmount;
    }

    public BigDecimal getIgstRatePercent() {
        return igstRatePercent;
    }

    public void setIgstRatePercent(BigDecimal igstRatePercent) {
        this.igstRatePercent = igstRatePercent;
    }

    public BigDecimal getIgstAmount() {
        return igstAmount;
    }

    public void setIgstAmount(BigDecimal igstAmount) {
        this.igstAmount = igstAmount;
    }

    public BigDecimal getCessRatePercent() {
        return cessRatePercent;
    }

    public void setCessRatePercent(BigDecimal cessRatePercent) {
        this.cessRatePercent = cessRatePercent;
    }

    public BigDecimal getCessAmount() {
        return cessAmount;
    }

    public void setCessAmount(BigDecimal cessAmount) {
        this.cessAmount = cessAmount;
    }

    public BigDecimal getTotalTaxAmount() {
        return totalTaxAmount;
    }

    public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
        this.totalTaxAmount = totalTaxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
