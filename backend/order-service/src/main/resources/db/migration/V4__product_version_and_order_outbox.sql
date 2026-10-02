ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE order_outbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_order_outbox_pending ON order_outbox(published_at, occurred_at);
