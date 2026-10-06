# Data model and ownership

PostgreSQL runs as one local instance. Flyway migrations create `order-service` tables in `public`, ticket tables in `support`, and notifications in `notification`. UUID columns used across services are references by value: a service checks them through an API or event, and no cross-service foreign key or join is used. The early `public.support_tickets` and `public.ticket_comments` tables are legacy scaffolding; a guarded migration removes them only when empty and unreferenced. See [the upgrade guide](legacy-ticket-upgrade.md) for nonempty legacy data.

```mermaid
erDiagram
  USERS ||--o{ ORDERS : places
  ORDERS ||--|{ ORDER_ITEMS : contains
  PRODUCTS ||--o{ ORDER_ITEMS : reserved-in
  ORDERS ||--o{ ORDER_OUTBOX : emits
  SUPPORT_TICKETS ||--o{ TICKET_COMMENTS : contains
  SUPPORT_TICKETS ||--o{ TICKET_OUTBOX : emits

  USERS {
    uuid id PK
    string email UK
    string password_hash
    string role
    timestamp created_at
  }
  PRODUCTS {
    uuid id PK
    string sku UK
    decimal price
    int stock_quantity
    bigint version
    boolean active
  }
  ORDERS {
    uuid id PK
    string order_number UK
    uuid customer_id FK
    string status
    decimal total_amount
  }
  ORDER_ITEMS {
    uuid id PK
    uuid order_id FK
    uuid product_id FK
    int quantity
    decimal unit_price
    decimal subtotal
  }
  ORDER_OUTBOX {
    uuid event_id PK
    uuid order_id
    string event_type
    int event_version
    string correlation_id
    timestamp published_at
  }
  SUPPORT_TICKETS {
    uuid id PK
    uuid customer_id
    uuid order_id
    uuid assignee_id
    string status
  }
  TICKET_COMMENTS {
    uuid id PK
    uuid ticket_id FK
    uuid author_id
    string body
  }
  TICKET_OUTBOX {
    uuid event_id PK
    uuid ticket_id
    string event_type
    int event_version
    string correlation_id
    timestamp published_at
  }
  NOTIFICATIONS {
    uuid id PK
    uuid event_id UK
    uuid customer_id
    uuid subject_id
    string event_type
    string correlation_id
  }
```

`SUPPORT_TICKETS.customer_id`, `order_id` and `assignee_id`, `TICKET_COMMENTS.author_id`, and notification IDs are not SQL foreign keys into another service's schema. The order API validates an order reference for a customer; the support API validates staff identity. The notification consumer trusts only validated versioned events and stores one row per `event_id`.

Key indexes include unique email, SKU, order number and notification event ID; customer/status order and ticket indexes; product active/name lookup; and customer/creation-time notification lookup. Order items have a unique `(order_id, product_id)` constraint. Product stock has a nonnegative check and optimistic version column. The SQL source is under each service's `src/main/resources/db/migration/` directory.
