# API guide

The local frontend at `http://localhost:8080` proxies all API calls. Direct service ports are 8081 (orders), 8082 (support) and 8083 (notifications) unless changed in `.env`. Swagger UI is available on each direct API port at `/swagger-ui/index.html`; generated OpenAPI JSON is at `/v3/api-docs`. The paths below are the direct service paths; through the frontend, prepend `/api/orders`, `/api/support` or `/api/notifications` respectively.

## Authentication and conventions

Public registration creates a `CUSTOMER`. Login and registration return `accessToken`, `role` and `displayName`. Send `Authorization: Bearer <token>` to protected routes. Staff accounts are not created by public registration. A caller can send `X-Correlation-ID: demo-order-1`; the API returns a safe supplied ID or generates one. Collection responses have `content`, `page`, `size`, `totalElements` and `totalPages`. Page numbers start at zero and page size is 1–100.

| Order API | Access | Purpose |
| --- | --- | --- |
| `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | Public | Customer registration and sign-in. |
| `GET /api/v1/auth/me` | Signed in | Current account. |
| `GET /api/v1/products`, `GET /api/v1/products/{id}` | Public | Paginated catalog and detail. |
| `POST /api/v1/products`, `PUT /api/v1/products/{id}`, `DELETE /api/v1/products/{id}` | ADMIN | Create, update with version, and soft-delete products. |
| `POST /api/v1/orders` | CUSTOMER | Reserve stock and create an order. |
| `GET /api/v1/orders`, `GET /api/v1/orders/{id}` | Signed in | Own orders for customers; staff can inspect orders. |
| `POST /api/v1/orders/{id}/cancel` | Owner or ADMIN | Cancel and restore stock once. |
| `PATCH /api/v1/orders/{id}/status` | ADMIN | Advance status. |

| Support and notification APIs | Access | Purpose |
| --- | --- | --- |
| `POST /api/v1/tickets` | CUSTOMER | Create a ticket, optionally linked to an owned order. |
| `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}` | Signed in | Customer-owned or staff-visible tickets. |
| `PATCH /api/v1/tickets/{id}` | SUPPORT or ADMIN | Change status or assign a verified support agent. |
| `POST /api/v1/tickets/{id}/comments` | Authorized ticket viewer | Add a comment. |
| `GET /api/v1/notifications`, `GET /api/v1/notifications/{id}` | Signed in | Own notifications only. |

Status progressions are `PENDING → PROCESSING → SHIPPED → DELIVERED` for orders, with cancellation permitted from `PENDING` or `PROCESSING`; tickets progress `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, and a resolved ticket can reopen to `IN_PROGRESS`. Invalid transitions return `409`.

## Customer walkthrough

Start the Compose stack as described in the [README](../README.md). The normal stack starts with an empty catalog; an administrator must create a product before the order request below. `scripts/verify-e2e.sh` creates a disposable product and runs this flow automatically without needing an admin account.

```bash
BASE=http://localhost:8080
EMAIL="demo-$(date +%s)@example.test"
REGISTER_RESPONSE="$(curl -fsS -X POST "$BASE/api/orders/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Demo-password-123\",\"displayName\":\"Demo Customer\"}")"
TOKEN="$(printf '%s' "$REGISTER_RESPONSE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')"
curl -fsS "$BASE/api/orders/api/v1/products?page=0&size=20"
```

Choose an active product ID from the catalog. The placeholders below are values from preceding responses; replace them before running the calls. The order and ticket bodies match the request DTOs, and `X-Correlation-ID` lets you follow the event into the notification response.

```bash
PRODUCT_ID='REPLACE_WITH_PRODUCT_UUID'
ORDER_RESPONSE="$(curl -fsS -X POST "$BASE/api/orders/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: demo-order-1' \
  -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":1}]}")"
ORDER_ID="$(printf '%s' "$ORDER_RESPONSE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"
curl -fsS -X POST "$BASE/api/support/api/v1/tickets" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"orderId\":\"$ORDER_ID\",\"subject\":\"Delivery question\",\"description\":\"Please check the status.\"}"
curl -fsS "$BASE/api/notifications/api/v1/notifications?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Notification delivery is asynchronous, so a newly created order may take a few seconds to appear. The `OrderCreated` notification carries the order ID as `subjectId` and the request correlation ID. To add a ticket comment, post `{"body":"Thanks for checking"}` to `/api/support/api/v1/tickets/{id}/comments` with the same bearer token. Staff can patch a ticket with `{"status":"IN_PROGRESS","assignToMe":true}`; `ADMIN` can provide a verified `assigneeId`. Product creation by an admin uses `{"sku":"DEMO-1","name":"Demo item","price":12.50,"stockQuantity":5}`.

## Errors and health

Validation failures include field messages. This representative shape uses illustrative values:

```json
{
  "timestamp": "2026-10-03T00:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": { "email": "must be a well-formed email address" },
  "correlationId": "demo-order-1"
}
```

Each API exposes `/actuator/health`; Kubernetes probes use `/actuator/health/liveness` and `/actuator/health/readiness`. See [architecture](architecture.md) for ownership and event behavior, and [the ER diagram](er-diagram.md) for persistence.
