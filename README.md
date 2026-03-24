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
| Other | Lombok |

## What’s in the repo

- **REST API** — auth, projects, members, files, plans, subscription/checkout stubs, usage.
- **DTOs** — request/response records under `dto/` (with validation annotations where used).
- **Entities & enums** — domain model under `entity/` and `enums/`.
- **Repositories** — `UserRepository`, `ProjectRepository`, `ProjectMemberRepository`.
- **Services** — interfaces in `service/` with implementations in `service/impl/`.
- **Mappers** — `ProjectMapper`, `ProjectMemberMapper`, `UserMapper` (MapStruct).
- **Errors** — `GlobalExceptionHandler`, `ApiError`, custom exceptions under `error/`.
- **Security** — `WebSecurityConfig`, `JwtAuthFilter`, `JwtAuthEntryPoint`, `JwtUserPrincipal`, `SecurityUtil`.

Configure the database in `src/main/resources/application.yml` (URL, user, password). Use your own credentials locally and avoid committing production secrets.

## Completed work (explained)

This section describes **what is actually implemented end-to-end** today—not only REST routes, but persistence and behavior where applicable.

### 1. Application & configuration

- **Spring Boot 4** application (`LovableCloneApplication`) with **`spring-boot-starter-webmvc`**, **`spring-boot-starter-data-jpa`**, **`spring-boot-starter-security`**, and **`spring-boot-starter-validation`**.
- **PostgreSQL** via JDBC URL, username, and password in **`application.yml`** (adjust for your environment).
- **Hibernate** with `ddl-auto: update` so tables for mapped entities are created/updated against your database.
- **Lombok** + **MapStruct** (annotation processors configured in `pom.xml`).

#### Security (`security/`)

- **Stateless** HTTP sessions (`SessionCreationPolicy.STATELESS`), **CSRF disabled** (API + Bearer tokens).
- **`POST /api/auth/signup`** and **`POST /api/auth/login`** are **public** (`permitAll`). **Every other `/api/**` route** (including **`GET /api/auth/me`**) requires a valid **`Authorization: Bearer <JWT>`**; **`JwtAuthFilter`** validates the token and sets **`JwtUserPrincipal`**. Controllers use **`@AuthenticationPrincipal JwtUserPrincipal`** (user id from token claims).
- **`BCryptPasswordEncoder`** for **password hashing** (signup and login).

#### API errors (`error/` + `GlobalExceptionHandler`)

- **`@RestControllerAdvice`** centralizes errors into an **`ApiError`** JSON payload.
- Handles **`BadRequestException`**, **`ResourceNotFoundException`**, **`MethodArgumentNotValidException`** (field errors), malformed JSON, unsupported HTTP methods, **`DataIntegrityViolationException`** (e.g. conflicts), plus a generic fallback for uncaught exceptions.

### 2. JPA domain model (partial)

- **`User`** — `@Entity` on table `users`: id, **`username`**, **password** (stored **BCrypt-hashed**), name, timestamps, optional soft-delete `deletedAt`.
- **`Project`** — `@Entity` on `projects`: id, name, visibility, timestamps, soft-delete; **no `owner` column** — ownership and roles live in **`ProjectMember`** (including **`OWNER`**).
- **`ProjectMember`** — `@Entity` with **`@EmbeddedId` `ProjectMemberId`**, links to **`Project`** and **`User`**, **`ProjectRole`** (`OWNER`, `EDITOR`, `VIEWER`), invite/accept times.
- Other classes under `entity/` (e.g. chat, preview, plan) exist as domain shapes; **not all are mapped or used in live flows yet.**

### 3. Repositories

