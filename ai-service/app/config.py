"""AI service configuration.

The AI service is isolated from the Spring Boot core. It exposes an
OpenAI-compatible, bring-your-own-key interface: when no key is configured the
AI endpoints report "not configured" rather than returning fabricated output.
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "flowops-ai"
    version: str = "0.1.0"

    # OpenAI-compatible provider (bring-your-own-key). base_url lets you point at
    # OpenAI, Azure OpenAI, a local model, or any compatible gateway.
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"

    # Shared secret the Spring backend presents when calling this service.
    ai_service_token: str = ""

    # Allowed callers (the backend). Comma-separated origins.
    cors_origins: str = "http://localhost:8080"

    @property
    def ai_configured(self) -> bool:
        """True when an API key is present. Never expose the key itself."""
        return bool(self.openai_api_key.strip())

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
