# Order Support Platform — Working Guide

Use this guide when developing, demonstrating, or troubleshooting the local Order Support Platform.

## Stack

- Frontend: Angular, served by Nginx at `http://localhost:8080`
- APIs: Spring Boot services for orders (`8081`), support (`8082`), and notifications (`8083`)
- Dependencies: PostgreSQL (`5432`), Redis (`6379`), RabbitMQ (`5672`) and RabbitMQ Management (`15672`)
- Delivery tooling: Jenkins, Docker Hub, and kind Kubernetes manifests

## Start locally

Create `.env` from `.env.example`, then set a private JWT key before starting Compose:

```bash
cp .env.example .env
printf 'JWT_SECRET=%s\n' "$(openssl rand -hex 32)" >> .env
docker compose up -d --build
```

Open `http://localhost:8080`. Check readiness with:

```bash
docker compose ps
curl http://localhost:8081/actuator/health
```

Do not commit `.env`; it contains local secrets.

## Local demo data

Demo data is opt-in and intended only for a disposable local environment. Add these settings to `.env` before starting or recreating `order-service`:

```dotenv
DEMO_DATA_ENABLED=true
DEMO_ADMIN_EMAIL=admin@order-support.local
DEMO_ADMIN_PASSWORD=choose-a-private-local-password
```

The order service creates one Admin account and six products with `DEMO-` SKUs. The seeder is idempotent: it does not add duplicate users or products on later starts.

Use the Admin account to demonstrate product management:

| Field | Value |
| --- | --- |
| Email | `admin@order-support.local` |
| Password | The value you set in `.env` |
| Role | `ADMIN` |

Public registration always creates a `CUSTOMER`; it must never be changed to grant staff roles.

## Common commands

```bash
# Follow every service log in real time
docker compose logs -f

# Follow one service
docker compose logs -f order-service

# Recreate Order service after a backend or .env change
docker compose build order-service
docker compose up -d --no-deps --force-recreate order-service

# Stop the local stack without deleting database volumes
docker compose down
```

Compose requires `JWT_SECRET` even for `docker compose logs`. Keep `.env` present, or export a value in the current shell before running Compose.

## Browser/API troubleshooting

- The frontend proxies order API calls through `/api/orders`, support through `/api/support`, and notifications through `/api/notifications`.
- Compose allows both `http://localhost:8080` (Docker frontend) and `http://localhost:4200` (Angular dev server) for all APIs. If registration or sign-in returns `403 Invalid CORS request`, recreate the affected service after checking its environment.
- A successful login redirects to `/dashboard`. Admin capabilities require signing out and signing back in with the demo Admin account.

## Docker resource note

Docker Desktop is configured with limited memory. Running Compose, Jenkins, kind, Prometheus, and Grafana together can starve the Spring services and make login or registration time out.

For a local UI demo, pause nonessential workloads first:

```bash
docker stop order-support-control-plane orderflow-grafana-1 orderflow-prometheus-1
docker stop jenkins-order-support-agent jenkins-order-support
```

These commands stop containers only; they do not delete their data. Start them again when you need CI/CD or Kubernetes:

```bash
docker start jenkins-order-support jenkins-order-support-agent
docker start order-support-control-plane orderflow-prometheus-1 orderflow-grafana-1
```

## Verification

The Order service test suite is run with Maven and Java 21. If Java is unavailable on the host, use the Maven container:

```bash
docker run --rm \
  -v "$(pwd)/backend/order-service:/workspace" \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn test
```

Before claiming a local change works, verify the affected API health endpoint and the browser flow that uses it.
