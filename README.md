# Verbamind

Verbamind is a production‑style Retrieval‑Augmented Generation (RAG) backend implemented with Spring Boot. It provides an end‑to‑end pipeline for uploading documents, asynchronous text extraction and chunking, embedding generation stored as vectors in PostgreSQL (pgvector), and provider‑agnostic LLM query/streaming to answer user questions with numbered citations. The codebase demonstrates multi‑tenant workspaces, JWT auth, MinIO object storage, Redis caching, usage/quota tracking, and a Razorpay payment integration.

This README documents only features implemented in the repository and explains architecture, request flow, AI/RAG design, and how to run the project for evaluation or portfolio review.

---

## At a glance

- Language / runtime: Java 21, Spring Boot 3.5
- Persistence: PostgreSQL + pgvector, Flyway for migrations
- Object storage: MinIO (S3-compatible)
- Cache: Redis
- AI providers: provider adapters for Ollama, OpenAI, Gemini (embeddings, completions, streaming)
- Background processing: Spring @Async for ingestion jobs
- Auth: JWT (access + refresh)
- Payments: Razorpay (order creation + webhook verification)
- Entry point: `src/main/java/com/verbamind/VerbamindApplication.java`

---

## Summary of implemented features

- User authentication: registration, login, token refresh, password reset flows; verification email hooks.
- Organizations (workspaces), memberships and access guards.
- Document lifecycle:
  - Upload (multipart) with file type and size validation.
  - Content-hash duplicate prevention.
  - MinIO-backed storage via `StorageService` and a `MinioStorageService` implementation.
  - Document versioning and endpoints to list/download/preview versions.
- Asynchronous ingestion pipeline:
  - Download from object storage → text extraction (Apache Tika) → chunking → embedding generation via `AiProvider`.
  - Embeddings persisted as `pgvector` values on `DocumentChunk` entities.
  - Document status transitions: UPLOADED → PROCESSING → READY / FAILED.
  - Notification emails on success/failure (email service implementation present).
- Retrieval and RAG:
  - Hybrid search (vector + full-text) with `HybridSearchService`.
  - Top-K selection and prompt assembly in `RagQueryService`.
  - Provider‑agnostic completion generation and streaming through provider adapters (`OpenAiProvider`, `OllamaProvider`, `GeminiProvider`).
  - Responses include numbered citations that map back to selected chunks/documents.
- Chat persistence with streaming endpoint (`SseEmitter`) that forwards provider tokens to clients.
- Usage and quota enforcement (reservation and token accounting).
- Subscription + payments integration with Razorpay (order creation and webhook verification).
- Flyway migrations for schema (auth, organization, document, AI/chunk, chat, subscription, payments, usage).

---

## Architecture (concise)

Mermaid architecture diagram (high‑level):

<img width="8113" height="2817" alt="image" src="https://github.com/user-attachments/assets/48cfad3d-0fa6-423c-9732-cac1c0495a5d" />


Key architectural choices:
- Provider‑adapter pattern (AiProvider interface) isolates provider-specific HTTP/streaming parsing from higher-level RAG code.
- Embeddings stored in Postgres (pgvector) to keep operational footprint in a single relational DB.
- Ingestion jobs run asynchronously using Spring `@Async` and are scheduled to start after successful DB commit (transaction synchronization).
- Micrometer timers are used to collect provider and ingestion latencies.

---

## Request / RAG flow (detailed)

1. Document upload
   - Endpoint: POST /api/organizations/{orgId}/documents (multipart file)
   - Behavior:
     - Controller validates content type and size (document service enforces allowed types and MAX_FILE_SIZE).
     - Storage quota asserted via `UsageService`.
     - Content hash (SHA‑256) is computed and used to prevent duplicate documents.
     - File uploaded to MinIO (`MinioStorageService.upload`), a `Document` row is created and persisted with status UPLOADED.
     - A transaction synchronization registers `DocumentIngestionService.processDocument(documentId)` to run after the transaction commits.

2. Ingestion (background)
   - `DocumentIngestionService.processDocument(UUID)` (annotated with `@Async`) executes:
     - Downloads the uploaded file via `StorageService.download`.
     - Extracts plain text using `TextExtractionService` (Apache Tika).
     - Splits text into chunks with `ChunkingService`.
     - Calls `AiProvider.generateEmbeddings(List<String>)` to get embeddings for chunks.
     - Persists `DocumentChunk` entities with `PGvector` embeddings into Postgres.
     - Updates document status to READY or FAILED and sends notification emails (via `EmailService`).

