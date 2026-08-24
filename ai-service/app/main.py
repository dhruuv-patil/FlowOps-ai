"""FlowOps AI service — FastAPI entry point.

M0 exposes only health. M4 adds LangGraph-powered workflow generation
(`/ai/generate-workflow`) and agent execution (`/ai/agent/run`).
"""

from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()

    app = FastAPI(
        title="FlowOps AI Service",
        version=settings.version,
        description="LangGraph-powered AI workflow generation and agents for FlowOps.",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origin_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.get("/health", tags=["health"])
    def health() -> dict:
        return {
            "status": "ok",
            "service": settings.service_name,
            "version": settings.version,
            # Honest signal to the backend/UI without leaking the key.
            "ai_configured": settings.ai_configured,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

    return app


app = create_app()
