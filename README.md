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
| Security | Spring Security + JWT (`Authorization: Bearer …`) |
| Validation | Bean Validation (`spring-boot-starter-validation`) |
| Mapping | MapStruct |
| Build | Maven (wrapper included) |
| Payments | Stripe Java SDK (`stripe-java`) |
| AI | Spring AI 2.x (`spring-ai-starter-model-openai`) — OpenAI chat (streaming) |
| Object storage | MinIO Java client (`io.minio`) for project file buckets |
| Reactive (client) | Reactor / WebClient (Spring AI OpenAI transport) |
| Other | Lombok |

## What’s in the repo

- **REST API** — auth, projects, members, files, plans, **Stripe checkout + customer portal (partial)**, subscription APIs, usage stubs, payment webhooks.
- **DTOs** — request/response records under `dto/` (with validation annotations where used).
- **Entities & enums** — domain model under `entity/` and `enums/`.
- **Repositories** — `UserRepository`, `ProjectRepository`, `ProjectMemberRepository`, **`PlanRepository`**, **`SubscriptionRepository`**.
- **Services** — interfaces in `service/` with implementations in `service/impl/` (including **`SubscriptionService`** for billing flows).
- **Mappers** — `ProjectMapper`, `ProjectMemberMapper`, `UserMapper`, **`SubscriptionMapper`** (MapStruct).
- **Errors** — `GlobalExceptionHandler`, `ApiError`, custom exceptions under `error/`.
- **Security** — `WebSecurityConfig` (`@EnableMethodSecurity`), `JwtAuthFilter`, `JwtAuthEntryPoint`, `JwtUserPrincipal`, `SecurityUtil`, **`SecurityExpression`** (`@Component("security")`) for **`@PreAuthorize`** on project APIs. **`JwtAuthFilter`** runs on **Servlet async** dispatches and **always** applies a valid `Bearer` token to `SecurityContextHolder` so streaming endpoints (`text/event-stream`) stay authenticated.
- **AI chat** — `POST /api/chat/stream` returns **SSE** (`Flux<ServerSentEvent<String>>`) via `AiGenerationService` / Spring AI **OpenAI** (configure **`OPENAI_API_KEY`** and network/DNS access to the API host).
- **Config** — `AiConfig`, `StorageConfig` (MinIO); **`services.docker-compose.yml`** for local **PostgreSQL (pgvector)** and **MinIO**.
- **Project files** — `ProjectFileService` / `ProjectFileServiceImpl`, `ProjectFileRepository`, `ProjectFileMapper` (evolved from earlier `FileService`).
- **Roles & permissions** — `ProjectRole` (**OWNER**, **EDITOR**, **VIEWER**) maps to **`ProjectPermission`** (view, edit, delete, manage members, view members); used by **`SecurityExpression`** and **`ProjectMemberRepository.findRoleByProjectIdAndUserId`**.

Configure the database and secrets locally: **`application.yml`** expects **`DB_PASSWORD`**, **`JWT_SECRET_KEY`**, **`OPENAI_API_KEY`** (for chat streaming), **`STRIPE_API_KEY`**, **`STRIPE_WEBHOOK_SECRET`**, and **`minio.*`** (URL + credentials; align the MinIO port with Docker or your install). **Do not commit real API keys** to version control.

## Completed work (explained)

This section describes **what is actually implemented end-to-end** today—not only REST routes, but persistence and behavior where applicable.

### 1. Application & configuration

- **Spring Boot 4** application (`LovableCloneApplication`) with **`spring-boot-starter-webmvc`**, **`spring-boot-starter-data-jpa`**, **`spring-boot-starter-security`**, and **`spring-boot-starter-validation`**.
- **PostgreSQL** via JDBC URL, username, and password in **`application.yml`** (adjust for your environment).
- **Hibernate** with `ddl-auto: update` so tables for mapped entities are created/updated against your database.
- **Lombok** + **MapStruct** (annotation processors configured in `pom.xml`).

#### Security (`security/`)

- **Stateless** HTTP sessions (`SessionCreationPolicy.STATELESS`), **CSRF disabled** (API + Bearer tokens).
- **`@EnableMethodSecurity`** — project services use **`@PreAuthorize`** with SpEL such as **`@security.canViewProject(#id)`**, **`@security.canEditProject(#id)`**, **`@security.canManageMembers(#projectId)`**, where **`security`** is the **`SecurityExpression`** bean. It resolves the current user from the JWT context and checks **`ProjectRole`** permissions via **`ProjectMemberRepository.findRoleByProjectIdAndUserId`**.
- **`POST /api/auth/signup`**, **`POST /api/auth/login`**, and **`POST /webhooks/**`** (Stripe signature verification) are **public** (`permitAll`). **Every other `/api/**` route** (including **`GET /api/auth/me`** and **`POST /api/chat/stream`**) requires a valid **`Authorization: Bearer <JWT>`**; **`JwtAuthFilter`** validates the token and sets **`JwtUserPrincipal`**. For **SSE/async** requests the filter still runs on **async dispatch** (`shouldNotFilterAsyncDispatch = false`) so the principal is not lost after the response commits. Controllers use **`@AuthenticationPrincipal JwtUserPrincipal`** (user id from token claims).
- **`BCryptPasswordEncoder`** for **password hashing** (signup and login).

