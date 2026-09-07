-- FastBills initial database foundation migration
-- Enables required PostgreSQL extensions for UUID generation and hashing

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
