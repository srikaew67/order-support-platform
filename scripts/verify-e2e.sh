#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"
project="cdg-e2e-$$"

for command in docker python3; do
  command -v "$command" >/dev/null || { echo "Missing required command: $command" >&2; exit 1; }
done

# Each run uses its own containers, volume, and host ports.
read -r FRONTEND_HOST_PORT ORDER_HOST_PORT SUPPORT_HOST_PORT NOTIFICATION_HOST_PORT \
  POSTGRES_HOST_PORT REDIS_HOST_PORT RABBITMQ_HOST_PORT RABBITMQ_MANAGEMENT_PORT < <(
  python3 - <<'PY'
import socket
sockets = []
try:
    for _ in range(8):
        sock = socket.socket()
        sock.bind(("127.0.0.1", 0))
        sockets.append(sock)
    print(*(sock.getsockname()[1] for sock in sockets))
finally:
    for sock in sockets:
        sock.close()
PY
)
export FRONTEND_HOST_PORT ORDER_HOST_PORT SUPPORT_HOST_PORT NOTIFICATION_HOST_PORT
export POSTGRES_HOST_PORT REDIS_HOST_PORT RABBITMQ_HOST_PORT RABBITMQ_MANAGEMENT_PORT
export IMAGE_TAG="smoke-$$"

compose() { docker compose --project-name "$project" "$@"; }
cleanup() {
  result=$?
  if (( result != 0 )); then
    compose ps >&2 || true
    compose logs --tail=80 order-service support-service notification-service frontend >&2 || true
  fi
  if [[ "${KEEP_SMOKE_STACK:-0}" != 1 ]]; then
    compose down -v --remove-orphans >/dev/null 2>&1 || true
    docker image rm \
      "order-support/frontend:$IMAGE_TAG" \
      "order-support/order-service:$IMAGE_TAG" \
      "order-support/support-service:$IMAGE_TAG" \
      "order-support/notification-service:$IMAGE_TAG" >/dev/null 2>&1 || true
  else
    echo "Kept Compose project $project for inspection" >&2
  fi
}
trap cleanup EXIT

compose up -d --build --wait --wait-timeout 300

# Registration always creates a CUSTOMER. Seed one catalog row only in this disposable database.
product_id="$(python3 -c 'import uuid; print(uuid.uuid4())')"
compose exec -T postgres psql -v ON_ERROR_STOP=1 \
  -U "${POSTGRES_USER:-order_support}" -d "${POSTGRES_DB:-order_support}" \
  -c "INSERT INTO products (id, sku, name, description, price, stock_quantity, active, created_at, updated_at, version) VALUES ('$product_id', 'SMOKE-$$', 'Smoke product', 'Disposable verification item', 12.50, 5, TRUE, now(), now(), 0)" >/dev/null

python3 scripts/smoke-e2e.py --base-url "http://127.0.0.1:$FRONTEND_HOST_PORT" --product-id "$product_id"
echo "End-to-end Compose smoke test passed"
