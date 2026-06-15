# Image Processing Platform — Learning Roadmap

> **Philosophy:** One concept at a time. Understand before you build. Production patterns, not tutorials.
> 
> **Legend:** `[ ]` Not started · `[~]` In progress · `[x]` Done · `[!]` Needs revisit

---

## Phase 0 — Project Foundation
> Goal: Understand the architecture before writing a single line of code.

- [x] **0.1** Architecture deep-dive — why microservices, what each service owns, and the data flow
- [x] **0.2** Maven multi-module project setup (parent POM + child modules)
- [x] **0.3** Docker Compose skeleton — PostgreSQL, Redis, RabbitMQ running locally
- [x] **0.4** Shared library module (`common`) — DTOs, exceptions, constants

---

## Phase 1 — Authentication Service
> Goal: Understand JWT, Spring Security filter chain, and stateless auth.

- [ ] **1.1** Spring Security filter chain — how requests flow (theory + diagram)
- [ ] **1.2** User entity + repository (Spring Data JPA + PostgreSQL)
- [ ] **1.3** Password hashing with BCrypt — why and how
- [ ] **1.4** JWT generation and validation (access token + refresh token)
- [ ] **1.5** Registration endpoint (`POST /auth/register`)
- [ ] **1.6** Login endpoint (`POST /auth/login`) + JWT response
- [ ] **1.7** Refresh token endpoint (`POST /auth/refresh`)
- [ ] **1.8** Role-based access control (USER / ADMIN roles)
- [ ] **1.9** Global exception handling (`@ControllerAdvice`)
- [ ] **1.10** Bean Validation (`@Valid`, custom validators)
- [ ] **1.11** Auth service unit tests (JUnit 5 + Mockito)

---

## Phase 2 — API Gateway
> Goal: Understand reverse proxying, JWT validation at the edge, and routing.

- [ ] **2.1** Why an API Gateway exists (theory — single entry point, cross-cutting concerns)
- [ ] **2.2** Spring Cloud Gateway setup + routing config
- [ ] **2.3** JWT validation filter at the gateway
- [ ] **2.4** Route definitions for each downstream service
- [ ] **2.5** Rate limiting at the gateway (using Redis)
- [ ] **2.6** CORS configuration

---

## Phase 3 — Job Service
> Goal: Understand REST design, file uploads, job state machines, and event publishing.

- [ ] **3.1** Job entity design + state machine (Pending → Queued → Processing → Completed/Failed/Cancelled)
- [ ] **3.2** Image upload endpoint — `multipart/form-data`, file storage strategy
- [ ] **3.3** File storage — local disk (dev) with abstraction for future S3 swap
- [ ] **3.4** Job creation flow — persist metadata, assign job ID
- [ ] **3.5** RabbitMQ introduction — exchanges, queues, bindings, routing keys (theory)
- [ ] **3.6** Publishing `IMAGE_JOB_CREATED` event to RabbitMQ
- [ ] **3.7** Job status endpoint (`GET /jobs/{id}`)
- [ ] **3.8** Job history endpoint (`GET /jobs?page=0&size=20`) — pagination
- [ ] **3.9** Job cancellation endpoint (`DELETE /jobs/{id}`)
- [ ] **3.10** Optimistic locking on job status updates
- [ ] **3.11** Job service integration tests

---

## Phase 4 — Worker Service
> Goal: Understand async processing, thread pools, CompletableFuture, and concurrency.

- [ ] **4.1** Thread pools — `ExecutorService`, `Executors`, `ThreadPoolExecutor` (theory + sizing rules)
- [ ] **4.2** `CompletableFuture` — composition, error handling, chaining (theory + examples)
- [ ] **4.3** RabbitMQ consumer setup — `@RabbitListener`, acknowledgement modes
- [ ] **4.4** Image processing pipeline — strategy pattern per operation type
- [ ] **4.5** Resize operation (using `Thumbnailator` / `ImageIO`)
- [ ] **4.6** Compress operation
- [ ] **4.7** Crop, Rotate operations
- [ ] **4.8** Format conversion (PNG ↔ JPG ↔ WebP)
- [ ] **4.9** Watermark operation
- [ ] **4.10** Thumbnail generation
- [ ] **4.11** Metadata extraction (EXIF data)
- [ ] **4.12** ZIP batch export
- [ ] **4.13** Batch processing — parallel processing of 100s of images with `CompletableFuture.allOf()`
- [ ] **4.14** Publishing `IMAGE_JOB_COMPLETED` / `IMAGE_JOB_FAILED` events
- [ ] **4.15** Dead letter queue (DLQ) — handling poison messages
- [ ] **4.16** Worker service tests — concurrency correctness

---

## Phase 5 — Notification Service
> Goal: Understand WebSockets (STOMP), event-driven architecture, and real-time push.

- [ ] **5.1** WebSockets vs polling vs SSE — when to use what (theory)
- [ ] **5.2** STOMP over WebSocket setup with Spring
- [ ] **5.3** RabbitMQ consumer for job completion events
- [ ] **5.4** Broadcasting job status updates to connected clients
- [ ] **5.5** Per-user topic subscriptions (private channels)
- [ ] **5.6** Notification history entity + persistence
- [ ] **5.7** `GET /notifications` endpoint with read/unread state
- [ ] **5.8** Notification service tests

