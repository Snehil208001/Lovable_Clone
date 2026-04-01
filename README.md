# Lovable Clone — Full Stack AI App Generator

A powerful, full-stack AI-powered application generation platform inspired by [Lovable](https://lovable.dev). Build, preview, and deploy applications instantly using natural language.

## 🚀 Features

### 💻 Modern Frontend
- **AI Workspace**: Interactive chat interface with real-time SSE streaming for app generation.
- **Sandpack Preview**: Instant live preview and code editing powered by CodeSandbox Sandpack.
- **Project Dashboard**: Clean, premium dashboard to manage all your AI-generated projects.
- **Billing & Usage**: Integrated Stripe subscription management, usage tracking (tokens/previews), and plan comparisons.
- **Team Collaboration**: Invite members, manage roles (Owner/Editor/Viewer), and collaborate in real-time.
- **Responsive Design**: Premium dark-mode UI built with Tailwind CSS, Framer Motion, and Shadcn/UI.

### ⚙️ Robust Backend
- **Spring Boot 4 API**: High-performance RESTful services built with Java 21.
- **Spring AI Integration**: Orchestrating OpenAI's GPT models for intelligent code generation.
- **Secure Auth**: JWT-based authentication with role-based access control (RBAC).
- **Stripe Payments**: End-to-end subscription lifecycle management (Checkout, Webhooks, Customer Portal).
- **Scalable Storage**: MinIO/S3 object storage for project files with a PostgreSQL fallback.

## 📋 API Overview

Base URL: `http://localhost:8080`

| Area | Path | Description |
|------|------|-------------|
| Auth | `/api/auth/**` | Login, Signup, Profile |
| Projects | `/api/projects/**` | CRUD projects & settings |
| Members | `/api/projects/{id}/members/**` | Team management |
| Files | `/api/projects/{id}/files/**` | File tree and content |
| Chat | `/api/chat/stream` | AI SSE code generation |
| Billing | `/api/payments/**` | Stripe Checkout & Portal |
| Usage | `/api/usage/**` | Token & preview monitoring |

## 🛠️ Completed Work (Deep Dive)

This section describes **what is actually implemented end-to-end** today—not only REST routes, but persistence and behavior where applicable.

### 1. Application & Configuration
- **Spring Boot 4** application with Web, Data JPA, Security, and Validation.
- **PostgreSQL** via JDBC (pgvector supported).
- **Hibernate** with `ddl-auto: update` for automatic schema management.
- **Lombok + MapStruct** for boilerplate reduction and clean DTO mapping.

### 2. Security (`security/`)
- **Stateless** JWT sessions with **Spring Security**.
- **@EnableMethodSecurity**: RBAC enforced via SpEL (e.g., `@security.canViewProject(#id)`).
- **Async Support**: `JwtAuthFilter` correctly handles SSE/async dispatches to keep the principal active during long streams.

### 3. Repositories & Domain Model
- **Membership-based Access**: Projects are owned/accessed via the `ProjectMember` entity, allowing for real multi-user collaboration.
- **Optimized Queries**: Repository methods use `EXISTS` and `JOIN` patterns to ensure secure project access at the database level.

### 4. Billing & Payments
- **Stripe Integration**: Checkout sessions, customer portal creation, and webhook handling.
- **Plan Seeding**: `BillingPlansInitializer` automatically seeds plans into the database on startup.

### 5. AI & Storage
- **Streaming Chat**: SSE implementation using Spring AI and OpenAI.
- **Hybrid Storage**: `ProjectFileService` uploads to MinIO with a PostgreSQL content fallback for maximum reliability.

## 🚀 Getting Started

### Prerequisites
- Java 21
- Node.js 20+
- Docker (for PostgreSQL & MinIO)
- OpenAI API Key

### 1. Setup Infrastructure
```bash
docker compose -f services.docker-compose.yml up -d
```

### 2. Backend Setup
Configure your environment variables (or `application.yml`):
- `OPENAI_API_KEY`, `JWT_SECRET_KEY`, `STRIPE_API_KEY`, `DB_PASSWORD`

```bash
./mvnw spring-boot:run
```

### 3. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

## 📈 Roadmap & Progress

- [x] **Auth & RBAC**: JWT login/signup with project-level permissions.
- [x] **Project Management**: Creation, deletion, and settings UI.
- [x] **AI Generation**: SSE streaming with file-system persistence.
- [x] **Stripe Integration**: Plan seeding, checkout, and portal flows.
- [x] **Usage Guardrails**: Daily token limits and preview slot tracking.
- [x] **Team Management**: Invite system with role updates.
- [ ] **Deployment**: One-click Vercel/Netlify integration.

## 🔗 Repository
**GitHub:** [Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## 📄 License
Educational and personal use only.
