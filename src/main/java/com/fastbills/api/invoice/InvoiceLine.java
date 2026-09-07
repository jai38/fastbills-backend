package com.fastbills.api.invoice;

import com.fastbills.api.tax.TaxCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "invoice_lines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invoice_lines_num", columnNames = {"invoice_id", "line_number"})
        }
)
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "item_description", nullable = false, columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "hsn_or_sac_code", length = 10)
    private String hsnOrSacCode;

    @Column(name = "is_service", nullable = false)
    private Boolean isService = false;

    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE.setScale(3);

    @Column(name = "unit_of_measure", length = 20, nullable = false)
    private String unitOfMeasure = "NOS";

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO.setScale(2);

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "discount_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableValue = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", length = 20, nullable = false)
    private TaxCategory taxCategory = TaxCategory.TAXABLE;

    @Column(name = "tax_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRatePercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "cgst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal cgstRatePercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "cgst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "sgst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal sgstRatePercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "sgst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "igst_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal igstRatePercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "igst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "cess_rate_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal cessRatePercent = BigDecimal.ZERO.setScale(2);

    @Column(name = "cess_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal cessAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "line_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InvoiceLine() {}

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

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public BigDecimal getGrossAmount() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return quantity.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        // Derived from quantity * unit_price
    }

    public String getHsnOrSacCode() {
        return hsnOrSacCode;
    }

    public void setHsnOrSacCode(String hsnOrSacCode) {
        this.hsnOrSacCode = hsnOrSacCode;
    }

    public Boolean getIsService() {
        return isService;
    }

    public void setIsService(Boolean service) {
        isService = service;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
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

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
