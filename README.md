# Verbamind

Verbamind is a production-style Retrieval-Augmented Generation (RAG) backend built with Spring Boot that supports asynchronous document ingestion, vector search with pgvector, provider-agnostic LLM integrations (OpenAI, Gemini, Ollama), streaming responses, and multi-tenant workspaces.

This README documents only features implemented in the codebase and explains the architecture, request flow, AI/RAG pipeline, and how to run the project for evaluation or portfolio review.

---

## Key facts (at-a-glance)

- Language / runtime: Java 21, Spring Boot 3.5
- Persistence: PostgreSQL (pgvector extension used for vector embeddings), Flyway for migrations
- Object storage: MinIO (S3-compatible)
- Cache: Redis
- AI integrations: pluggable provider adapters for Ollama, OpenAI, and Gemini (embedding + completion + streaming)
- Auth: JWT-based access + refresh tokens
- Background processing: Spring @Async for ingestion/extraction/embedding jobs
- Payments: Razorpay integration (orders + webhook verification)
- Project entrypoint: `src/main/java/com/verbamind/VerbamindApplication.java`

---

## What is implemented

The repository implements a production-style RAG platform backend with the following working features:

- User authentication, registration, JWT token handling, email-based notifications (email service used by ingestion).
- Organizations (workspaces), organization membership checks and access guards.
- Document upload flow:
  - Files are stored in MinIO via a StorageService implementation.
  - Uploads are validated (file types and size) and duplicate content is prevented using a content hash.
  - After successful upload and DB commit, an asynchronous ingestion job runs to extract text, chunk the text, generate embeddings, and persist chunks + embeddings.
- Document versioning, tagging, listing, download and preview.
- AI ingestion pipeline:
  - Text extraction service (pluggable) → chunking → embeddings via AiProvider implementations.
  - Embeddings are stored into Postgres as vectors (pgvector wrapper used in code).
- RAG query pipeline:
  - Top-k selection via vector similarity (pgvector) and prompt assembly.
  - Provider-agnostic completion generation and token streaming through provider adapters (Ollama, OpenAI, Gemini).
  - Responses include numbered citations that map back to selected chunks/documents.
- Chat and conversation persistence; streaming endpoints to stream token-by-token completions.
- Usage and quota enforcement (storage and AI request tracking).
- Subscriptions and payments (Razorpay) with webhook verification.
- Flyway-managed schema migrations, including tables for auth, organizations, documents, AI chunks/embeddings, chat, subscription, payment and usage (migrations live under `src/main/resources/db/migration/`).

Note: Anthropic Claude is referenced in the original project README but there is no provider implementation for it in the codebase; it is intentionally not included here.

---

## Architecture (concise)

The application is a Spring Boot web service exposing HTTP JSON endpoints (controllers). It uses relational persistence for metadata and pgvector for vector operations. Uploaded files are stored in MinIO and processed asynchronously. AI provider communication is encapsulated behind a provider interface with concrete adapters.

Mermaid overview:

<img width="3898" height="3757" alt="image" src="https://github.com/user-attachments/assets/fdb1035b-d35d-4d11-b330-d1911cee18ef" />

