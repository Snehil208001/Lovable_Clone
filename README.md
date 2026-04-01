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

## 🛠️ Tech Stack

### Frontend
- **Core**: Next.js 14, TypeScript, React
- **Styling**: Tailwind CSS, Lucide React, Framer Motion
- **UI Components**: Shadcn/UI, Radix UI
- **State Management**: Zustand
- **Preview**: Sandpack React

### Backend
- **Core**: Java 21, Spring Boot 4.0.4
- **Security**: Spring Security, JWT (Stateless)
- **Data**: Spring Data JPA, PostgreSQL, pgvector
- **AI**: Spring AI (OpenAI transport)
- **Storage**: MinIO (S3 compatible)
- **Payments**: Stripe Java SDK

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

## 🚀 Getting Started

### Prerequisites
- Java 21
- Node.js 20+
- Docker (for PostgreSQL & MinIO)
- OpenAI API Key
- Stripe API Key (optional for billing)

### 1. Setup Infrastructure
```bash
docker compose -f services.docker-compose.yml up -d
```

### 2. Backend Setup
Copy `src/main/resources/application.yml` and configure your environment variables:
- `OPENAI_API_KEY`
- `JWT_SECRET_KEY`
- `STRIPE_API_KEY`
- `DB_PASSWORD`

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
- [ ] **Custom Domain**: Assign custom domains to project previews.

## 🔗 Repository
**GitHub:** [Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)

## 📄 License
Educational and personal use only.
