# Lovable Clone — Backend

Spring Boot API that mirrors the core ideas behind [Lovable](https://lovable.dev): AI-assisted projects, collaboration, files, previews, usage tracking, and Stripe-style billing. This repo is a **work in progress**: HTTP layer and domain types are in place; persistence and auth need to be wired up.

## Tech Stack

| Layer       | Technology           |
|------------|------------------------|
| Language   | Java 21                |
| Framework  | Spring Boot 4.0.4      |
| Web        | Spring Web MVC         |
| Persistence| Spring Data JPA        |
| Database   | PostgreSQL (planned)   |
| Build      | Maven + wrapper        |
| Utilities  | Lombok                 |

## Features (current)

- **REST controllers** for auth, projects, project members, files, billing/plans/subscription, and usage.
- **DTOs** for requests and responses under `dto/`.
- **Service interfaces** defining business operations (`service/`); implementations are not committed yet.
- **Domain model** under `entity/` and `enums/` (`User`, `Plan`, and others as Plain Old Java Objects; JPA mapping is partial — see below).

## API Overview

Base URL (local default): `http://localhost:8080`

| Area | Method | Path |
|------|--------|------|
| Auth | `POST` | `/api/auth/signup` |
| Auth | `POST` | `/api/auth/login` |
| Auth | `GET` | `/api/auth/me` |
| Projects | `GET` | `/api/projects` |
| Projects | `GET` | `/api/projects/{id}` |
| Projects | `POST` | `/api/projects` |
| Projects | `PATCH` | `/api/projects/{id}` |
| Projects | `DELETE` | `/api/projects/{id}` |
| Members | `GET` | `/api/projects/{projectId}/members` |
| Members | `POST` | `/api/projects/{projectId}/members` |
| Members | `PATCH` | `/api/projects/{projectId}/members/{memberId}` |
| Members | `DELETE` | `/api/projects/{projectId}/members/{memberId}` |
| Files | `GET` | `/api/projects/{projectId}/files` |
| Files | `GET` | `/api/projects/{projectId}/files/{*path}` |
| Plans | `GET` | `/api/plans` |
| Subscription | `GET` | `/api/me/subscription` |
| Stripe (stub) | `POST` | `/api/stripe/checkout` |
| Stripe (stub) | `POST` | `/api/stripe/portal` |
| Usage | `GET` | `/api/usage/today` |
| Usage | `GET` | `/api/usage/limits` |

> **Note:** Endpoints expect service implementations and a configured database. Many handlers currently use a placeholder user id until authentication is added.

## Domain Model (conceptual)

```
User ──< Project ──< ProjectFile
              │
              ├──< ProjectMember (EDITOR / VIEWER)
              ├──< ChatSession ──< ChatMessage
              ├──< Preview
              └──< UsageLog

Plan ──< Subscription ──> User
```

## Prerequisites

- **Java 21**
- **Maven** (or `./mvnw` / `mvnw.cmd`)
- **PostgreSQL** when you enable JPA against a real database

## Configuration

Add datasource and JPA settings to `src/main/resources/application.yml`, for example:

```yaml
spring:
  application:
    name: lovable-clone
  datasource:
    url: jdbc:postgresql://localhost:5432/lovable_clone
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

## Build & run

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

## Project layout

```
src/main/java/com/snehil/project/lovable_clone/
├── LovableCloneApplication.java
├── controller/          # REST endpoints
├── dto/                 # Records for API payloads
├── entity/             # Domain types (JPA mapping incomplete)
├── enums/
└── service/             # Service interfaces (implementations TBD)
```

## Roadmap

- [ ] Service implementations + Spring beans
- [ ] Repositories and full JPA mapping for all entities
- [ ] Spring Security + JWT (replace placeholder user id)
- [ ] Stripe integration and webhooks
- [ ] MinIO / object storage for project files
- [ ] AI chat providers and preview/runtime integration

## Repository

- **GitHub:** [github.com/Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## License

Educational and personal use.
