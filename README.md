# Order & Support Management Platform

Portfolio project for an order and support platform built with Java 21, Spring Boot, Angular, PostgreSQL, Redis, and RabbitMQ.

## Project layout

- `backend/order-service/` — authentication, catalog, and order API (port 8081)
- `backend/support-service/` — support ticket API (port 8082)
- `backend/notification-service/` — notification API and event consumer (port 8083)
- `frontend/` — strict TypeScript Angular application

## Local prerequisites

- Docker Compose v2
- Java 21 and Maven 3.9+ (for running APIs outside containers)
- Node.js 22+ and npm (for the Angular dashboard)

## Run the local stack

Copy `.env.example` to `.env` if you want to customize local credentials, then start the dependencies and APIs:

```bash
docker compose up --build
```

The services expose health at `/actuator/health`. PostgreSQL is available on `localhost:5432`, Redis on `localhost:6379`, and RabbitMQ on `localhost:5672`; the RabbitMQ management console is at `http://localhost:15672`.

| API | Local address |
| --- | --- |
| Order | `http://localhost:8081` |
| Support | `http://localhost:8082` |
| Notification | `http://localhost:8083` |

Start the dashboard separately with `cd frontend && npm ci && npm start`. Development API URLs are in `src/environments/environment.ts`; production replacements are configured in `angular.json`.

To start only data dependencies, use `docker compose up -d postgres redis rabbitmq`.
