# Task 1 report — Scaffold services and local dependencies

## Files changed

- Relocated the existing backend project and all auth/catalog files and tests to `backend/order-service/`; preserved their contents and configured the order-service name, datasource settings, and JWT secret.
- Added standalone `backend/support-service/` and `backend/notification-service/` Spring Boot actuator applications, Maven builds, and Dockerfiles.
- Added a Dockerfile for the order service.
- Expanded root `docker-compose.yml` with PostgreSQL, Redis, RabbitMQ, and all three APIs, including dependency health checks and API health probes.
- Added Angular development/production API environment files and production file replacement configuration.
- Updated `.env.example`, `.gitignore`, and README local setup instructions.

## Verification

- Order service tests: passed (`./backend/mvnw -q -f backend/order-service/pom.xml test`, Java 21).
- Support service tests: passed (`./backend/mvnw -q -f backend/support-service/pom.xml test`, Java 21).
- Notification service tests: passed (`./backend/mvnw -q -f backend/notification-service/pom.xml test`, Java 21).
- Angular production build: passed (`npm ci --ignore-scripts --no-audit --no-fund && npm run build`).
- `docker compose config --quiet`: passed.
- `docker compose up --build -d`: all six containers started; PostgreSQL, Redis, RabbitMQ and all three service health checks reached healthy. `curl` against `/actuator/health` on ports 8081, 8082, and 8083 returned `status: UP`.
- `git diff --cached --check`: passed.

## Concerns

- Initial startup validation found mismatches between the legacy order-service configuration and Compose environment variables. The configuration was aligned and the full stack was rebuilt and verified healthy.
- The support and notification services are health-checkable scaffolds only; their functional modules are assigned to later tasks.

## Commit

Pending at report creation; see commit hash in task completion response.

## Fix round 1 — datasource configuration hierarchy

- Moved `datasource`, `jpa`, and `flyway` under the top-level `spring` key; left only `jwt` under `security`.
- Preserved `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as primary environment inputs with `SPRING_DATASOURCE_*` fallbacks.
- Verification: `./backend/mvnw -q -f backend/order-service/pom.xml test` passed on Java 21. A Compose PostgreSQL + order-service startup reached healthy; `http://localhost:8081/actuator/health` returned `{"status":"UP","groups":["liveness","readiness"]}`.
- Compose stack stopped after verification.
- Fix commit: pending.