```
---
Components:
- API controllers: request validation, authorization, and orchestration.
- Services: DocumentService, DocumentIngestionService, TextExtractionService, ChunkingService, RagQueryService, HybridSearchService.
- Persistence: JPA repositories and Flyway migrations; DocumentChunk entities use pgvector types.
- AI provider adapters: `AiProvider` interface + `OllamaProvider`, `OpenAiProvider`, `GeminiProvider` (embedding, completion, and streaming methods).
- Background ingestion: `DocumentIngestionService.processDocument` is annotated with `@Async` and is scheduled to run after the upload transaction commits.

---

## Request / RAG flow (detailed)

1. Client uploads a file
   - POST /api/organizations/{orgId}/documents with multipart file
   - Server validates content type/size, ensures quota, computes content hash to avoid duplicates, stores file in MinIO and creates a Document row.
   - After the upload transaction commits, `DocumentIngestionService.processDocument(documentId)` runs asynchronously.

2. Document ingestion (background)
   - StorageService downloads file from MinIO.
   - TextExtractionService extracts plain text from file input stream.
   - ChunkingService splits text into chunks (the chunking implementation is in code).
   - AiProvider.generateEmbeddings is called for chunks (providers implement batch/looping).
   - For each chunk, a DocumentChunk with an embedding (pgvector) is created and persisted.
   - Document status transitions from UPLOADED → PROCESSING → READY (or FAILED on error).
   - Owner receives notification emails on success/failure via EmailService.

3. Querying / Chat (RAG)
   - Client posts a question (e.g., POST /api/organizations/{orgId}/ai or chat endpoints).
   - API performs usage/quota checks, then runs a similarity search (pgvector) to select top-k chunks.
   - The selected chunks are composed into prompt context and passed to `AiProvider.generateCompletion(...)` or `generateCompletionStream(...)`.
   - Provider returns a completion (either final text or token stream). The API returns the answer along with numbered citations linking to the chunk/document sources and persists chat/messages.

4. Streaming
   - Streaming is supported: providers expose streaming logic and the application streams tokens to clients (SSE or streaming controller endpoints implemented in code).

Sequence diagram:

<img width="8192" height="3540" alt="image" src="https://github.com/user-attachments/assets/9681a439-b306-43e8-85d1-5b9fcd9e4c78" />


---

## Agent / async communication

- Asynchronous ingestion is implemented using Spring's `@Async` support. The upload handler registers a transaction synchronization that calls the ingestion service after the upload transaction commits (`TransactionSynchronizationManager.registerSynchronization(...).afterCommit()`), ensuring ingestion only runs for committed uploads.
- Provider adapters perform blocking HTTP calls and in the streaming case read provider SSE/stream responses and forward tokens to the registered consumer callbacks.

---

## Project tree (relevant portions)

```
.
├── Dockerfile
├── docker/
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/verbamind/
│   │   │   ├── VerbamindApplication.java
│   │   │   ├── ai/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/                (DocumentChunk entity uses pgvector)
│   │   │   │   ├── provider/              (AiProvider, OllamaProvider, OpenAiProvider, GeminiProvider)
│   │   │   │   ├── repository/            (chunk repository)
│   │   │   │   └── service/               (DocumentIngestionService, RagQueryService, ChunkingService, TextExtractionService)
│   │   │   ├── auth/
│   │   │   ├── document/                  (DocumentService, MinioStorageService, FileHashService, versioning)
│   │   │   ├── chat/
│   │   │   ├── organization/
│   │   │   ├── payment/
│   │   │   ├── subscription/
│   │   │   ├── usage/
│   │   │   ├── security/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       └── db/migration/   (Flyway migrations V1..V14)
└── README.md
```

---

## How to run (developer / evaluator)

Prerequisites:
- JDK 21
- Docker & Docker Compose
- Maven (or use bundled `./mvnw`)

1. Start local infra (dev)

```bash
cd docker
docker compose -f docker-compose.dev.yml up -d
```

The dev compose brings up Postgres (pgvector image), Redis, MinIO and Ollama (local AI runtime used for development).

2. Configure dev Spring profile

Edit `src/main/resources/application-dev.yaml` or set env vars. Example snippet present in the repo:

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

3. Run the application

```bash
# run with dev profile (uses application-dev.yaml)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or build and run via Docker for a production-style run:

```bash
cp .env.example .env   # fill in real values
cd docker
docker compose -f docker-compose.prod.yml --env-file ../.env up -d --build
```

4. (Optional) Pull Ollama models (when using local Ollama)

```bash
docker exec -it documind-ollama ollama pull nomic-embed-text
docker exec -it documind-ollama ollama pull llama3.2
```

5. Health check

```
GET /actuator/health
```

---

## Environment variables

Primary env vars used by the application (examples and defaults are in `.env.example`):

- Database
  - DB_URL (or set `spring.datasource.url`) — e.g. `jdbc:postgresql://<host>:5432/<db>`
  - DB_USERNAME
  - DB_PASSWORD

- Redis
  - REDIS_HOST
  - REDIS_PORT (default: 6379)

- JWT
  - JWT_SECRET

- MinIO (S3)
  - MINIO_ENDPOINT
  - MINIO_ACCESS_KEY
  - MINIO_SECRET_KEY
  - MINIO_BUCKET (if applicable)

- AI provider selection + keys
  - AI_PROVIDER (one of `ollama`, `openai`, `gemini`)
  - OPENAI_API_KEY (used by `OpenAiProvider`)
  - GEMINI_API_KEY (used by `GeminiProvider`)
  - OLLAMA_BASE_URL (used by `OllamaProvider`)

- Razorpay (payments)
  - RAZORPAY_KEY_ID
  - RAZORPAY_KEY_SECRET
  - RAZORPAY_WEBHOOK_SECRET

Set these values either in `application-*.yaml` files or as environment variables.

---

## API overview (implemented endpoints / areas)

Base path: `/api`

Implemented areas (controllers and routes exist in the codebase):

