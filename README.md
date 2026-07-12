

````markdown
# Verbamind

Verbamind is an AI-powered document Q&A (RAG) platform. Users upload documents, and the system answers natural-language questions about them with citations back to the exact source passages.

Built with Spring Boot 3.5 (Java 21), PostgreSQL + pgvector, MinIO, and pluggable AI providers (Ollama, OpenAI, Gemini, Anthropic Claude).

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Environment Variables](#environment-variables)
- [API Overview](#api-overview)
- [Database Migrations](#database-migrations)
- [Known Issues](#known-issues)
- [License](#license)

## Features

- **Authentication** — email/password registration, JWT access + refresh tokens, email verification, password reset
- **Organizations** — personal workspace auto-created on signup, team organizations with Owner / Admin / Member roles, invite-by-email
- **Document management** — upload PDF, DOCX, and TXT files to MinIO; async text extraction (Apache Tika) → chunking → embedding → vector storage
- **AI chat (RAG)** — ask questions and get answers grounded in your documents, with numbered citations back to source chunks
- **Subscriptions & billing** — Free / Pro / Enterprise plans, Razorpay checkout and webhook-based payment verification, admin plan overrides
- **Usage & quotas** — daily and monthly AI request limits, storage limits per plan, enforced before every action
- **Admin console** — manage users and organizations, view payments, and track platform-wide usage analytics

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Security, Data JPA, Validation, Mail) |
| Database | PostgreSQL 16 with the pgvector extension |
| Migrations | Flyway |
| Object storage | MinIO (S3-compatible) |
| Auth | JWT (jjwt) |
| Payments | Razorpay |
| AI providers | Ollama (local/dev), OpenAI, Gemini, Anthropic Claude |
| Cache | Redis |

## Architecture

The codebase is organized by feature module under `com.verbamind`:

```
auth/          registration, login, tokens, email verification, password reset
organization/  workspaces, memberships, invites, roles
document/      upload, storage (MinIO), listing, download
ai/            text extraction, chunking, embeddings, RAG query pipeline
chat/          conversation threads and messages
subscription/  plans and subscription lifecycle
payment/       Razorpay order creation, verification, webhook handling
usage/         quota checks and usage tracking
admin/         admin-only user/org/payment/analytics management
user/          current-user profile and account settings
security/      JWT filter, Spring Security configuration
config/        MinIO, JPA, and web configuration
common/        shared DTOs and base entities
exception/     global exception handling
```

## Prerequisites

- JDK 21
- Docker and Docker Compose
- Maven (or use the bundled `./mvnw`)

## Local Setup

**1. Start infrastructure**

```bash
cd docker
docker compose -f docker-compose.dev.yml up -d
```

**2. Configure `src/main/resources/application-dev.yaml`**

Set your local datasource and Redis connection details:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/documind
    username: documind
    password: documind
  data:
    redis:
      host: localhost
      port: 6379
```

**3. Run the application**

```bash
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080` with the `dev` Spring profile active.

**4. Pull local AI models (Ollama is the dev-default provider)**

```bash
docker exec -it documind-ollama ollama pull nomic-embed-text
docker exec -it documind-ollama ollama pull llama3.2
```

## Environment Variables

For production, provide the following:

```env
# Database
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=
DB_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=6379

# JWT
JWT_SECRET=

# MinIO
MINIO_ENDPOINT=
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=

# AI provider
AI_PROVIDER=openai
OPENAI_API_KEY=
GEMINI_API_KEY=
ANTHROPIC_API_KEY=

# Razorpay
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
```

## API Overview

Base path: `/api`

| Area | Base route |
|---|---|
| Auth | `/api/auth` |
| Organizations | `/api/organizations` |
| Documents | `/api/organizations/{organizationId}/documents` |
| AI / RAG | `/api/organizations/{organizationId}/ai` |
| Chats | `/api/organizations/{organizationId}/chats` |
| Subscriptions | `/api/organizations/{organizationId}/subscription` |
| Payments | `/api/organizations/{organizationId}/payments` |
| Usage | `/api/organizations/{organizationId}/usage` |
| Current user | `/api/users/me` |
| Admin | `/api/admin/**` (requires `ROLE_ADMIN`) |
| Razorpay webhook | `/api/webhooks/razorpay` |

All routes except `/api/auth/**`, `/api/webhooks/**`, and `/actuator/health` require:

```
Authorization: Bearer <access_token>
```

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration` and run automatically on startup, in order from `V1__init.sql` through `V9__usage.sql`.

## Known Issues

The following should be addressed before deploying to production:

- [ ] `docker-compose.dev.yml` uses `postgres:16-alpine`, which lacks the pgvector extension required by `V5__ai.sql`. Switch to `pgvector/pgvector:pg16`.
- [ ] `application-dev.yaml` ships with blank datasource and Redis values; fill in local defaults.
- [ ] `application-prod.yaml` has the `razorpay:` block incorrectly nested under `logging:` instead of under `verbamind:`, so Razorpay properties won't bind at runtime.
- [ ] `docker-compose.prod.yml` is currently empty and needs a production deployment definition.
- [ ] `docker/.env.example` is empty; populate it with the variables listed above.

## License

Add your license here (e.g. MIT, Apache 2.0, or proprietary).
````
