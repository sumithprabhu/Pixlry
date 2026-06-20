# Pixlry — Distributed Image Processing Platform

A production-inspired image processing platform built with Java 21, Spring Boot 3, Angular, RabbitMQ, Redis, and PostgreSQL.

## Services

| Service | Port | Responsibility |
|---------|------|---------------|
| API Gateway | 8080 | Routing, JWT validation, rate limiting |
| Auth Service | 8081 | Registration, login, JWT issuance |
| Job Service | 8082 | Image uploads, job management |
| Worker Service | 8083 | Async image processing |
| Analytics Service | 8084 | Dashboard stats with Redis caching |
| Notification Service | 8085 | Real-time WebSocket push |

## Quick Start (Infrastructure)

```bash
cd docker
docker-compose up -d
```

This starts PostgreSQL, Redis, RabbitMQ, Prometheus, and Grafana.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA
- **Messaging:** RabbitMQ (AMQP)
- **Cache:** Redis
- **Database:** PostgreSQL
- **Monitoring:** Prometheus + Grafana
- **Build:** Maven (multi-module)
