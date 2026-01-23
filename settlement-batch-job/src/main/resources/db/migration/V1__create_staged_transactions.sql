-- V1__create_staged_transactions.sql
-- Flyway migration: Create staging table for transactions

CREATE TABLE IF NOT EXISTS staged_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    payer_id VARCHAR(64) NOT NULL,
    payee_id VARCHAR(64) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    issuer_bank_id VARCHAR(32) NOT NULL,
    acquirer_bank_id VARCHAR(32) NOT NULL,
    merchant_category_code VARCHAR(4),
    origin_country VARCHAR(2),
    authorized_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    batch_id VARCHAR(50),
    processed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes for batch processing performance
CREATE INDEX idx_staged_status ON staged_transactions(processing_status);
CREATE INDEX idx_staged_issuer ON staged_transactions(issuer_bank_id);
CREATE INDEX idx_staged_created ON staged_transactions(created_at);
