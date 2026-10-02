CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    order_id UUID,
    subject VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assignee_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_support_tickets_customer_status ON support_tickets(customer_id, status);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES support_tickets(id),
    author_id UUID NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ticket_comments_ticket_created ON ticket_comments(ticket_id, created_at);
