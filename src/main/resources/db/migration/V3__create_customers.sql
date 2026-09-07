-- FastBills Milestone M2: Customers Table

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE RESTRICT,
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    is_registered BOOLEAN NOT NULL DEFAULT FALSE,
    billing_address_line1 TEXT,
    billing_address_line2 TEXT,
    billing_city VARCHAR(100),
    billing_state_code VARCHAR(2) NOT NULL,
    billing_pincode VARCHAR(6),
    shipping_address_line1 TEXT,
    shipping_address_line2 TEXT,
    shipping_city VARCHAR(100),
    shipping_state_code VARCHAR(2),
    shipping_pincode VARCHAR(6),
    contact_person VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    default_place_of_supply_state_code VARCHAR(2),
    credit_days INTEGER NOT NULL DEFAULT 30,
    notes TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customers_gstin_state CHECK (
        gstin IS NULL OR (length(gstin) = 15 AND substring(gstin from 1 for 2) = billing_state_code)
    ),
    CONSTRAINT chk_customers_registered CHECK (
        is_registered = FALSE OR (gstin IS NOT NULL AND length(gstin) = 15)
    ),
    CONSTRAINT chk_customers_billing_state CHECK (
        billing_state_code ~ '^[0-9]{2}$'
    ),
    CONSTRAINT chk_customers_shipping_state CHECK (
        shipping_state_code IS NULL OR shipping_state_code ~ '^[0-9]{2}$'
    ),
    CONSTRAINT chk_customers_pos_state CHECK (
        default_place_of_supply_state_code IS NULL OR default_place_of_supply_state_code ~ '^[0-9]{2}$'
    ),
    CONSTRAINT chk_customers_credit_days CHECK (
        credit_days >= 0
    )
);

CREATE UNIQUE INDEX uq_customers_org_legal_name ON customers(organisation_id, lower(legal_name));
CREATE INDEX idx_customers_org_archived ON customers(organisation_id, is_archived);
CREATE INDEX idx_customers_org_trade_name ON customers(organisation_id, lower(trade_name));
CREATE INDEX idx_customers_org_gstin ON customers(organisation_id, gstin);
