#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
project="cdg-schema-verify-$$"
export POSTGRES_HOST_PORT="${POSTGRES_HOST_PORT:-55432}"
export POSTGRES_DB=task5_schema_verify
export POSTGRES_USER=task5_verify
export POSTGRES_PASSWORD=task5_verify_password
export JWT_SECRET=task5-verification-secret-at-least-thirty-two-characters
maven_bin="${MVN_BIN:-$root_dir/.tools/apache-maven-3.9.9/bin/mvn}"
java_bin="${JAVA_BIN:-$root_dir/.tools/jdk-21.0.12.1+1/bin/java}"
export JAVA_HOME="${JAVA_HOME:-$root_dir/.tools/jdk-21.0.12.1+1}"
compose=(docker compose -p "$project" -f "$root_dir/docker-compose.yml")
order_pid=''
support_pid=''
cleanup() {
  if [[ -n "$support_pid" ]]; then kill "$support_pid" 2>/dev/null || true; wait "$support_pid" 2>/dev/null || true; fi
  if [[ -n "$order_pid" ]]; then kill "$order_pid" 2>/dev/null || true; wait "$order_pid" 2>/dev/null || true; fi
  "${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

"${compose[@]}" up -d --wait postgres
"$maven_bin" -q -f "$root_dir/backend/order-service/pom.xml" -DskipTests package
"$maven_bin" -q -f "$root_dir/backend/support-service/pom.xml" -DskipTests package
export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:${POSTGRES_HOST_PORT}/${POSTGRES_DB}"
export SPRING_DATASOURCE_USERNAME="$POSTGRES_USER"
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export RABBITMQ_HOST=127.0.0.1
export REDIS_HOST=127.0.0.1
export ORDER_SERVICE_URL=http://127.0.0.1:18081
SERVER_PORT=18081 "$java_bin" -jar "$root_dir/backend/order-service/target/order-service-0.0.1-SNAPSHOT.jar" > /tmp/cdg-task5-order-startup.log 2>&1 &
order_pid=$!
wait_health() {
  local port="$1"
  for _ in $(seq 1 60); do
    if curl -sS "http://127.0.0.1:${port}/actuator/health" >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  echo "Service on port ${port} did not start" >&2
  return 1
}
wait_health 18081
SERVER_PORT=18082 "$java_bin" -jar "$root_dir/backend/support-service/target/support-service-0.0.1-SNAPSHOT.jar" > /tmp/cdg-task5-support-startup.log 2>&1 &
support_pid=$!
wait_health 18082

query="SELECT
  (SELECT count(*) FROM information_schema.tables WHERE table_schema='support' AND table_name IN ('support_tickets','ticket_comments','ticket_outbox','support_flyway_schema_history')),
  (SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('support_tickets','ticket_comments')),
  (SELECT count(*) FROM public.flyway_schema_history WHERE version='5'),
  (SELECT count(*) FROM support.support_flyway_schema_history WHERE version='2');"
result="$("${compose[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c "$query")"
if [[ "$result" != '4|0|1|1' ]]; then
  echo "Unexpected shared database state: $result" >&2
  exit 1
fi
printf 'Shared PostgreSQL startup and schema verification passed: %s\n' "$result"
