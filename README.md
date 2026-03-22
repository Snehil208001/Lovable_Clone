# Lovable Clone — Backend

Spring Boot API inspired by [Lovable](https://lovable.dev): projects, collaboration, files, billing-style endpoints, and usage. Built with **Java 21**, **Spring Boot 4**, **PostgreSQL**, **JPA**, **MapStruct**, and REST controllers under `/api`.

## Tech stack

| Layer | Technology |
|--------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Web | Spring Web MVC |
| Data | Spring Data JPA |
| DB | PostgreSQL |
| Mapping | MapStruct |
| Build | Maven (wrapper included) |
| Other | Lombok |

## What’s in the repo

- **REST API** — auth, projects, members, files, plans, subscription/checkout stubs, usage.
- **DTOs** — request/response records under `dto/`.
- **Entities & enums** — domain model under `entity/` and `enums/`.
- **Repositories** — `UserRepository`, `ProjectRepository`.
- **Services** — interfaces in `service/` with implementations in `service/impl/`.
- **Mappers** — e.g. `ProjectMapper` (MapStruct).

Configure the database in `src/main/resources/application.yml` (URL, user, password). Use your own credentials locally and avoid committing production secrets.

## API overview

Base URL (local): `http://localhost:8080`

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
| Billing | `POST` | `/api/stripe/checkout` |
| Billing | `POST` | `/api/stripe/portal` |
| Usage | `GET` | `/api/usage/today` |
| Usage | `GET` | `/api/usage/limits` |

## Domain model (conceptual)

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

- Java 21  
- Maven or `./mvnw` / `mvnw.cmd`  
- PostgreSQL (e.g. Docker on `localhost:9000` → container `5432`)

## Configuration

Example `application.yml` shape (adjust host, port, database name, and credentials):

```yaml
spring:
  application:
    name: lovable-clone
  datasource:
    url: jdbc:postgresql://localhost:9000/your_database
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

## Run

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
├── controller/
├── dto/
├── entity/
├── enums/
├── mapper/
├── repository/
└── service/
    └── impl/
```

## Next steps

- [ ] Orientation & Spring Boot project setup
- [ ] Requirement gathering & system design (Lovable Clone)
- [ ] Entity classes for Lovable Clone
- [ ] REST API endpoints for Lovable Clone
- [ ] MapStruct & `ProjectService`
- [ ] Project member management
- [ ] Exception handling, code cleanup & Spring Security
- [ ] JWT authentication
- [ ] Security methods & authorization
- [ ] Stripe payments integration
- [ ] Stripe webhooks (part 1 & part 2)
- [ ] AI code generation — architecture & setup
- [ ] AI code generation & MinIO setup
- [ ] Tool calling & file-tree advisor
- [ ] AI chat events
- [ ] AI code execution — system architecture
- [ ] Frontend app integration
- [ ] Kubernetes for code execution environment
- [ ] Reverse proxy for preview URLs
- [ ] Distributed Lovable Clone architecture & setup
- [ ] Account service, API gateway & workspace
- [ ] Intelligence service & workspace service
- [ ] Kafka & SAGA in distributed systems
- [ ] Kubernetes configuration for distributed systems
- [ ] Deploy cluster on Google Kubernetes Engine (GKE)
- [ ] GitHub Actions workflows for CI/CD (distributed systems)

## Repository

**GitHub:** [github.com/Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## License

Educational and personal use.
