-- FastBills Milestone M4: Number Series & Sequence Allocation

CREATE TABLE number_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    document_type VARCHAR(20) NOT NULL,
    financial_year INTEGER NOT NULL,
    prefix VARCHAR(10) NOT NULL,
    next_number INTEGER NOT NULL DEFAULT 1,
    width INTEGER NOT NULL DEFAULT 4,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_document_type CHECK (document_type IN ('INVOICE', 'CREDIT_NOTE')),
    CONSTRAINT chk_next_number CHECK (next_number >= 1),
    CONSTRAINT chk_width CHECK (width >= 3 AND width <= 6),
    CONSTRAINT uq_org_doc_fy UNIQUE (organisation_id, document_type, financial_year)
);

CREATE INDEX idx_number_series_org ON number_series(organisation_id);
