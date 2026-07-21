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

- [x] **1.1** Spring Security filter chain — how requests flow (theory + diagram)
- [x] **1.2** User entity + repository (Spring Data JPA + PostgreSQL)
- [x] **1.3** Password hashing with BCrypt — why and how
- [x] **1.4** JWT generation and validation (access token + refresh token)
- [x] **1.5** Registration endpoint (`POST /auth/register`)
- [x] **1.6** Login endpoint (`POST /auth/login`) + JWT response
- [x] **1.7** Refresh token endpoint (`POST /auth/refresh`)
- [x] **1.8** Role-based access control (USER / ADMIN roles)
- [x] **1.9** Global exception handling (`@ControllerAdvice`)
- [x] **1.10** Bean Validation (`@Valid`, custom validators)
- [ ] **1.11** Auth service unit tests (JUnit 5 + Mockito)

---

## Phase 2 — API Gateway
> Goal: Understand reverse proxying, JWT validation at the edge, and routing.

- [x] **2.1** Why an API Gateway exists (theory — single entry point, cross-cutting concerns)
- [x] **2.2** Spring Cloud Gateway setup + routing config
- [x] **2.3** JWT validation filter at the gateway (GlobalFilter + Ordered, reactive)
- [x] **2.4** Route definitions for each downstream service
- [ ] **2.5** Rate limiting at the gateway (using Redis)
- [ ] **2.6** CORS configuration

---

## Phase 3 — Job Service
> Goal: Understand REST design, file uploads, job state machines, and event publishing.

- [x] **3.1** Job entity design + state machine (Pending → Queued → Processing → Completed/Failed/Cancelled)
- [x] **3.2** Image upload endpoint — `multipart/form-data`, file storage strategy
- [x] **3.3** File storage — local disk (dev) with abstraction for future S3 swap
- [x] **3.4** Job creation flow — persist metadata, assign job ID
- [x] **3.5** RabbitMQ introduction — exchanges, queues, bindings, routing keys (theory)
- [x] **3.6** Publishing `IMAGE_JOB_CREATED` event to RabbitMQ
- [x] **3.7** Job status endpoint (`GET /jobs/{id}`)
- [x] **3.8** Job history endpoint (`GET /jobs?page=0&size=20`) — pagination
- [ ] **3.9** Job cancellation endpoint (`DELETE /jobs/{id}`)
- [x] **3.10** Optimistic locking on job status updates (`@Version`)
- [ ] **3.11** Job service integration tests

---

## Phase 4 — Worker Service
> Goal: Understand async processing, thread pools, CompletableFuture, and concurrency.

- [x] **4.1** Thread pools — `ExecutorService`, `Executors`, `ThreadPoolExecutor` (theory + sizing rules)
- [x] **4.2** `CompletableFuture` — composition, error handling, chaining (theory + examples)
- [x] **4.3** RabbitMQ consumer setup — `@RabbitListener`, acknowledgement modes, prefetch
- [x] **4.4** Image processing pipeline — factory pattern per operation type
- [x] **4.5** Resize operation (using `Thumbnailator`) — ONLY operation implemented
- [ ] **4.6** Compress operation
- [ ] **4.7** Crop, Rotate operations
- [ ] **4.8** Format conversion (PNG ↔ JPG ↔ WebP)
- [ ] **4.9** Watermark operation
- [ ] **4.10** Thumbnail generation
- [ ] **4.11** Metadata extraction (EXIF data)
- [ ] **4.12** ZIP batch export
- [ ] **4.13** Batch processing — parallel processing of 100s of images with `CompletableFuture.allOf()`
- [x] **4.14** Publishing `IMAGE_JOB_COMPLETED` / `IMAGE_JOB_FAILED` events (fan-out to 3 queues)
- [x] **4.15** Dead letter queue (DLQ) — handling poison messages
- [ ] **4.16** Worker service tests — concurrency correctness

---

## Phase 5 — Notification Service
> Goal: Understand WebSockets (STOMP), event-driven architecture, and real-time push.

- [x] **5.1** WebSockets vs polling vs SSE — when to use what (theory)
- [x] **5.2** STOMP over WebSocket setup with Spring
- [x] **5.3** RabbitMQ consumer for job completion events (`job.completed.notifications` queue)
- [x] **5.4** Broadcasting job status updates to connected clients
- [x] **5.5** Per-user topic subscriptions (`/user/{id}/queue/job-updates`)
- [x] **5.6** Notification history entity + Flyway migration + persistence
- [x] **5.7** `GET /notifications` endpoint with read/unread state
- [ ] **5.8** Notification service tests

---

## Phase 6 — Analytics Service
> Goal: Understand Redis caching strategies, cache invalidation, and aggregate queries.

- [x] **6.1** Redis data structures — String, Hash, List, Set, Sorted Set (theory + use cases)
- [x] **6.2** Caching strategies — Cache-Aside, Write-Through, Write-Behind (theory)
- [x] **6.3** `GET /analytics/summary` — total jobs, completed, failed, avg processing time
- [ ] **6.4** `GET /analytics/queue` — current queue depth, active workers
- [ ] **6.5** `GET /analytics/throughput` — jobs per minute / hour
- [x] **6.6** Cache-aside pattern with Spring Cache + Redis (`@Cacheable`, `@CacheEvict`)
- [x] **6.7** Cache TTL design + invalidation on job completion events
- [ ] **6.8** Rate limiting implementation using Redis + sliding window
- [ ] **6.9** Analytics service tests

---

## Phase 7 — Monitoring
> Goal: Understand observability — metrics, dashboards, and alerting.

