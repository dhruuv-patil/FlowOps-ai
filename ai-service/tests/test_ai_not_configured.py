"""With no provider key, both AI endpoints report ``configured: false`` and
fabricate nothing — the NO FAKE FUNCTIONALITY guarantee. No network is touched:
the not-configured branch short-circuits before any client is built.
"""

from fastapi.testclient import TestClient

import app.routes.ai as ai_routes
from app.main import app

client = TestClient(app)


def test_generate_workflow_not_configured(monkeypatch):
    monkeypatch.setattr(ai_routes, "is_configured", lambda: False)
    resp = client.post("/ai/generate-workflow", json={"prompt": "notify me every morning"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["configured"] is False
    assert body.get("graph") is None


def test_agent_run_not_configured(monkeypatch):
    monkeypatch.setattr(ai_routes, "is_configured", lambda: False)
    resp = client.post(
        "/ai/agent/run",
        json={"instructions": "You are a helpful assistant.", "tools": []},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["configured"] is False
    assert body.get("output") is None
    assert body["tool_calls"] == []


def test_agent_run_not_configured_ignores_missing_instructions(monkeypatch):
    # The honest "not configured" answer must not depend on a valid body. When the
    # service has no key, an empty/missing-instructions run still returns
    # configured:false — never a 422 that the backend would map to
    # "AI service unavailable" (regression: M4 agent-run bug).
    monkeypatch.setattr(ai_routes, "is_configured", lambda: False)
    for payload in ({}, {"instructions": ""}, {"instructions": "   "}):
        resp = client.post("/ai/agent/run", json=payload)
        assert resp.status_code == 200, payload
        assert resp.json()["configured"] is False, payload


def test_generate_workflow_validates_prompt():
    # An empty prompt is rejected by the schema (422), never silently accepted.
    resp = client.post("/ai/generate-workflow", json={"prompt": ""})
    assert resp.status_code == 422