3. Querying / RAG
   - Endpoint: POST /api/organizations/{orgId}/ai or chat endpoints
   - Flow (`RagQueryService`):
     - `UsageService.reserveAiRequest` to enforce quota.
     - `AiProvider.generateEmbedding(question)` to get question embedding.
     - `HybridSearchService.search` performs hybrid retrieval: pgvector similarity (via `DocumentChunkRepository.findSimilarChunks`) and full-text search; results are fused by Reciprocal Rank Fusion (RRF).
     - If no relevant chunks are found an informative fallback is returned.
     - Otherwise selected chunks are assembled into a context block and a system prompt is prepared.
     - `AiProvider.generateCompletion` or `generateCompletionStream` is invoked for a synchronous or streaming response respectively.
     - Tokens used are approximated and recorded with `UsageService.addTokensUsed`.
     - The returned answer is accompanied by numbered citations (mapping to chunk/document ids), persisted in chat/message entities.

4. Streaming
   - Chat streaming endpoint: POST /api/organizations/{orgId}/chats/{chatId}/messages/stream (produces `text/event-stream`) implemented in `ChatController`.
   - `ChatController` uses an executor to call `chatService.streamMessage(...)`, which ultimately invokes `RagQueryService.answerStream` that calls `AiProvider.generateCompletionStream` and forwards token events to connected clients via SSE events (`token`, `done`).

---

## AI components (implementation details)

AiProvider interface (core abstraction)
- Location: `src/main/java/com/verbamind/ai/provider/AiProvider.java`
- Methods:
  - `float[] generateEmbedding(String text)`
  - `List<float[]> generateEmbeddings(List<String>)`
  - `String generateCompletion(String systemPrompt, String userPrompt)`
  - `void generateCompletionStream(String systemPrompt, String userPrompt, Consumer<String> onToken, Runnable onComplete)`

Provider implementations:
- `OpenAiProvider`:
  - Uses OpenAI REST endpoints for embeddings and chat completions.
  - Streaming: parses SSE-style responses (lines starting with `data:`), extracts delta tokens and forwards them via `onToken`.
  - Location: `src/main/java/com/verbamind/ai/provider/OpenAiProvider.java`
- `OllamaProvider`:
  - Communicates with Ollama runtime (local service) for embeddings and chat; supports JSON-lines streaming from Ollama.
  - Location: `src/main/java/com/verbamind/ai/provider/OllamaProvider.java`
- `GeminiProvider`:
  - Provider stub/implementation for Google Gemini (present in repo).
  - Location: `src/main/java/com/verbamind/ai/provider/GeminiProvider.java`

RAG prompt and citations:
- `RagQueryService` builds a context by formatting selected chunk content with markers `[1]`, `[2]` etc.
- The system prompt instructs the model to answer strictly from excerpts and include citations.
- `RagQueryService` constructs `AskQuestionResponse` containing the generated answer and `CitationDto` objects linking to document/chunk metadata.

Embedding storage:
- Entities: `DocumentChunk` stores chunk content, chunk index and embedding as `PGvector`.
- Migration: `V5__ai.sql` creates the AI/chunk tables (Flyway).

Hybrid retrieval:
- `HybridSearchService` fuses vector and full-text results using RRF to produce ranked chunks.

---

## Agent / asynchronous communication

- Ingestion agent: ingestion is asynchronous and transaction-aware. The upload handler registers a transaction synchronization callback that triggers ingestion only after DB commit, avoiding races between metadata and background processing.
- Provider streaming: each provider adapter handles provider-specific streaming formats and forwards tokens back via callbacks; the application forwards these tokens to clients over SSE.

---

## Folder / project tree (relevant)

```
.
├── Dockerfile
├── docker/
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
├── pom.xml
├── .env.example
├── src/
│   ├── main/
│   │   ├── java/com/verbamind/
│   │   │   ├── VerbamindApplication.java
│   │   │   ├── ai/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── provider/        # AiProvider + OpenAi/Ollama/Gemini
│   │   │   │   ├── repository/
│   │   │   │   └── service/         # RagQueryService, HybridSearchService, ingestion services
│   │   │   ├── auth/                # JWT auth, registration, email integration
│   │   │   ├── document/            # upload controllers, DocumentService, StorageService, MinioStorageService
│   │   │   ├── chat/                # chat controllers, streaming endpoints (SSE)
│   │   │   ├── organization/
│   │   │   ├── payment/             # Razorpay integration
│   │   │   ├── subscription/
│   │   │   ├── usage/
│   │   │   ├── security/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── application*.yaml
│   │       └── db/migration/
└── frontend/                        # React + Vite client
```

---

## How to run (developer / evaluator)

Prereqs:
- JDK 21
- Docker & Docker Compose
- Maven (or use the bundled `./mvnw`)

1) Start development infrastructure
```bash
cd docker
docker compose -f docker-compose.dev.yml up -d
```
This brings up:
- PostgreSQL image with pgvector
- Redis
- MinIO
- Ollama (local model runtime used in dev compose)

