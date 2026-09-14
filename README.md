# FlowOps

**Build and operate workflows you can trust.**

FlowOps is a production-grade, multi-tenant **AI workflow automation** platform: a
visual builder, a real execution engine with live monitoring, anomaly detection,
and AI investigation — all backed by real auth, RBAC, encrypted credentials, and
encrypted webhook ingress. No fake functionality: everything visible either works
or is clearly marked unavailable.

> **Status — M6 (production-hardened).** All milestones M0–M5 complete. Reliability
> loop (Detect → Alert → Diagnose → Resolve → Monitor) is live. Ready for design-partner pilots.

## Architecture

Three isolated, independently deployable services. The browser talks **only** to the
Spring Boot API; the API brokers every AI call to the Python service.

```
  Next.js Web (TS)               Spring Boot API                 Python AI Service
  :3000                          :8080                           :8100
  builder, dashboards   ──HTTP/JSON + SSE──▶  auth · RBAC · workflows   ──HTTP──▶  Google Gemini
  TanStack Query, Zod                         execution engine                    google-genai SDK
  React Flow                                  ├──▶ PostgreSQL 16 :5432             (bring-your-own-key)
                                              └──▶ Redis :6379 (optional queue)
```

| Service      | Stack                                   | Port | Purpose                                        |
|--------------|-----------------------------------------|------|------------------------------------------------|
| `frontend/`  | Next.js 14 · TypeScript · Tailwind · shadcn/ui · React Flow · Framer Motion | 3000 | Landing, auth, visual builder, monitoring, reliability UI   |
| `backend/`   | Java 21 · Spring Boot 3.4 · Maven        | 8080 | API, auth/JWT, RBAC, workflows, execution engine, anomaly detection, webhooks |
| `ai-service/`| Python 3.12 · FastAPI · google-genai      | 8100 | Workflow generation, agent runs, anomaly investigation (BYO key) |
| postgres     | PostgreSQL 16 (+pgvector)                | 5432 | System of record                               |
| redis        | Redis 7 (**optional**)                   | 6379 | Distributed execution queue (in-memory default)|

## Prerequisites

- **JDK 21** and **Maven 3.9+**  (backend)
- **Node.js 20+** and npm  (frontend)
- **Python 3.11 or 3.12**  (ai-service — 3.13/3.14 may lack some AI-lib wheels)
- **PostgreSQL 16**  (required from M1 onward)
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

**1. Database**

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
cp ../.env.example .env    # or create ai-service/.env with the GEMINI_* / AI_SERVICE_TOKEN vars
uvicorn app.main:app --reload --port 8100
```

**4. Frontend — Next.js** → http://localhost:3000

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

### Verify

| Check | Command / URL | Expected |
|-------|---------------|----------|
| Backend health | `curl http://localhost:8080/health` | `{"status":"ok","service":"flowops-api",...}` |
| Backend readiness | `curl http://localhost:8080/health/ready` | `{"status":"ready","checks":{"api":"ok","db":"ok","queue":"ok"},...}` |
| API docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| AI health | `curl http://localhost:8100/health` | `{"status":"ok","ai_configured":false,...}` |
| Web | http://localhost:3000 | FlowOps landing page (violet/indigo theme, Framer Motion animations) |

`ai_configured` is honest: it's `false` until you set `GEMINI_API_KEY`. AI endpoints
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
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Backend Postgres connection. Defaults to localhost. |
| `JWT_SECRET` | JWT signing secret. ≥32 chars — `openssl rand -base64 48`. |
| `ENCRYPTION_KEY` | AES-256-GCM key for stored credentials. Base64 32 bytes — `openssl rand -base64 32`. |
| `REDIS_HOST` / `REDIS_PORT` | Optional distributed queue. In-memory if unset. |
| `AI_SERVICE_URL` / `AI_SERVICE_TOKEN` | Backend → ai-service address + shared secret. |
| `GEMINI_API_KEY` / `GEMINI_MODEL` | Bring-your-own-key Google Gemini. Empty ⇒ AI features report "not configured". |
| `NEXT_PUBLIC_API_URL` | Backend URL exposed to the browser. |