---

## Phase 6 — Analytics Service
> Goal: Understand Redis caching strategies, cache invalidation, and aggregate queries.

- [ ] **6.1** Redis data structures — String, Hash, List, Set, Sorted Set (theory + use cases)
- [ ] **6.2** Caching strategies — Cache-Aside, Write-Through, Write-Behind (theory)
- [ ] **6.3** `GET /analytics/summary` — total jobs, processed, failed, avg time
- [ ] **6.4** `GET /analytics/queue` — current queue depth, active workers
- [ ] **6.5** `GET /analytics/throughput` — jobs per minute / hour
- [ ] **6.6** Cache-aside pattern with Spring Cache + Redis
- [ ] **6.7** Cache TTL design + invalidation on job events
- [ ] **6.8** Rate limiting implementation using Redis + sliding window
- [ ] **6.9** Analytics service tests

---

## Phase 7 — Monitoring
> Goal: Understand observability — metrics, dashboards, and alerting.

- [ ] **7.1** Observability pillars — metrics vs logs vs traces (theory)
- [ ] **7.2** Spring Boot Actuator — `/health`, `/metrics`, `/info` endpoints
- [ ] **7.3** Custom metrics with Micrometer (`Counter`, `Gauge`, `Timer`)
- [ ] **7.4** Prometheus scrape configuration
- [ ] **7.5** Grafana dashboard — CPU, memory, queue size, processing time, active threads
- [ ] **7.6** Alert rules in Prometheus (queue too large, high failure rate)

---

## Phase 8 — Angular Frontend
> Goal: Understand Angular architecture, reactive patterns, and WebSocket integration.

- [ ] **8.1** Angular project setup + Angular Material + folder structure
- [ ] **8.2** Auth module — login / register pages, JWT storage, HTTP interceptor
- [ ] **8.3** Route guards — protecting authenticated routes
- [ ] **8.4** Dashboard page — analytics summary cards
- [ ] **8.5** Upload page — multi-file upload with progress bar
- [ ] **8.6** Job history page — paginated table with status badges
- [ ] **8.7** Live job progress — WebSocket/STOMP client integration
- [ ] **8.8** Notifications panel — real-time badge counter
- [ ] **8.9** Worker status page
- [ ] **8.10** Admin dashboard
- [ ] **8.11** Downloads page
- [ ] **8.12** Error handling + loading states

---

## Phase 9 — Containerization
> Goal: Understand Docker multi-stage builds and Docker Compose orchestration.

- [ ] **9.1** Docker fundamentals — images, layers, multi-stage builds (theory)
- [ ] **9.2** Dockerfile for each Spring Boot service (multi-stage)
- [ ] **9.3** Dockerfile for Angular frontend (nginx)
- [ ] **9.4** Docker Compose — full platform (`docker-compose.yml`)
- [ ] **9.5** Environment variable management + `.env` files
- [ ] **9.6** Health checks + service dependency ordering
- [ ] **9.7** Volume mounts for persistence (Postgres, Redis, image storage)

---

## Phase 10 — Production Hardening
> Goal: Apply patterns that separate hobby projects from real systems.

- [ ] **10.1** Structured logging with correlation IDs (MDC)
- [ ] **10.2** Distributed tracing concepts (theory — where Zipkin/Jaeger fits)
- [ ] **10.3** Circuit breaker pattern (Resilience4j) — theory + implementation
- [ ] **10.4** Idempotency — safe retries for job creation
- [ ] **10.5** Graceful shutdown — in-flight job handling
- [ ] **10.6** API versioning strategy
- [ ] **10.7** Database migrations with Flyway
- [ ] **10.8** Connection pool tuning (HikariCP)

---

## Concepts Learned
> Fill this in as we complete each phase.

| # | Concept | Covered In |
|---|---------|-----------|
| 1 | Microservices vs Monolith — trade-offs | 0.1 |
| 2 | API Gateway — single entry point, JWT validation at edge | 0.1 |
| 3 | Database-per-service — loose coupling | 0.1 |
| 4 | Async via message broker vs synchronous HTTP | 0.1 |
| 5 | Two-path architecture — fast path (sync) + async processing path | 0.1 |
| 6 | WebSocket for real-time push vs polling | 0.1 |
| 7 | Maven multi-module — parent POM, dependencyManagement, module ordering | 0.2 |
| 8 | Why common module must be a plain JAR, not a Spring Boot fat JAR | 0.2 |
| 9 | Spring Cloud Gateway runs on WebFlux/Netty — incompatible with spring-boot-starter-web | 0.2 |
| 10 | `${VAR:default}` pattern in application.yml for environment-aware config | 0.2 |

---

## Interview Questions Bank
> Running list of Q&A built up as we go.

| # | Question | Phase |
|---|----------|-------|
| 1 | Monolith vs microservices — what are the trade-offs? | 0.1 |
| 2 | What is an API Gateway and why not validate JWT in every service? | 0.1 |
| 3 | Why use a message broker instead of direct HTTP calls for async work? | 0.1 |
| 4 | What does "database per service" mean and why does it matter? | 0.1 |

---

*Last updated: 0.2 done — moving to 0.3 Docker Compose skeleton*