- [x] **7.1** Observability pillars — metrics vs logs vs traces (theory)
- [x] **7.2** Spring Boot Actuator — `/health`, `/metrics`, `/info`, `/prometheus` endpoints
- [ ] **7.3** Custom metrics with Micrometer (`Counter`, `Gauge`, `Timer`)
- [x] **7.4** Prometheus scrape configuration (all 5 services at `/actuator/prometheus`)
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

- [x] **9.1** Docker fundamentals — images, layers, multi-stage builds (theory)
- [x] **9.2** Dockerfile for each Spring Boot service (multi-stage, non-root user, JVM container flags)
- [ ] **9.3** Dockerfile for Angular frontend (nginx)
- [x] **9.4** Docker Compose — full platform (`docker-compose.yml`)
- [x] **9.5** Environment variable management + YAML anchor (`x-common-env`)
- [x] **9.6** Health checks + service dependency ordering (`depends_on: condition: service_healthy`)
- [x] **9.7** Volume mounts for persistence (Postgres, Redis, uploads, processed images)

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
| 11 | Spring Security filter chain — request lifecycle and SecurityContextHolder | 1.1 |
| 12 | OncePerRequestFilter — why it guarantees single execution per request | 1.1 |
| 13 | Stateless JWT auth — no session, token re-validated on every request | 1.1 |
| 14 | BCrypt password hashing — why slow hashing resists brute-force | 1.3 |
| 15 | JWT access token vs opaque refresh token — different lifetimes, different storage | 1.4 |
| 16 | Refresh token rotation — revoke-on-use to detect token theft | 1.7 |
| 17 | User enumeration prevention — same error message for wrong email or wrong password | 1.6 |
| 18 | AuthenticationManager vs AuthenticationProvider — delegation pattern | 1.6 |
| 19 | Flyway migrations — schema versioning, never use ddl-auto create in production | 1.2 |
| 20 | Spring Cloud Gateway uses WebFlux/Netty — GlobalFilter + Ordered, not OncePerRequestFilter | 2.3 |
| 21 | Header spoofing prevention — strip X-User-Id header at gateway before injecting trusted one | 2.3 |
| 22 | RabbitMQ fan-out via topic exchange — one routing key fans out to N queues | 3.5 |
| 23 | Competing consumers vs fan-out — one queue = competing consumers, N queues = fan-out | 3.5 |
| 24 | Worker stateless design — process only, no DB write, publish event for others to update | 4.3 |
| 25 | CompletableFuture + ExecutorService — two-layer concurrency for non-blocking RabbitMQ listener | 4.2 |
| 26 | Dead letter queue — unprocessable messages rerouted to DLX to avoid poison message loops | 4.15 |
| 27 | WebSocket STOMP per-user channels — convertAndSendToUser() for private push | 5.5 |
| 28 | Cache-aside pattern — read from cache, miss → load from DB → write to cache | 6.6 |
| 29 | @CacheEvict vs @CachePut — evict forces fresh load on next read, put updates eagerly | 6.7 |
| 30 | Event-sourced analytics — analytics never queries job DB, builds its own read model from events | 6.2 |
| 31 | Optimistic locking with @Version — detect concurrent job updates without table locks | 3.10 |
| 32 | Docker multi-stage build — builder image with Maven, runtime image with JRE only (smaller, no secrets) | 9.2 |
| 33 | Non-root user in Docker — running as appuser limits blast radius if container is compromised | 9.2 |
| 34 | -XX:+UseContainerSupport — tells JVM to read CPU/memory limits from cgroups, not host | 9.2 |

---

## Interview Questions Bank
> Running list of Q&A built up as we go.

| # | Question | Phase |
|---|----------|-------|
| 1 | Monolith vs microservices — what are the trade-offs? | 0.1 |
| 2 | What is an API Gateway and why not validate JWT in every service? | 0.1 |
| 3 | Why use a message broker instead of direct HTTP calls for async work? | 0.1 |
| 4 | What does "database per service" mean and why does it matter? | 0.1 |
| 5 | What is the Spring Security filter chain? | 1.1 |
| 6 | Can you revoke a JWT? What are the options? | 1.4 |
| 7 | Why return same error for wrong email and wrong password? | 1.6 |
| 8 | What is refresh token rotation and why does it matter? | 1.7 |
| 9 | What is UserDetailsService and when is it called? | 1.2 |
| 10 | What is AuthenticationManager vs AuthenticationProvider? | 1.6 |
| 11 | Why does Spring Cloud Gateway use GlobalFilter instead of OncePerRequestFilter? | 2.3 |
| 12 | How do you prevent header spoofing in a gateway-based microservice architecture? | 2.3 |
| 13 | Explain RabbitMQ topic exchange fan-out. What happens if you use one queue instead of N? | 3.5 |
| 14 | Why is the worker service stateless? What are the trade-offs? | 4.3 |
| 15 | How do CompletableFuture and @RabbitListener work together? Why not process in the listener thread? | 4.2 |
| 16 | What is a dead letter queue and what problem does it solve? | 4.15 |
| 17 | How does STOMP per-user messaging work in Spring WebSocket? | 5.5 |
| 18 | What is the cache-aside pattern and when does it fall apart? | 6.6 |
| 19 | How do you ensure analytics are consistent without querying another service's database? | 6.2 |
| 20 | What is optimistic locking and when is it better than pessimistic locking? | 3.10 |

---

*Last updated: Phase 1 complete. Phases 2–7, 9 core implementation done. Only RESIZE operation in worker.*
