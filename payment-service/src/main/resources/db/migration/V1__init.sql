CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount NUMERIC NOT NULL,
    status VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    version BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
