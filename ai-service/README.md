# FlowOps AI Service

Isolated Python service that powers FlowOps AI features via an
**OpenAI-compatible, bring-your-own-key** interface. Only the Spring Boot
backend calls it — never the browser.

- **M0 (now):** `GET /health` (reports `ai_configured` without leaking the key).
- **M4:** `POST /ai/generate-workflow` (LangGraph → workflow graph) and
  `POST /ai/agent/run` (agent execution with structured output), plus an
  allowlisted tool registry.

## Run locally

```bash
cd ai-service
python3.12 -m venv .venv        # 3.11/3.12 recommended
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8100
```

Health check: `curl http://localhost:8100/health`

## Configuration

| Variable | Purpose |
|----------|---------|
| `OPENAI_API_KEY` | Provider key. Empty ⇒ AI endpoints report "not configured". |
| `OPENAI_BASE_URL` | OpenAI-compatible endpoint (OpenAI, Azure, local gateway). |
| `OPENAI_MODEL` | Default model id. |
| `AI_SERVICE_TOKEN` | Shared secret the backend presents on each call. |
| `CORS_ORIGINS` | Allowed callers (the backend origin). |
