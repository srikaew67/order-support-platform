# Order & Support Management Platform

Portfolio project for a Java Spring Boot back end, Angular dashboard, Jenkins CI/CD, and Kubernetes deployment.

## Local prerequisites

- Java 21
- Maven 3.9+
- Node.js 22+
- Docker Desktop or Docker Engine (for PostgreSQL)

## Start PostgreSQL

```bash
docker compose up -d postgres
```

The API configuration accepts `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; see `.env.example`.
