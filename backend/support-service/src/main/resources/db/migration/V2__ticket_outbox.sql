CREATE TABLE ticket_outbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ticket_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_ticket_outbox_pending ON ticket_outbox(published_at, occurred_at);
