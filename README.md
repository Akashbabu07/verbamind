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
docker exec -it documind-ollama ollama pull llama3.2:3b
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

## Deployment

**Build and run with Docker:**

```bash
cp .env.example .env   # fill in real values
cd docker
docker compose -f docker-compose.prod.yml --env-file ../.env up -d --build
```

This builds the app image from the root `Dockerfile` (multi-stage Maven build → JRE runtime)
and starts it alongside Postgres (with pgvector), Redis, and MinIO. The app's `/actuator/health`
endpoint is used for the container health check.

## Known Issues

Resolved:

- [x] `docker-compose.dev.yml` already uses `pgvector/pgvector:pg16` (has the pgvector extension required by `V5__ai.sql`).
- [x] `application-dev.yaml` has local datasource/Redis/MinIO defaults filled in.
- [x] `application-prod.yaml`'s `razorpay:` block is correctly nested under `verbamind:`.
- [x] `docker-compose.prod.yml` now defines a full production stack (app + postgres + redis + minio).
- [x] `.env.example` is populated with all variables the app needs.
- [x] Added a root `Dockerfile` for building the app image (previously missing).
- [x] **Streaming completions now work for every provider.** `AiProvider.generateCompletionStream`
  was only implemented by `OllamaProvider` — `OpenAiProvider` and `GeminiProvider` were missing
  it entirely, which meant the project could not compile with those providers active. Both now
  stream via SSE (OpenAI's `stream: true` chat completions, Gemini's `streamGenerateContent?alt=sse`),
  matching the token-by-token behavior already used by `/chats/{chatId}/messages/stream`.

Outstanding:

- [ ] **Anthropic Claude is not yet a real provider.** `application-prod.yaml` has a `verbamind.ai.claude.api-key`
  property and the README lists Claude as supported, but there is no `ClaudeProvider` class. Since Anthropic
  has no public embeddings API, wiring it in cleanly requires deciding how embeddings are generated when
  `AI_PROVIDER=claude` (e.g. pairing it with a separate embedding provider). Left out rather than guessed at.
- [ ] No CI pipeline, TLS termination, or reverse proxy config is included — put this behind a reverse
  proxy (nginx/Caddy/your cloud LB) with TLS in front of port 8080 before exposing it publicly.
- [ ] The `.env` file bundled in this project contains real-looking local secrets. Rotate any credentials
  in it before using it beyond local development, and never commit it (it is already gitignored).

## License

Add your license here (e.g. MIT, Apache 2.0, or proprietary).