#### API errors (`error/` + `GlobalExceptionHandler`)

- **`@RestControllerAdvice`** centralizes errors into an **`ApiError`** JSON payload.
- Handles **`BadRequestException`**, **`ResourceNotFoundException`**, **`MethodArgumentNotValidException`** (field errors), malformed JSON, unsupported HTTP methods, **`DataIntegrityViolationException`** (e.g. conflicts), plus a generic fallback for uncaught exceptions.

### 2. JPA domain model (partial)

- **`User`** — `@Entity` on table `users`: id, **`username`**, **password** (stored **BCrypt-hashed**), name, timestamps, optional soft-delete `deletedAt`.
- **`Project`** — `@Entity` on `projects`: id, name, visibility, timestamps, soft-delete; **no `owner` column** — ownership and roles live in **`ProjectMember`** (including **`OWNER`**).
- **`ProjectMember`** — `@Entity` with **`@EmbeddedId` `ProjectMemberId`**, links to **`Project`** and **`User`**, **`ProjectRole`** (`OWNER`, `EDITOR`, `VIEWER`) each carrying a set of **`ProjectPermission`** values, invite/accept times.
- Other classes under `entity/` (e.g. chat, preview, plan) exist as domain shapes; **not all are mapped or used in live flows yet.**

### 3. Repositories

- **`UserRepository`** — `JpaRepository<User, Long>` plus **`findByUsername`** (signup uniqueness, invitations).
- **`ProjectRepository`** — `JpaRepository<Project, Long>` with:
  - **`findAllAccessibleByUser(userId)`** — lists **non-deleted** projects where the user has a **`project_members`** row: `INNER JOIN ProjectMember` on composite id (`projectId`, `userId`), plus an **`EXISTS`** guard on the same membership, **`DISTINCT`**, ordered by **`updatedAt`** descending.
  - **`findAccessibleProjectById(projectId, userId)`** — returns **`Optional<Project>`** only when the project exists, **`deletedAt` is null**, and the user is a member (same join + **`EXISTS`** pattern). Used by **`ProjectServiceImpl`** for get/update/delete so access rules stay in the repository layer.
- **`ProjectMemberRepository`** — `JpaRepository<ProjectMember, ProjectMemberId>`; **`findByIdProjectId`**, **`findRoleByProjectIdAndUserId`** (for **`SecurityExpression`** permission checks).

### 4. Mapping layer

- **`UserMapper`** — signup DTO → **`User`**, entity → **`UserProfileResponse`**.
- **`ProjectMapper`** — **`Project`** → **`ProjectResponse`** / **`ProjectSummaryResponse`** (and lists); **`owner`** on **`ProjectResponse`** is currently **ignored** in MapStruct — extend mapping if the API should return an owner object.
- **`ProjectMemberMapper`** — **`ProjectMember`** → **`MemberResponse`**.

### 5. Authentication

- **`AuthServiceImpl`** — **`signup`** and **`login`**: validate credentials, **JWT access token** via **`AuthUtil`**, **`AuthResponse`** includes token + profile.
- **`UserServiceImpl.getProfile`** — loads **`User`** by id and returns **`UserProfileResponse`** (used by **`GET /api/auth/me`** with the authenticated principal).

### 6. Project service (fully implemented vertical)

**`ProjectServiceImpl`** is **`@Transactional`** and uses the repositories + mapper to:

| Operation | Behavior |
|-----------|----------|
| **Create** | Loads **`User`** by id, **saves** a new **`Project`**, then creates a **`ProjectMember`** row with role **`OWNER`** for that user (membership-based access). Returns **`ProjectResponse`**. |
| **List** | Uses **`ProjectRepository.findAllAccessibleByUser`** → mapped list of **`ProjectSummaryResponse`**. |
| **Get** | **`@PreAuthorize("@security.canViewProject(#id)")`** — **`ProjectRepository.findAccessibleProjectById`**; if empty, **`ResourceNotFoundException`**. |
| **Update / soft-delete** | **`@PreAuthorize("@security.canEditProject(#id)")`** — same repository lookup as get; soft-delete sets **`deletedAt`**. |

