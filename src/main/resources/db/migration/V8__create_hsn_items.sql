-- V8__create_hsn_items.sql
-- Table for managing stored HSN/SAC codes, linked GST slabs, descriptions, and default HSN

CREATE TABLE hsn_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tax_rate_percent NUMERIC(5,2) NOT NULL,
    is_service BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_hsn_org_code UNIQUE (organisation_id, code)
);

CREATE INDEX idx_hsn_org_active ON hsn_items(organisation_id, is_active);
CREATE INDEX idx_hsn_org_default ON hsn_items(organisation_id, is_default);
