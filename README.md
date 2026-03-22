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

## Lovable Clone Project — spec & progress

*Aligned with the **Lovable Clone Project** design (features, APIs, ERD). Below, **[x]** means delivered in this repo in a meaningful way; **route only** means the controller path exists but service logic is still stubbed / not wired to persistence.*

### Design

- [x] Requirement gathering & system design (Lovable Clone)

### Core features

**Projects**

- [x] Create project, manage project *(JPA + `ProjectServiceImpl` + `ProjectMapper`)*
- [x] List projects

**Auth**

- [ ] Login *(route: `POST /api/auth/login`)*
- [ ] Sign up *(route: `POST /api/auth/signup`)*
- [ ] Get my profile *(route: `GET /api/auth/me`)*

**AI code generation**

- [ ] List chat sessions
- [ ] Create new chat session
- [ ] Load full chat history
- [ ] Chat stream (SSE)
- [ ] Retry if failed

**Files**

- [ ] Get file tree & metadata *(route: `GET /api/projects/{id}/files`)*
- [ ] Get file content *(route: `GET /api/projects/{id}/files/**`)*
- [ ] Download all files as ZIP *(no `.../download-zip` route yet)*

**Preview**

- [ ] Get project preview
- [ ] Get logs stream (SSE)

### Additional features

- [ ] Payment (Stripe)
- [ ] Quota management — plans FREE | PRO
- [ ] Token usage & previews-running quotas
- [ ] Rate limiting (e.g. Redis)
- [ ] Zipkin tracing
- [ ] Members — one project, many users *(member routes exist; logic not implemented)*

### Core APIs — implementation snapshot

| Area | Spec | In codebase |
|------|------|-------------|
| Auth | `POST /api/auth/login`, `POST /api/auth/signup`, `GET /api/auth/me` | Routes yes · logic no |
| Projects | CRUD `/api/projects/{id}`, `GET /api/projects` | Yes (projects CRUD + list) |
| Files | Tree, file by path, `GET .../download-zip` | Tree + path routes · stubs · **no** download-zip |
| Members | `GET/POST/PATCH/DELETE` `/api/projects/{id}/members...` | Routes yes · logic no |
| Plans & billing | `GET /api/plans`, `GET /api/me/subscription`, `POST /api/stripe/checkout`, `POST /api/stripe/portal` | Routes yes · logic no |
| Usage | `GET /api/usage/today`, `GET /api/usage/limits` | Routes yes · logic no |
| Chat & AI | `.../chat-sessions`, messages, `POST /api/chat/stream` (SSE) | **Not implemented** |
| Preview & runner | `POST .../preview`, preview status, logs SSE, `DELETE .../preview` | **Not implemented** |

### Course / platform roadmap (optional follow-up)

- [x] Orientation & Spring Boot project setup
- [x] Entity classes & REST scaffolding
- [x] MapStruct & `ProjectService`
- [ ] Project member management (full implementation)
- [ ] Exception handling, Spring Security & JWT
- [ ] Stripe integration & webhooks
- [ ] AI, MinIO, previews, K8s, distributed architecture *(see project backlog)*

---

*Implementation note: `ProjectServiceImpl` is the main completed vertical; most other services return `null` or empty lists until built out.*

## Repository

**GitHub:** [github.com/Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## License

Educational and personal use.
