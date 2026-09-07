# FastBills Setup Guide

This guide describes how to get the backend API running locally on Windows, macOS, or Linux in under 10 minutes.

## Prerequisites

- **Java 21 LTS** (`java -version`)
- **Docker & Docker Compose** (`docker compose version`)
- Git

## Step 1: Start Infrastructure Containers

From the `fastbills-backend` directory, start PostgreSQL, MinIO, and MailHog:

```bash
docker compose up -d
```

Verify containers are running healthy:
```bash
docker compose ps
```

- PostgreSQL 16: `localhost:5432` (database: `fastbills`, user: `fastbills_app`, pass: `fastbills_secret`)
- MinIO: `localhost:9000` (console: `http://localhost:9001`, user: `minioadmin`, pass: `minioadmin`)
- MailHog: `localhost:1025` (UI: `http://localhost:8025`)

## Step 2: Configure Environment

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

## Step 3: Build and Run

Run using the included Maven wrapper:

```bash
# On Windows PowerShell
.\mvnw.cmd spring-boot:run

# On macOS/Linux
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3 JSON:** `http://localhost:8080/v3/api-docs`
- **Health check:** `http://localhost:8080/actuator/health`