Access is **membership-based** via **`ProjectMember`**, not a denormalized `owner` field on **`Project`**. Method security enforces **view** vs **edit** using **`ProjectPermission`** on the caller’s role.

### 7. Project member service (implemented)

**`ProjectMemberServiceImpl`** uses **`ProjectMemberRepository`**, **`ProjectRepository`**, **`UserRepository`**, and **`ProjectMemberMapper`**:

| Operation | Behavior |
|-----------|----------|
| **List members** | **`@PreAuthorize("@security.canViewProject(#projectId)")`** — membership + **`ResourceNotFoundException`** / **`AccessDeniedException`** where applicable; returns **all `ProjectMember` rows** for the project. |
| **Invite** | **`@PreAuthorize("@security.canManageMembers(#projectId)")`** — invitee by **`username`**, **`BadRequestException`** for self-invite or duplicate; **saves** `ProjectMember`. |
| **Update role** | **`@PreAuthorize("@security.canManageMembers(#projectId)")`** — updates **`ProjectRole`** for the target member. |
| **Remove** | **`@PreAuthorize("@security.canViewProject(#projectId)")`** — **`OWNER`** may remove others; any member may **remove themselves**; otherwise **`AccessDeniedException`**; **HTTP 204** on success. |

### 8. HTTP API for projects

**`ProjectController`** exposes:

- `GET /api/projects` — list projects for the **authenticated** user (JWT).
- `GET /api/projects/{id}` — get one project.
- `POST /api/projects` — create.
- `PATCH /api/projects/{id}` — update.
- `DELETE /api/projects/{id}` — soft delete.

### 9. HTTP API for members

**`ProjectMemberController`** exposes:

- `GET /api/projects/{projectId}/members` — list members.
- `POST /api/projects/{projectId}/members` — invite by **username** + role.
- `PATCH /api/projects/{projectId}/members/{memberId}` — change role.
- `DELETE /api/projects/{projectId}/members/{memberId}` — remove member (or leave project).

**Projects** and **project members** are the main areas where **controllers → service → mapper → DB** are wired end-to-end. **Auth** issues JWTs; protected routes use **`@AuthenticationPrincipal`**.

### 10. What exists but is not “done” yet

- **Billing** — **Stripe** checkout URLs (**`PaymentProcessor`** / **`StripePaymentProcessor`**), **`PaymentConfig`**, and **`POST /webhooks/payment`** are wired; **customer portal** and deep subscription persistence may still be partial; **set Stripe env vars** before calling live APIs.
- **Files** — file tree/content APIs exist; **MinIO** is configurable for persistence; behavior may still be partial vs. production file storage.
- **Chat** — **`POST /api/chat/stream`** (SSE) is implemented with Spring AI + OpenAI; **chat session CRUD**, **history**, and **retry** flows are not fully implemented yet.
- **Preview, ZIP download** — preview runner and **`download-zip`** are not implemented.

---

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
| Billing | `POST` | `/api/payments/checkout` |
| Billing | `POST` | `/api/payments/portal` |
| Webhooks | `POST` | `/webhooks/payment` |
| Usage | `GET` | `/api/usage/today` |
| Usage | `GET` | `/api/usage/limits` |
| Chat (AI) | `POST` | `/api/chat/stream` — **SSE** (`text/event-stream`); JSON body **`{ "message": "...", "projectId": <long> }`** + **`Authorization: Bearer <JWT>`** |

## Domain model (conceptual)

```
User ──< ProjectMember (OWNER / EDITOR / VIEWER) >── Project ──< ProjectFile
                                                         │
              ├──< ChatSession ──< ChatMessage
              ├──< Preview
              └──< UsageLog

Plan ──< Subscription ──> User
```

## Prerequisites

- **Java 21**
- **Maven** or `./mvnw` / `mvnw.cmd`
- **PostgreSQL** (local install or Docker)
- **OpenAI API key** if you use **`POST /api/chat/stream`** (outbound HTTPS/DNS must work from the machine running the app)
- **MinIO** (optional for file features; Docker compose below includes it)

## Local dependencies (Docker)

From the repo root:

```bash
docker compose -f services.docker-compose.yml up -d
```

This starts:

- **PostgreSQL (pgvector)** — host port **`9010`** → container `5432`, DB `pgvector-test`, user `user` (set **`DB_PASSWORD`** to match **`POSTGRES_PASSWORD`** in the compose file, e.g. `password`).
- **MinIO** — API on host **`9002`** (mapped from container `9000`), console on **`9001`**. Set **`minio.url`** in **`application.yml`** to **`http://localhost:9002`** when using this compose file (adjust if you run MinIO elsewhere).