- Auth: `/api/auth/**` (registration, login, token refresh)
- Organizations: `/api/organizations/**`
- Documents: `/api/organizations/{organizationId}/documents` (upload, list, download, versions, tags)
- AI / RAG: `/api/organizations/{organizationId}/ai` (question endpoints)
- Chats: `/api/organizations/{organizationId}/chats` (threads, messages, streaming)
- Subscriptions: `/api/organizations/{organizationId}/subscription`
- Payments: `/api/organizations/{organizationId}/payments`
- Usage: `/api/organizations/{organizationId}/usage`
- Current user: `/api/users/me`
- Admin: `/api/admin/**` (requires admin role)
- Webhooks: `/api/webhooks/razorpay`
- Actuator: `/actuator/health` (health check)

Most routes require Authorization: `Bearer <access_token>` except auth, health, and webhook endpoints.

---

## Usage examples

1) Register & login (obtain tokens)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"P@ssw0rd","name":"Alice"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"P@ssw0rd"}'
```

2) Upload a document

```bash
curl -X POST "http://localhost:8080/api/organizations/{orgId}/documents" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F "file=@/path/to/document.pdf"
```

Note: after upload, ingestion is scheduled asynchronously; check document status via listing or metadata endpoints to wait for status READY.

3) Ask a question (RAG)

```bash
curl -X POST "http://localhost:8080/api/organizations/{orgId}/ai" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"question":"Summarize the key points from the uploaded document.", "topK":5}'
```

4) Stream chat messages (SSE — example)

```bash
curl -N -H "Authorization: Bearer <ACCESS_TOKEN>" \
  "http://localhost:8080/api/organizations/{orgId}/chats/{chatId}/messages/stream"
```

Refer to DTO classes in `src/main/java/com/verbamind/*/dto` for exact request/response shapes.

---

## Notable implementation details and references

- Provider adapters: `AiProvider` interface defines:
  - `generateEmbedding(String text)` and `generateEmbeddings(List<String>)`
  - `generateCompletion(String systemPrompt, String userPrompt)`
  - `generateCompletionStream(systemPrompt, userPrompt, onToken, onComplete)`

  Implementations exist for Ollama, OpenAI, and Gemini under `src/main/java/com/verbamind/ai/provider/`. Each provider handles its own HTTP calls and streaming parsing.

- Ingestion orchestration: `DocumentService.upload(...)` registers a transaction synchronization to call `DocumentIngestionService.processDocument(documentId)` after commit, ensuring ingestion runs only for successfully committed uploads.

- Embeddings storage: `DocumentIngestionService` writes `DocumentChunk` entities with embeddings stored as PGvector (`com.pgvector.PGvector`) — migrations include AI-related SQL files under `src/main/resources/db/migration/` (e.g., `V5__ai.sql`).

- Streaming: `generateCompletionStream` is implemented in all three provider adapters; OpenAI implementation parses `data:` SSE lines, Gemini uses `streamGenerateContent?alt=sse`, Ollama reads provider-specific streaming JSON lines.

- Payments: Razorpay order creation and webhook handling are present under the `payment` package and corresponding migrations.

---

## Accuracy notes / intentionally omitted items

- The code includes a `TextExtractionService` used by ingestion; the exact extraction implementation is present in code (see `src/main/java/com/verbamind/ai/service/TextExtractionService`), so the ingestion flow is implemented.
---

## Future improvements (non-exhaustive, aligned with repo notes)

These are plausible next steps and some are noted in the repository itself:

- Improve batch embedding throughput (providers currently may call embeddings per-chunk in a loop; native batching would reduce latency).
- Add CI (build/test) and automated security scans.
---

## For recruiters / engineers reviewing this project

- Focus areas to evaluate in the codebase:
  - Ingestion: `DocumentIngestionService`, `TextExtractionService`, `ChunkingService`
  - Vector storage and similarity: `DocumentChunk` entity, `ai.repository` classes, and Flyway migration `V5__ai.sql`
  - Provider adapters: `src/main/java/com/verbamind/ai/provider/*` (OpenAI, Gemini, Ollama) — look at `generateCompletionStream` implementations to evaluate streaming handling
  - Security and auth: `security` package and `auth` package for JWT handling and authorization guards
  - Transactional/async behavior: `DocumentService.upload(...)` and `TransactionSynchronizationManager` use

This project demonstrates end-to-end work on an engineering problem set relevant to retrieval-augmented generation: ingestion, vector storage, provider integration, streaming, quotas, and multi-tenant workspace handling — all implemented and wired together in a Spring Boot backend.

---

## License

See `LICENSE.md` in the repository root.