- **`UserRepository`** — `JpaRepository<User, Long>` plus **`findByUsername`** (signup uniqueness, invitations).
- **`ProjectRepository`** — `JpaRepository<Project, Long>` with:
  - **`findAllAccessibleByUser(userId)`** — lists **non-deleted** projects where the user has a **`project_members`** row: `INNER JOIN ProjectMember` on composite id (`projectId`, `userId`), plus an **`EXISTS`** guard on the same membership, **`DISTINCT`**, ordered by **`updatedAt`** descending.
  - **`findAccessibleProjectById(projectId, userId)`** — returns **`Optional<Project>`** only when the project exists, **`deletedAt` is null**, and the user is a member (same join + **`EXISTS`** pattern). Used by **`ProjectServiceImpl`** for get/update/delete so access rules stay in the repository layer.
- **`ProjectMemberRepository`** — `JpaRepository<ProjectMember, ProjectMemberId>` for membership rows (invite, list, update role, delete).

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
| **Get / update / soft-delete** | Uses **`ProjectRepository.findAccessibleProjectById`** (membership + not soft-deleted). If empty, throws **`ResourceNotFoundException`** (same response shape for unknown id, deleted project, or non-member). |

Access is **membership-based** via **`ProjectMember`**, not a denormalized `owner` field on **`Project`**.

### 7. Project member service (implemented)

**`ProjectMemberServiceImpl`** uses **`ProjectMemberRepository`**, **`ProjectRepository`**, **`UserRepository`**, and **`ProjectMemberMapper`**:

| Operation | Behavior |
|-----------|----------|
| **List members** | Caller must be a **member** of the project; returns **all `ProjectMember` rows** for the project (mapped to DTOs). |
| **Invite** | Caller must be a **member**; resolves invitee by **`username`**, prevents duplicate membership and self-invite, **saves** `ProjectMember`. |
| **Update role** | Caller must be a **member**; updates **`ProjectRole`** for the target member. |
| **Remove** | **`OWNER`** may remove others; any member may **remove themselves**; **HTTP 204** on success. |

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

- **Files, subscription, Stripe, usage** — **controllers and DTOs exist**; several **`service/impl`** methods still return **`null`** or **empty lists**.
- **Chat, preview, ZIP download, SSE streams** — **not implemented** (no controllers for those spec paths yet).

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
| Billing | `POST` | `/api/stripe/checkout` |
| Billing | `POST` | `/api/stripe/portal` |
| Usage | `GET` | `/api/usage/today` |
| Usage | `GET` | `/api/usage/limits` |

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
├── error/
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
- [x] Members — one project, many users *(invite by **username**, roles, remove; see **Project member service** above)*

### Core APIs — implementation snapshot

| Area | Spec | In codebase |
|------|------|-------------|
| Auth | `POST /api/auth/login`, `POST /api/auth/signup`, `GET /api/auth/me` | JWT auth; signup/login/me implemented |
| Projects | CRUD `/api/projects/{id}`, `GET /api/projects` | Yes (projects CRUD + list) |
| Files | Tree, file by path, `GET .../download-zip` | Tree + path routes · stubs · **no** download-zip |
| Members | `GET/POST/PATCH/DELETE` `/api/projects/{id}/members...` | Yes (persistence + rules) |
| Plans & billing | `GET /api/plans`, `GET /api/me/subscription`, `POST /api/stripe/checkout`, `POST /api/stripe/portal` | Routes yes · logic no |
| Usage | `GET /api/usage/today`, `GET /api/usage/limits` | Routes yes · logic no |
| Chat & AI | `.../chat-sessions`, messages, `POST /api/chat/stream` (SSE) | **Not implemented** |
| Preview & runner | `POST .../preview`, preview status, logs SSE, `DELETE .../preview` | **Not implemented** |

### Future work

- [x] Orientation & Spring Boot project setup
- [x] Entity classes & REST scaffolding
- [x] MapStruct & `ProjectService`
- [x] Project member management
- [x] Global exception handling & validation errors *(see `GlobalExceptionHandler`)*
- [x] Spring Security + JWT *(password encoder, `JwtAuthFilter`, protected `/api/**` except auth)*
- [ ] Stripe integration & webhooks
- [ ] AI, MinIO, previews, K8s, distributed architecture

---

## Repository

**GitHub:** [github.com/Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## License

Educational and personal use.
