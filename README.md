# 🚀 Lovable Clone — Full-Stack AI App Generator

<div align="center">

  <p align="center">
    <b>A powerful, full-stack AI-powered application generation platform inspired by <a href="https://lovable.dev">Lovable</a>.</b><br />
    Build, preview, and deploy full-stack applications instantly using natural language prompts.
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring_Boot-4.0-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot 4.0" />
    <img src="https://img.shields.io/badge/Next.js-15-blue?style=for-the-badge&logo=nextdotjs" alt="Next.js 15" />
    <img src="https://img.shields.io/badge/Tailwind-CSS-38B2AC?style=for-the-badge&logo=tailwindcss" alt="Tailwind CSS" />
    <img src="https://img.shields.io/badge/PostgreSQL-18-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL" />
    <img src="https://img.shields.io/badge/Docker-Supported-blue?style=for-the-badge&logo=docker" alt="Docker" />
  </p>
</div>

---

## ✨ Features

### 💻 Modern Frontend
- **🤖 Interactive AI Workspace**: Dynamic chat interface with real-time SSE streaming for instant code generation.
- **⚡ Live Sandpack Preview**: Real-time app preview and inline code editing powered by CodeSandbox Sandpack.
- **📊 Premium Dashboard**: Clean, modern dark-mode dashboard to organize and manage all your AI-generated projects.
- **💳 Stripe Subscription & Billing**: Complete subscription lifecycle management with Stripe Checkout, Webhooks, and Plan Comparisons.
- **👥 Real-Time Collaboration**: Invite team members, configure RBAC roles (Owner/Editor/Viewer), and build together.
- **🎨 Elite Dark Theme UI**: Sleek, high-fidelity dark-mode interface built with Tailwind CSS, Framer Motion, and Radix/Shadcn UI.

### ⚙️ Robust Backend
- **☕ Spring Boot 4 REST API**: High-throughput RestControllers built with Java 21 and Spring Web.
- **🧠 Spring AI Integration**: Native integration with OpenAI GPT models for smart code generation.
- **🔒 Stateful & Stateless Security**: JWT-based session filters and method-level access control (@EnableMethodSecurity).
- **📂 MinIO Hybrid File Storage**: High-performance object storage for project file trees with a PostgreSQL database fallback.



---

## 📋 API Route Reference

The backend runs on `http://localhost:8080` by default.

| Category | Endpoint | Method | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `/api/auth/login` | `POST` | Authenticate user & return JWT token |
| **Auth** | `/api/auth/signup` | `POST` | Create a new user account |
| **Projects**| `/api/projects` | `GET/POST` | List all user projects / Create a new project |
| **Projects**| `/api/projects/{id}` | `GET/DELETE` | Retrieve project details / Delete project |
| **Members** | `/api/projects/{id}/members` | `GET/POST` | List members / Invite member to project |
| **Files** | `/api/projects/{id}/files` | `GET/PUT` | Retrieve file tree / Update file contents |
| **Chat** | `/api/chat/stream` | `GET` | SSE endpoint for streaming AI code generation |
| **Billing** | `/api/payments/checkout` | `POST` | Create Stripe Checkout Session |
| **Usage** | `/api/usage/today` | `GET` | Retrieve daily token and preview slots usage |

---

## 🛠️ System Architecture & Implementation

Here is a deep dive into the engineering behind Lovable Clone:

### 1. Security & RBAC Guardrails (`security/`)
- Enforces strict role-based access control (RBAC). For example, a user must be a project member to see or edit its files.
- Uses `@security.canViewProject(#id)` and `@security.canEditProject(#id)` inside method security annotations.
- Implements custom `JwtAuthFilter` that properly registers the auth principal during async requests (like Server-Sent Events chat stream).

### 2. Streamlined AI Code Generation (`llm/`, `service/`)
- Integrates **Spring AI** to construct multi-file code responses.
- Implements **LlmResponseParser** to parse LLM outputs into structured project files in real-time.
- Supports streaming of file updates over Server-Sent Events (SSE).

### 3. Subscription & Usage Limits (`stripe/`, `service/`)
- Automatically seeds default billing plans into the PostgreSQL database at startup via `BillingPlansInitializer`.
- Monitors daily tokens and active preview usage slots, preventing abuse on free tiers.

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** & Maven
- **Node.js 20+**
- **Docker & Docker Compose**
- **OpenAI API Key** (for code generation)

### 1. Launch Infrastructure
Start PostgreSQL (with pgvector support) and MinIO services:
```bash
docker compose -f services.docker-compose.yml up -d
```

### 2. Backend Setup
Create your local configuration profile. Copy the template and add your API keys:
```bash
cp application-local.yml.example src/main/resources/application-local.yml
```
Run the Spring Boot application (using local profile with timezone override to prevent PG timezone conflicts):
```bash
$env:DB_PASSWORD="password"; $env:OPENAI_API_KEY="your-openai-key"; $env:SPRING_PROFILES_ACTIVE="local"; $env:MAVEN_OPTS="-Duser.timezone=UTC"; .\mvnw.cmd spring-boot:run
```

### 3. Frontend Setup
Navigate to the frontend directory, install dependencies, and launch the Next.js development server:
```bash
cd frontend
npm install
npm run dev
```

Visit the app at **`http://localhost:5173`**.

---

## 📈 Roadmap & Progress
- [x] **Auth & RBAC**: JWT login/signup with project-level permissions.
- [x] **Project Management**: Project creation, deletion, and dashboard UI.
- [x] **AI App Generator**: SSE streaming code generation with file tree persistence.
- [x] **Sandpack Sandbox**: Live execution of React/Next.js files in-browser.
- [x] **Stripe Integration**: Plan seeding, checkout redirects, and portal management.
- [x] **Usage Guardrails**: Token quotas and active preview limits.
- [x] **Team Collaboration**: Invite system with Owner, Editor, Viewer roles.
- [ ] **One-Click Deployments**: Direct deployment of generated apps to Vercel/Netlify.

---

## 🔗 Project Info & License
- **Repository:** [Snehil208001/Lovable_Clone](https://github.com/Snehil208001/Lovable_Clone)
- **License:** Educational and personal use only.

