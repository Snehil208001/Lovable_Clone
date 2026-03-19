# Lovable Clone — Backend

A Spring Boot backend that replicates the core architecture of [Lovable](https://lovable.dev), the AI-powered web-app builder. This project models the full domain — users, projects, AI chat, live previews, and subscription billing — as a foundation for building an end-to-end AI code-generation platform.

## Tech Stack

| Layer       | Technology             |
|-------------|------------------------|
| Language    | Java 21                |
| Framework   | Spring Boot 4.0.4      |
| Web         | Spring Web MVC         |
| Persistence | Spring Data JPA        |
| Database    | PostgreSQL             |
| Build       | Maven (with wrapper)   |
| Utilities   | Lombok                 |

## Domain Model

```
User ──< Project ──< ProjectFile
              │
              ├──< ProjectMember (EDITOR / VIEWER)
              │
              ├──< ChatSession ──< ChatMessage (USER / ASSISTANT / SYSTEM / TOOL)
              │
              ├──< Preview (CREATING / RUNNING / FAILED / TERMINATED)
              │
              └──< UsageLog

Plan ──< Subscription ──> User
```

### Key Entities

| Entity           | Purpose                                                        |
|------------------|----------------------------------------------------------------|
| **User**         | Authentication, profile, soft-delete support                   |
| **Project**      | Workspace with visibility settings, owned by a user            |
| **ProjectFile**  | Files stored in MinIO (object-key reference)                   |
| **ProjectMember**| Role-based collaboration (Editor / Viewer)                     |
| **ChatSession**  | Per-project AI conversation thread                             |
| **ChatMessage**  | Individual messages with role, token counts, and tool-call IDs |
| **Preview**      | Live deployment preview (namespace, pod, URL)                  |
| **Subscription** | Stripe-backed billing tied to a plan                           |
| **Plan**         | Defines limits — max projects, tokens/day, previews, AI access |
| **UsageLog**     | Tracks actions, token usage, and duration                      |

## Prerequisites

- **Java 21** or later
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **PostgreSQL** instance running and accessible

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/Snehil208001/Lovable_Clone.git
   cd Lovable_Clone
   ```

2. **Configure the database**

   Update `src/main/resources/application.yml` with your PostgreSQL connection details:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/lovable_clone
       username: your_username
       password: your_password
     jpa:
       hibernate:
         ddl-auto: update
   ```

3. **Build and run**

   ```bash
   ./mvnw spring-boot:run
   ```

   The application will start on `http://localhost:8080`.

## Project Structure

```
src/main/java/com/snehil/project/lovable_clone/
├── LovableCloneApplication.java      # Entry point
├── entity/                            # JPA entities
│   ├── User.java
│   ├── Project.java
│   ├── ProjectFile.java
│   ├── ProjectMember.java
│   ├── ProjectMemberId.java
│   ├── ChatSession.java
│   ├── ChatMessage.java
│   ├── Preview.java
│   ├── Subscription.java
│   ├── Plan.java
│   └── UsageLog.java
└── enums/
    ├── MessageRole.java
    ├── PreviewStatus.java
    ├── ProjectRole.java
    └── SubscriptionStatus.java
```

## Roadmap

- [ ] Repository & service layers
- [ ] REST API controllers
- [ ] Authentication & authorization (Spring Security + JWT)
- [ ] AI chat integration (OpenAI / Anthropic)
- [ ] MinIO file storage integration
- [ ] Kubernetes-based preview deployments
- [ ] Stripe billing webhooks
- [ ] WebSocket support for real-time chat

## License

This project is for educational and personal use.
