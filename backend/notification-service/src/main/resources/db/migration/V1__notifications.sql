CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    subject_id UUID NOT NULL,
    message VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notifications_customer_created ON notifications(customer_id, created_at DESC, id DESC);
