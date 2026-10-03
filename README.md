# Order & Support Management Platform

Portfolio project for an order and support platform built with Java 21, Spring Boot, Angular, PostgreSQL, Redis, and RabbitMQ.

## Project layout

- `backend/order-service/` — authentication, catalog, and order API (port 8081)
- `backend/support-service/` — support ticket API (port 8082)
- `backend/notification-service/` — notification API and event consumer (port 8083)
- `frontend/` — strict TypeScript Angular application

## Local prerequisites

- Docker Compose v2
- OpenSSL for generating a private local JWT key; Python 3 for the smoke test
- Java 21 and Maven 3.9+ (for running APIs outside containers)
- Node.js 22+ and npm (for the Angular dashboard)

## Run the local stack

Create a private `.env` file before starting the stack. The JWT secret is required and must be shared by all three services:

```bash
umask 077
cp .env.example .env
printf 'JWT_SECRET=%s\n' "$(openssl rand -hex 32)" >> .env
```

Customize local database credentials or host ports in `.env` if needed, then build and start the complete stack:

```bash
docker compose up -d --build --wait
```

Open the Angular dashboard at `http://localhost:8080`. Nginx serves it and proxies API calls to the three services. The services expose health at `/actuator/health`. PostgreSQL is available on `localhost:5432`, Redis on `localhost:6379`, and RabbitMQ on `localhost:5672`; the RabbitMQ management console is at `http://localhost:15672`. All host ports can be changed in `.env`.

| API | Local address |
| --- | --- |
| Order | `http://localhost:8081` |
| Support | `http://localhost:8082` |
| Notification | `http://localhost:8083` |

For frontend development outside containers, run `cd frontend && npm ci && npm start`. Development API URLs are in `src/environments/environment.ts`; the container build uses the production API prefixes through Nginx.

To start only data dependencies, use `docker compose up -d postgres redis rabbitmq`.

## End-to-end verification

Run `scripts/verify-e2e.sh` with Docker Compose and Python 3 available. It generates a fresh private JWT secret for the run, builds the four images, starts a separate temporary Compose project with its own database and host ports, seeds one disposable product, then checks the Angular route, registration, login, product listing, order creation, ticket creation, and the resulting customer notification through the frontend proxy. It removes its containers and database volume afterward. Set `KEEP_SMOKE_STACK=1` to retain a run for inspection. To verify other database settings without changing the working tree, set `SMOKE_ENV_FILE` to an absolute path to a custom Compose env file.

The smoke test seeds a product through PostgreSQL because public registration only grants the CUSTOMER role and product creation requires ADMIN. The normal stack does not seed users or catalog data.

If startup fails, inspect `docker compose ps` and `docker compose logs <service>`. The API containers use their `/actuator/health` endpoints for health checks; Compose waits for PostgreSQL, Redis, and RabbitMQ before starting dependent services. If a host port is occupied, change the corresponding `*_HOST_PORT` value in `.env`.

## CI/CD

The Jenkins Multibranch Pipeline is in `infra/jenkins/Jenkinsfile`. Agent requirements, credential IDs, parameters, image tags, and the optional Kubernetes deployment contract are documented in `infra/jenkins/README.md`.
