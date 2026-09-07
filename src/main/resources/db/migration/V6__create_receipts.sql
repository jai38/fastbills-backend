-- FastBills Milestone M6: Receipts and Payment Tracking

CREATE TABLE receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE RESTRICT,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    amount NUMERIC(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_reason TEXT,
    reversed_at TIMESTAMPTZ,
    reversed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reversed_by_receipt_id UUID REFERENCES receipts(id) ON DELETE SET NULL,
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_receipt_amount CHECK (amount > 0),
    CONSTRAINT chk_receipt_payment_method CHECK (payment_method IN ('CASH', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'CHEQUE', 'CARD'))
);

CREATE INDEX idx_receipts_org ON receipts(organisation_id);
CREATE INDEX idx_receipts_invoice ON receipts(invoice_id);
CREATE INDEX idx_receipts_customer ON receipts(customer_id);
CREATE INDEX idx_receipts_payment_date ON receipts(organisation_id, payment_date);
CREATE INDEX idx_receipts_invoice_active ON receipts(invoice_id, is_reversed);