**Secrets never leave the server.** Credentials are encrypted at rest, masked in
responses, and never logged. Never commit `.env`.

## Project structure

```
flowops/
├── docker-compose.yml        # postgres · redis · backend · ai-service · frontend
├── .env.example              # canonical env reference
├── .github/workflows/ci.yml  # GitHub Actions CI pipeline
├── backend/                  # Spring Boot API (Java 21, Maven)
│   ├── pom.xml
│   └── src/main/java/com/flowops/
│       ├── FlowOpsApplication.java
│       ├── config/           # CORS, OpenAPI, security, JPA
│       ├── auth/             # JWT, sessions, RBAC
│       ├── workflow/         # CRUD, builder, validation, versioning
│       ├── execution/        # engine, executors, queue, SSE, telemetry
│       ├── reliability/      # baselines, detectors, anomalies, health, Slack alerts
│       ├── integration/      # Slack, webhook credentials
│       ├── webhook/          # inbound webhook ingress + HMAC
│       ├── ai/               # AI agent CRUD + broker to Python service
│       ├── notification/     # in-app notifications
│       ├── observability/    # logs, audit trail
│       └── common/           # errors, crypto, rate-limiting
├── frontend/                 # Next.js App Router (TypeScript)
│   ├── app/                  # routes: dashboard, workflows, executions, reliability, agents, integrations, team, settings
│   ├── components/           # ui/ (shadcn) · brand/ · app/ (shell, notifications) · reliability/ (health widget, anomaly cards) · motion/
│   └── lib/                  # api client (axios), auth store (zustand), SSE stream
└── ai-service/               # FastAPI (Python)
    └── app/                  # main.py, config.py, routes/ai.py, providers/gemini.py, tools.py
```

## Testing

```bash
cd backend    && mvn test                              # 168 JUnit + MockMvc tests
cd frontend   && npm run lint && npm run typecheck      # ESLint + tsc (no errors)
cd ai-service && source .venv/bin/activate && pytest -v # 22 pytest tests
```

All three pass in CI.

## CI Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs on every push/PR to `main`:
- Backend: `mvn -B test` (JUnit, H2 test profile)
- AI service: `pytest -q`
- Frontend: `npm run typecheck` && `npm run lint` && `npm run build`
- Docker: builds all three service images

## Roadmap (Complete)

Built and verified milestone by milestone — the vertical spine first, everything
shipped actually runs.

- ✅ **M0 — Foundation**: 3-service skeletons, health checks, landing page, Docker.
- ✅ **M1 — Auth & multi-tenancy**: register/login (JWT), organizations, RBAC (Owner/Admin/Member/Viewer), app shell.
- ✅ **M2 — Workflows & builder**: workflow CRUD, React Flow visual builder, validation, immutable versioning, publish.
- ✅ **M3 — Execution engine & monitoring**: real node executors, queue (in-memory/Redis), live SSE monitoring, dashboards, retry/cancel/approval.
- ✅ **M4 — AI**: natural-language → workflow graph, reusable AI agents, allowlisted tool registry (calculator, time).
- ✅ **M5 — Breadth**: integrations + encrypted credentials (Slack), secure webhooks (HMAC), templates, team management, settings, logs, audit trail, ⌘K palette.
- ✅ **M6 — Reliability loop & hardening**: telemetry capture, baseline learning, 4 anomaly detectors (latency, output/schema drift, behavioral, volume), anomaly persistence with dedup, in-app notifications, Slack alerts, AI investigation (fact vs inference), idempotency for webhooks/retry, external execution event ingestion API, CI pipeline, cleanup.

## Security

BCrypt password hashing · JWT access+refresh · server-side tenant isolation (auth
context is never client-supplied) · AES-256-GCM credential encryption · rate
limiting · CORS · secure headers · input validation · masked secrets · audit
logging · allowlisted AI tools with explicit user confirmation · webhook HMAC
verification · constant-time token comparison · structured error envelopes.
