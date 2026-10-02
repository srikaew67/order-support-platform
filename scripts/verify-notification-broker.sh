#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
project="cdg-notification-verify-$$"
export POSTGRES_HOST_PORT="${POSTGRES_HOST_PORT:-55433}"
export POSTGRES_DB=notification_verify
export POSTGRES_USER=notification_verify
export POSTGRES_PASSWORD=notification_verify_password
export RABBITMQ_HOST_PORT="${RABBITMQ_HOST_PORT:-55672}"
export RABBITMQ_MANAGEMENT_PORT="${RABBITMQ_MANAGEMENT_PORT:-55673}"
export RABBITMQ_USERNAME=notification_verify
export RABBITMQ_PASSWORD=notification_verify_password
export JWT_SECRET=notification-verification-secret-at-least-thirty-two-characters
maven_bin="${MVN_BIN:-$root_dir/.tools/apache-maven-3.9.9/bin/mvn}"
java_bin="${JAVA_BIN:-$root_dir/.tools/jdk-21.0.12.1+1/bin/java}"
export JAVA_HOME="${JAVA_HOME:-$root_dir/.tools/jdk-21.0.12.1+1}"
compose=(docker compose -p "$project" -f "$root_dir/docker-compose.yml")
service_pid=''
cleanup() {
  if [[ -n "$service_pid" ]]; then kill "$service_pid" 2>/dev/null || true; wait "$service_pid" 2>/dev/null || true; fi
  "${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

"${compose[@]}" up -d --wait postgres rabbitmq
"$maven_bin" -q -f "$root_dir/backend/notification-service/pom.xml" -DskipTests package
export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:${POSTGRES_HOST_PORT}/${POSTGRES_DB}"
export SPRING_DATASOURCE_USERNAME="$POSTGRES_USER"
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export RABBITMQ_HOST=127.0.0.1
export RABBITMQ_PORT="$RABBITMQ_HOST_PORT"
SERVER_PORT=18083 "$java_bin" -jar "$root_dir/backend/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar" > /tmp/cdg-task6-notification-startup.log 2>&1 &
service_pid=$!
for _ in $(seq 1 70); do
  if curl -fsS http://127.0.0.1:18083/actuator/health >/dev/null 2>&1; then break; fi
  sleep 1
done
curl -fsS http://127.0.0.1:18083/actuator/health >/dev/null

rabbit_api="http://127.0.0.1:${RABBITMQ_MANAGEMENT_PORT}/api"
for _ in $(seq 1 30); do
  if curl -fsS -u "$RABBITMQ_USERNAME:$RABBITMQ_PASSWORD" \
      "$rabbit_api/queues/%2F/notifications.events" >/dev/null 2>&1; then break; fi
  sleep 1
done

event_id='00000000-0000-0000-0000-000000000006'
payload='{"event_id":"'"$event_id"'","event_type":"OrderCreated","version":1,"occurred_at":"2026-10-01T00:00:00Z","order_id":"00000000-0000-0000-0000-000000000004","customer_id":"00000000-0000-0000-0000-000000000005","status":"PENDING","total_amount":12.50,"correlation_id":"broker-check"}'
publish() {
  local exchange="$1"
  local routing_key="$2"
  local body="$3"
  local envelope
  envelope="$(python3 -c 'import json,sys; print(json.dumps({"properties": {}, "routing_key": sys.argv[1], "payload": sys.argv[2], "payload_encoding": "string"}))' "$routing_key" "$body")"
  curl -fsS -u "$RABBITMQ_USERNAME:$RABBITMQ_PASSWORD" -H 'content-type: application/json' \
    -d "$envelope" "$rabbit_api/exchanges/%2F/$exchange/publish" | grep -Fq '"routed":true'
}
notification_count() {
  "${compose[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At \
    -c "SELECT count(*) FROM notification.notifications WHERE event_id='$event_id'"
}
dead_count() {
  curl -fsS -u "$RABBITMQ_USERNAME:$RABBITMQ_PASSWORD" "$rabbit_api/queues/%2F/notifications.dead" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin).get("messages", 0))'
}
publish order.events order.created "$payload"
for _ in $(seq 1 30); do
  if [[ "$(notification_count)" == '1' ]]; then break; fi
  sleep 1
done
[[ "$(notification_count)" == '1' ]]
publish order.events order.created "$payload"
sleep 2
[[ "$(notification_count)" == '1' ]]
ticket_payload='{"eventId":"00000000-0000-0000-0000-000000000007","eventType":"TicketStatusChanged","version":1,"occurredAt":"2026-10-01T00:00:00Z","ticketId":"00000000-0000-0000-0000-000000000008","customerId":"00000000-0000-0000-0000-000000000005","status":"RESOLVED","correlationId":"broker-check-ticket"}'
status_payload='{"event_id":"00000000-0000-0000-0000-000000000009","event_type":"OrderStatusChanged","version":1,"occurred_at":"2026-10-01T00:00:00Z","order_id":"00000000-0000-0000-0000-000000000004","customer_id":"00000000-0000-0000-0000-000000000005","status":"SHIPPED","total_amount":12.50,"correlation_id":"broker-check-status"}'
publish ticket.events ticket.status.changed "$ticket_payload"
publish order.events order.status.changed "$status_payload"
for _ in $(seq 1 30); do
  total="$("${compose[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At \
    -c 'SELECT count(*) FROM notification.notifications')"
  if [[ "$total" == '3' ]]; then break; fi
  sleep 1
done
[[ "$total" == '3' ]]
publish order.events order.created '{bad-json'
for _ in $(seq 1 30); do
  if [[ "$(dead_count)" == '1' ]]; then break; fi
  sleep 1
done
[[ "$(dead_count)" == '1' ]]
printf 'Notification broker verification passed: three event types stored, duplicate ignored, malformed event dead-lettered\n'