2) Configure the application
- Copy `.env.example` to `.env` for Docker production compose, or set env variables directly.
- For local dev you can edit `src/main/resources/application-dev.yaml` (the repo provides a dev config).

3) Run the application locally (dev profile)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

4) Or run in production-like mode with Docker (build + compose)
```bash
cp .env.example .env
# edit .env with real secrets
cd docker
docker compose -f docker-compose.prod.yml --env-file ../.env up -d --build
```

Health check:
```
GET /actuator/health
```

---

## Important environment variables

(see `.env.example` for full list and defaults)

- Database
  - DB_URL / spring.datasource.url
  - DB_USERNAME, DB_PASSWORD

- Redis
  - REDIS_HOST, REDIS_PORT

- JWT
  - JWT_SECRET

- MinIO (object storage)
  - MINIO_ENDPOINT
  - MINIO_ACCESS_KEY
  - MINIO_SECRET_KEY
  - MINIO_BUCKET

- Email (SMTP)
  - MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD
  - Verbamind mail properties (verification/reset/invite URLs) are configured in application YAML via `verbamind.mail.*`

- AI provider selection & keys
  - VERBAMIND_AI_PROVIDER (configured under `verbamind.ai.provider` in YAML; commonly `ollama`, `openai`, or `gemini`)
  - OPENAI_API_KEY
  - GEMINI_API_KEY
  - OLLAMA_BASE_URL (or `verbamind.ai.ollama.base-url`)

- Payments
  - RAZORPAY_KEY_ID
  - RAZORPAY_KEY_SECRET
  - RAZORPAY_WEBHOOK_SECRET

---

## API overview (selected / implemented endpoints)

Base path: `/api`

- Auth
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/refresh
  - POST /api/auth/forgot-password
  - POST /api/auth/reset-password

- Organizations
  - /api/organizations/* (create, list, get, invite members)

- Documents
  - POST /api/organizations/{orgId}/documents (upload)
  - POST /api/organizations/{orgId}/documents/{documentId}/versions (upload new version)
  - GET /api/organizations/{orgId}/documents (list)
  - GET /api/organizations/{orgId}/documents/{documentId}/download
  - GET /api/organizations/{orgId}/documents/{documentId}/preview
  - GET /api/organizations/{orgId}/documents/{documentId}/versions
  - PATCH/DELETE endpoints for document management
  - Tagging endpoints: POST /{documentId}/tags, DELETE /{documentId}/tags/{tag}

- AI / RAG
  - POST /api/organizations/{orgId}/ai (ask question — uses `RagQueryService`)

- Chats
  - POST /api/organizations/{orgId}/chats
  - POST /api/organizations/{orgId}/chats/{chatId}/messages
  - POST /api/organizations/{orgId}/chats/{chatId}/messages/stream (SSE streaming endpoint)

- Payments & Subscriptions
  - Payment/order creation and webhook handling (Razorpay integration under `payment` package)

- Usage
  - /api/organizations/{orgId}/usage (usage/quota endpoints)

Most routes require Authorization: `Bearer <access_token>`.

Refer to the DTO classes under `src/main/java/com/verbamind/*/dto` for request/response shapes.

---

## Usage examples

Register & login:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"P@ssw0rd","fullName":"Alice"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"P@ssw0rd"}'
```

Upload a document (after obtaining an access token):

```bash
curl -X POST "http://localhost:8080/api/organizations/{orgId}/documents" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F "file=@/path/to/document.pdf"
```

Ask a RAG question:

```bash
curl -X POST "http://localhost:8080/api/organizations/{orgId}/ai" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"question":"Summarize the key points from the uploaded document.", "topK":5}'
```

Stream chat messages (SSE):

```bash
curl -N -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"content":"Explain section 2"}' \
  "http://localhost:8080/api/organizations/{orgId}/chats/{chatId}/messages/stream"
```

---

## Where to look in the code (reviewer guide)

- Application entry: `src/main/java/com/verbamind/VerbamindApplication.java`
- AI abstraction & adapters: `src/main/java/com/verbamind/ai/provider/*`
- RAG and search: `src/main/java/com/verbamind/ai/service/RagQueryService.java`, `HybridSearchService.java`
- Ingestion: `src/main/java/com/verbamind/ai/service/DocumentIngestionService.java`
- Document upload and versions: `src/main/java/com/verbamind/document/` (DocumentService, DocumentController, DocumentVersion entity)
- Storage: `src/main/java/com/verbamind/document/service/MinioStorageService.java`
- Email service: `src/main/java/com/verbamind/auth/service/SmtpEmailService.java`
- Payments: `src/main/java/com/verbamind/payment/`
- DB migrations: `src/main/resources/db/migration/` (look at `V5__ai.sql` for AI tables)
- Tests: `src/test/java/...` includes unit tests for RAG behavior (e.g., `RagQueryServiceTest`)



---

## License

See `LICENSE.md` in the repository root.
