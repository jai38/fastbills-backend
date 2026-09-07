-- FastBills Milestone M5: Invoices, Invoice Lines, and Audit Log

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    series_id UUID REFERENCES number_series(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(20),
    financial_year INTEGER,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    place_of_supply_state_code VARCHAR(2) NOT NULL,
    supply_type VARCHAR(20) NOT NULL,
    is_reverse_charge BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_or_acceptance_date DATE,
    agreed_credit_days INTEGER NOT NULL DEFAULT 30,
    collection_reference VARCHAR(50),
    po_number VARCHAR(100),
    po_date DATE,
    notes TEXT,
    terms_text TEXT,
    subtotal_before_discount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_discount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_value NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_cgst NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_sgst NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_igst NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_cess NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    round_off NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    amount_in_words TEXT,
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMPTZ,
    issued_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMPTZ,
    cancelled_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    cancellation_reason TEXT,
    pdf_object_key VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invoice_state CHECK (state IN ('DRAFT', 'ISSUED', 'CANCELLED')),
    CONSTRAINT chk_invoice_supply_type CHECK (supply_type IN ('INTRA_STATE', 'INTER_STATE', 'EXPORT')),
    CONSTRAINT chk_invoice_credit_days CHECK (agreed_credit_days >= 0)
);

CREATE INDEX idx_invoices_org ON invoices(organisation_id);
CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_state ON invoices(organisation_id, state);
CREATE INDEX idx_invoices_date ON invoices(organisation_id, invoice_date);
CREATE INDEX idx_invoices_due_date ON invoices(organisation_id, due_date);
CREATE UNIQUE INDEX uq_invoices_org_number ON invoices(organisation_id, invoice_number) WHERE invoice_number IS NOT NULL;
CREATE UNIQUE INDEX uq_invoices_org_collection_ref ON invoices(organisation_id, collection_reference) WHERE collection_reference IS NOT NULL;

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    item_description TEXT NOT NULL,
    hsn_or_sac_code VARCHAR(10),
    is_service BOOLEAN NOT NULL DEFAULT FALSE,
    quantity NUMERIC(15,3) NOT NULL DEFAULT 1.000,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'NOS',
    unit_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    taxable_value NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    tax_category VARCHAR(20) NOT NULL DEFAULT 'TAXABLE',
    tax_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    cgst_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    cess_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    cess_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_line_tax_category CHECK (tax_category IN ('TAXABLE', 'NIL_RATED', 'EXEMPT', 'ZERO_RATED', 'NON_GST')),
    CONSTRAINT chk_line_quantity CHECK (quantity > 0),
    CONSTRAINT chk_line_unit_price CHECK (unit_price >= 0),
    CONSTRAINT uq_invoice_lines_num UNIQUE (invoice_id, line_number)
);

CREATE INDEX idx_invoice_lines_org ON invoice_lines(organisation_id);
CREATE INDEX idx_invoice_lines_invoice ON invoice_lines(invoice_id);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    before_json JSONB,
    after_json JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_org ON audit_log(organisation_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);
