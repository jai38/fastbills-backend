package com.fastbills.api.series;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "number_series",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_org_doc_fy",
                        columnNames = {"organisation_id", "document_type", "financial_year"}
                )
        }
)
public class NumberSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "financial_year", nullable = false)
    private Integer financialYear;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "next_number", nullable = false)
    private Integer nextNumber = 1;

    @Column(name = "width", nullable = false)
    private Integer width = 4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public NumberSeries() {}

    public NumberSeries(
            UUID organisationId,
            DocumentType documentType,
            Integer financialYear,
            String prefix,
            Integer nextNumber,
            Integer width
    ) {
        this.organisationId = organisationId;
        this.documentType = documentType;
        this.financialYear = financialYear;
        this.prefix = prefix;
        this.nextNumber = nextNumber != null ? nextNumber : 1;
        this.width = width != null ? width : 4;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Integer getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Integer financialYear) {
        this.financialYear = financialYear;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Integer getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(Integer nextNumber) {
        this.nextNumber = nextNumber;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
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
}