## Configuration

Secrets and environment-specific values belong in env vars or a local override—not committed keys.

**Required / common variables**

| Variable | Purpose |
|----------|---------|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET_KEY` | Signing key for JWT access tokens |
| `OPENAI_API_KEY` | OpenAI API for chat streaming |
| `STRIPE_API_KEY` / `STRIPE_WEBHOOK_SECRET` | Stripe (if using billing webhooks/checkout) |

**Example shape** (see committed **`application.yml`** for the live structure):

```yaml
spring:
  application:
    name: lovable-clone
  datasource:
    url: jdbc:postgresql://localhost:9010/pgvector-test
    username: user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}

jwt:
  secret-key: ${JWT_SECRET_KEY}

stripe:
  api:
    secret: ${STRIPE_API_KEY}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}

client.url: http://localhost:8080

minio:
  url: http://localhost:9002
  access-key: minioadmin
  secret-key: minioadmin123
  project-bucket: projects
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
├── config/
├── controller/
├── dto/
├── entity/
├── enums/
├── error/
├── llm/
├── mapper/
├── repository/
├── security/
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

- [x] Login *(route: `POST /api/auth/login` — JWT + `UserDetailsService`)*
- [x] Sign up *(route: `POST /api/auth/signup` — persists user + BCrypt + JWT)*
- [x] Get my profile *(route: `GET /api/auth/me` — requires Bearer token)*

**AI code generation**

- [ ] List chat sessions
- [ ] Create new chat session
- [ ] Load full chat history
- [x] Chat stream (SSE) — **`POST /api/chat/stream`** with Bearer JWT + JSON **`message`**, **`projectId`**
- OpenAI calls require **`OPENAI_API_KEY`** and working DNS/network to the provider.
- [ ] Retry if failed

**Files**

- [ ] Get file tree & metadata *(route exists; service/storage integration evolving)*
- [ ] Get file content *(route exists; service/storage integration evolving)*
- [ ] Download all files as ZIP *(no `.../download-zip` route yet)*

**Preview**

- [ ] Get project preview
- [ ] Get logs stream (SSE)

### Additional features

- [x] Payment (Stripe) *(checkout session + webhook endpoint; configure API and webhook secrets via env)*
- [ ] Quota management — plans FREE | PRO
- [ ] Token usage & previews-running quotas
- [ ] Rate limiting (e.g. Redis)
- [ ] Zipkin tracing
- [x] Members — one project, many users *(invite by **username**, roles, remove; see **Project member service** above)*

### Core APIs — implementation snapshot

| Area | Spec | In codebase |
|------|------|-------------|
| Auth | `POST /api/auth/login`, `POST /api/auth/signup`, `GET /api/auth/me` | JWT auth; signup/login/me implemented |
| Projects | CRUD `/api/projects/{id}`, `GET /api/projects` | Yes (projects CRUD + list) |
| Files | Tree, file by path, `GET .../download-zip` | Tree + path routes · **`ProjectFileService`** / MinIO config · **no** download-zip |
| Members | `GET/POST/PATCH/DELETE` `/api/projects/{id}/members...` | Yes (persistence + rules) |
| Plans & billing | `GET /api/plans`, `GET /api/me/subscription`, `POST /api/payments/checkout`, `POST /api/payments/portal`, `POST /webhooks/payment` | Stripe SDK + checkout; webhook handler; subscription/portal evolving |
| Usage | `GET /api/usage/today`, `GET /api/usage/limits` | Routes yes · logic no |
| Chat & AI | `.../chat-sessions`, messages, `POST /api/chat/stream` (SSE) | **SSE chat implemented**; session/history APIs **not yet** |
| Preview & runner | `POST .../preview`, preview status, logs SSE, `DELETE .../preview` | **Not implemented** |

### Future work

- [x] Orientation & Spring Boot project setup
- [x] Entity classes & REST scaffolding
- [x] MapStruct & `ProjectService`
- [x] Project member management
- [x] Global exception handling & validation errors *(see `GlobalExceptionHandler`)*
- [x] Spring Security + JWT *(password encoder, `JwtAuthFilter`, protected `/api/**` except auth)*
- [x] Stripe integration & webhooks *(baseline: checkout, `PaymentConfig`, `POST /webhooks/payment`)*
- [x] AI (OpenAI streaming) + MinIO wiring *(baseline: Spring AI + `StorageConfig`)*
- [ ] Chat sessions/history APIs, previews, K8s, full distributed architecture

---

## Repository

**GitHub:** [github.com/Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## License

Educational and personal use.
