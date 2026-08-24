# FlowOps

**Build workflows. Let AI run the work.**

FlowOps is a production-grade, multi-tenant **AI workflow automation** platform: a
visual builder, a real execution engine with live monitoring, and AI that can draft
workflows from natural language — all backed by real auth, RBAC, and encrypted
credentials. No fake functionality: everything visible either works or is clearly
marked unavailable.

> **Status — M0 (foundation).** Three service skeletons boot and answer health
> checks. The vertical spine lands next, milestone by milestone (see
> [Roadmap](#roadmap)). Each milestone is built and verified before the next.

## Architecture

Three isolated, independently deployable services. The browser talks **only** to the
Spring Boot API; the API brokers every AI call to the Python service.

```
  Next.js Web (TS)               Spring Boot API                 Python AI Service
  :3000                          :8080                           :8100
  builder, dashboards   ──HTTP/JSON + SSE──▶  auth · RBAC · workflows   ──HTTP──▶  LangGraph
  TanStack Query, Zod                         execution engine                    OpenAI-compatible
  React Flow                                  ├──▶ PostgreSQL 16 :5432             (bring-your-own-key)
                                              └──▶ Redis :6379 (optional queue)
```

| Service      | Stack                                   | Port | Purpose                                        |
|--------------|-----------------------------------------|------|------------------------------------------------|
| `frontend/`  | Next.js 14 · TypeScript · Tailwind · shadcn/ui · React Flow | 3000 | Landing, auth, visual builder, monitoring UI   |
| `backend/`   | Java 21 · Spring Boot 3.4 · Maven        | 8080 | API, auth/JWT, RBAC, workflows, execution engine |
| `ai-service/`| Python 3.12 · FastAPI · LangGraph        | 8100 | Workflow generation & agent runs (BYO key)     |
| postgres     | PostgreSQL 16 (+pgvector)                | 5432 | System of record                               |
| redis        | Redis 7 (**optional**)                   | 6379 | Distributed execution queue (in-memory default)|

## Prerequisites

- **JDK 21** and **Maven 3.9+**  (backend)
- **Node.js 20+** and npm  (frontend)
- **Python 3.11 or 3.12**  (ai-service — 3.13/3.14 may lack some AI-lib wheels)
- **PostgreSQL 16**  (needed from M1 onward; M0 health checks don't require it)
- *Optional:* Docker Desktop (full-stack compose) and Redis (distributed queue)

macOS (Homebrew) one-time setup:

```bash
brew install openjdk@21 maven node python@3.12 postgresql@16
brew services start postgresql@16
```

## Quick start — local (no Docker)

Run each service in its own terminal. This is the primary development workflow.

**0. Environment**

```bash
cp .env.example .env
```

**1. Database** (from M1 onward; skip for the M0 health-check smoke test)

```bash
createdb flowops
psql -d flowops -c "CREATE ROLE flowops LOGIN PASSWORD 'flowops'; GRANT ALL ON DATABASE flowops TO flowops;"
```

**2. Backend — Spring Boot** → http://localhost:8080

```bash
cd backend
mvn spring-boot:run
```

**3. AI service — FastAPI** → http://localhost:8100

```bash
cd ai-service
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp ../.env.example .env    # or create ai-service/.env with the OPENAI_* / AI_SERVICE_TOKEN vars
uvicorn app.main:app --reload --port 8100
```

**4. Frontend — Next.js** → http://localhost:3000

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

### Verify (M0)

| Check | Command / URL | Expected |
|-------|---------------|----------|
| Backend health | `curl http://localhost:8080/health` | `{"status":"ok","service":"flowops-api",...}` |
| Backend readiness | `curl http://localhost:8080/health/ready` | `{"status":"ready","checks":{"api":"ok"},...}` |
| API docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| AI health | `curl http://localhost:8100/health` | `{"status":"ok","ai_configured":false,...}` |
| Web | http://localhost:3000 | FlowOps landing page (dark theme, animated diagram) |

`ai_configured` is honest: it's `false` until you set `OPENAI_API_KEY`. AI endpoints
never fabricate output when unconfigured.

## Quick start — Docker

Requires Docker Desktop. Builds and runs all five services.

```bash
cp .env.example .env
docker compose up -d --build

curl http://localhost:8080/health     # backend
curl http://localhost:8100/health     # ai-service
open  http://localhost:3000           # web
```

Stop with `docker compose down` (add `-v` to drop the Postgres volume).

## Configuration

`.env.example` is the canonical, fully-commented reference. Highlights:

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Backend Postgres connection (M1+). Defaults to localhost. |
| `JWT_SECRET` | JWT signing secret (M1). ≥32 chars — `openssl rand -base64 48`. |
| `ENCRYPTION_KEY` | AES-256-GCM key for stored credentials (M5). Base64 32 bytes — `openssl rand -base64 32`. |
| `REDIS_HOST` / `REDIS_PORT` | Optional distributed queue (M3). In-memory if unset. |
| `AI_SERVICE_URL` / `AI_SERVICE_TOKEN` | Backend → ai-service address + shared secret (M4). |
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL` | Bring-your-own-key AI provider. Empty ⇒ AI features report "not configured". |
| `NEXT_PUBLIC_API_URL` | Backend URL exposed to the browser. |

**Secrets never leave the server.** Credentials are encrypted at rest, masked in
responses, and never logged. Never commit `.env`.

## Project structure

```
flowops/
├── docker-compose.yml        # postgres · redis · backend · ai-service · frontend
├── .env.example              # canonical env reference
├── backend/                  # Spring Boot API (Java 21, Maven)
│   ├── pom.xml
│   └── src/main/java/com/flowops/
│       ├── FlowOpsApplication.java
│       ├── config/           # CORS, OpenAPI (+ security, JPA from M1)
│       └── common/health/    # /health, /health/ready
├── frontend/                 # Next.js App Router (TypeScript)
│   ├── app/                  # layout, landing page, globals.css
│   ├── components/           # ui/ (shadcn) · brand/ · marketing/
│   └── lib/                  # utils
└── ai-service/               # FastAPI + LangGraph (Python)
    └── app/                  # main.py, config.py
```

## Testing

```bash
cd backend    && mvn test                              # JUnit + MockMvc
cd frontend   && npm run lint && npm run typecheck      # ESLint + tsc
cd ai-service && source .venv/bin/activate && pytest -v # pytest
```

## Roadmap

Built and verified milestone by milestone — the vertical spine first, everything
shipped actually runs.

- **M0 — Foundation** *(current)*: 3-service skeletons, health checks, landing page, Docker.
- **M1 — Auth & multi-tenancy**: register/login (JWT), organizations, RBAC (Owner/Admin/Member/Viewer), app shell.
- **M2 — Workflows & builder**: workflow CRUD, React Flow visual builder, validation, immutable versioning.
- **M3 — Execution engine & monitoring**: real node executors, queue (in-memory/Redis), live SSE monitoring, dashboards.
- **M4 — AI**: natural-language → workflow graph, reusable AI agents, allowlisted tool registry.
- **M5 — Breadth**: integrations + encrypted credentials, secure webhooks, templates, team, settings, logs, ⌘K palette.
- **M6 — Hardening**: full test suites, docs, production sweep.

## Security

BCrypt password hashing · JWT access+refresh · server-side tenant isolation (auth
context is never client-supplied) · AES-256-GCM credential encryption · rate
limiting · CORS · secure headers · input validation · masked secrets · audit
logging · allowlisted AI tools with explicit user confirmation before any generated
workflow runs. Details land with their milestones.
