-- FastBills Milestone M9: Credit Notes (Section 34 CGST Act, Rule 53 CGST Rules)

CREATE TABLE credit_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    original_invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    credit_note_number VARCHAR(16) NOT NULL,
    financial_year INTEGER NOT NULL,
    credit_note_date DATE NOT NULL,
    reason VARCHAR(50) NOT NULL,
    reason_notes TEXT,
    taxable_value NUMERIC(15, 2) NOT NULL,
    total_cgst NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_sgst NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_igst NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_cess NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    round_off NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15, 2) NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    cancellation_reason TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by_user_id UUID,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_credit_note_state CHECK (state IN ('ISSUED', 'CANCELLED')),
    CONSTRAINT chk_credit_note_reason CHECK (reason IN (
        'SALES_RETURN',
        'POST_SALE_DISCOUNT',
        'DEFICIENCY_IN_SERVICE',
        'CORRECTION_IN_INVOICE',
        'CHANGE_IN_POS_OR_RATE',
        'OTHER'
    )),
    CONSTRAINT uq_credit_note_number UNIQUE (organisation_id, credit_note_number, financial_year)
);

CREATE TABLE credit_note_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_note_id UUID NOT NULL REFERENCES credit_notes(id) ON DELETE CASCADE,
    original_invoice_line_id UUID REFERENCES invoice_lines(id) ON DELETE SET NULL,
    item_description TEXT NOT NULL,
    hsn_sac_code VARCHAR(10),
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    taxable_value NUMERIC(15, 2) NOT NULL,
    tax_category VARCHAR(20) NOT NULL,
    tax_rate_percent NUMERIC(5, 2) NOT NULL,
    cgst_rate_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_rate_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_rate_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cess_rate_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    cess_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    line_total NUMERIC(15, 2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_cn_tax_category CHECK (tax_category IN ('TAXABLE', 'NIL_RATED', 'EXEMPT', 'ZERO_RATED', 'NON_GST'))
);

CREATE INDEX idx_credit_notes_org_state ON credit_notes(organisation_id, state);
CREATE INDEX idx_credit_notes_invoice ON credit_notes(original_invoice_id);
CREATE INDEX idx_credit_note_lines_cn ON credit_note_lines(credit_note_id);
