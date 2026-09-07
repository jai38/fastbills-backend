-- FastBills Milestone M1: Organisations, Users, and Refresh Tokens

CREATE TABLE organisations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_code VARCHAR(2),
    pincode VARCHAR(10),
    country VARCHAR(50) NOT NULL DEFAULT 'India',
    phone VARCHAR(20),
    email VARCHAR(255),
    logo_object_key VARCHAR(500),
    invoice_prefix VARCHAR(6) NOT NULL DEFAULT 'INV',
    bank_account_name VARCHAR(255),
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    bank_name VARCHAR(255),
    bank_branch VARCHAR(255),
    upi_id VARCHAR(100),
    default_terms_text TEXT,
    default_credit_days INTEGER NOT NULL DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_org_gstin_state_code CHECK (
        gstin IS NULL OR (length(gstin) = 15 AND substring(gstin from 1 for 2) = state_code)
    ),
    CONSTRAINT chk_org_invoice_prefix CHECK (
        length(invoice_prefix) >= 1 AND length(invoice_prefix) <= 6 AND invoice_prefix ~ '^[A-Za-z0-9/-]+$'
    )
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE RESTRICT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('OWNER', 'STAFF'))
);

CREATE INDEX idx_users_org ON users(organisation_id);
CREATE INDEX idx_users_email ON users(lower(email));

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
