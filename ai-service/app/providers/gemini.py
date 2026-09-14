"""Google Gemini provider (the ``google-genai`` SDK).

Bring-your-own-key via ``GEMINI_API_KEY``. The SDK is imported lazily inside the
methods that actually call the API, so the "not configured" path, the health
endpoint, and the test suite all work without ``google-genai`` installed — a
keyless deployment never needs the package at all.

The key is read from settings and never logged. Any SDK/transport failure is
converted to :class:`ProviderError` so the route can answer with a clear error
instead of fabricated output.
"""

from __future__ import annotations

import json
import logging

from app.config import get_settings
from app.providers.base import AgentResult, ProviderError, ToolInvocation
from app.tools import execute_tool, function_specs

logger = logging.getLogger("flowops.ai")

# Provider-error logging is kept deliberately narrow: the type and a short
# status. The full Gemini `message` and `details` are intentionally NOT logged:
# they can echo parts of the user payload (request size, schema names, finetuned
# model ids) and we have no need for them in production. The shared service
# token and the Gemini API key are scrubbed by absence, not regex — they are
# never held in a string we log.
_MAX_LOG_CHARS = 256


def _safe_error_detail(exc: Exception) -> str:
    """Single-line, narrow description of a provider error for diagnostics."""
    parts: list[str] = [f"type={type(exc).__name__}"]
    for attr in ("code", "status"):
        value = getattr(exc, attr, None)
        if value not in (None, ""):
            parts.append(f"{attr}={value}")
    if len(parts) == 1:  # not an APIError-shaped exception — include the class name only
        parts.append("error=<unstructured>")
    return " ".join(parts)[:_MAX_LOG_CHARS]


class GeminiProvider:
    """:class:`~app.providers.base.LLMProvider` backed by the Gemini Developer API."""

    name = "gemini"

    def is_configured(self) -> bool:
        return bool(get_settings().gemini_api_key.strip())

    def default_model(self) -> str:
        return get_settings().gemini_model

    # -- SDK access (lazy) ----------------------------------------------------

    def _genai_modules(self):
        """Import the google-genai modules on demand. Missing SDK -> ProviderError."""
        try:
            from google import genai
            from google.genai import types
        except ImportError as exc:  # deploy/config error, not a runtime completion
            logger.error("google-genai is not installed")
            raise ProviderError("The AI provider SDK is not installed.") from exc
        return genai, types

    def _build_client(self):
        """Construct the google-genai client from settings. Never logs the key."""
        genai, types = self._genai_modules()
        settings = get_settings()
        # HttpOptions.timeout is milliseconds.
        timeout_ms = int(settings.provider_timeout_seconds * 1000)
        try:
            return genai.Client(
                api_key=settings.gemini_api_key,
                http_options=types.HttpOptions(timeout=timeout_ms),
            )
        except Exception as exc:  # noqa: BLE001 — surface a safe, keyless message
            logger.warning("gemini client init failed: %s", _safe_error_detail(exc))
            raise ProviderError("The AI provider could not be initialised.") from exc

    # -- structured generation ------------------------------------------------

    def generate_json(self, system_prompt, user_prompt, model=None):
        _, types = self._genai_modules()
        client = self._build_client()
        used_model = model or self.default_model()
        try:
            response = client.models.generate_content(
                model=used_model,
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    response_mime_type="application/json",
                ),
            )
        except Exception as exc:  # noqa: BLE001 — provider/transport failure
            logger.warning("gemini generate_content failed: %s", _safe_error_detail(exc))
            raise ProviderError("The AI provider could not be reached.") from exc
        return response.text or ""

    # -- agent run (manual tool-calling loop) ---------------------------------

    def run_agent(self, instructions, model, user_input, enabled_tools):
        _, types = self._genai_modules()
        client = self._build_client()
        used_model = model or self.default_model()

        config = types.GenerateContentConfig(
            system_instruction=instructions,
            tools=self._tools_config(enabled_tools, types),
            # We drive the loop by hand so every call is routed through the
            # allowlisted, schema-validated execute_tool pipeline.
            automatic_function_calling=types.AutomaticFunctionCallingConfig(disable=True),
        )

        contents = [
            types.Content(
                role="user",
                parts=[types.Part(text=user_input or "Proceed with your instructions.")],
            )
        ]

        invocations: list[ToolInvocation] = []
        max_iterations = get_settings().agent_max_iterations

        for _ in range(max_iterations):
            response = self._generate(client, used_model, contents, config)
            calls = response.function_calls or []
            if not calls:
                return AgentResult(
                    output=response.text or "", model=used_model, tool_calls=invocations
                )

            # Preserve the model's function-call turn so the next request has context.
            candidate = response.candidates[0] if response.candidates else None
            if candidate is not None and candidate.content is not None:
                contents.append(candidate.content)

            response_parts = []
            for call in calls:
                args = dict(call.args or {})
                result = execute_tool(call.name, args, enabled_tools)
                invocations.append(
                    ToolInvocation(name=call.name, arguments=args, result=result)
                )
                response_parts.append(
                    types.Part.from_function_response(
                        name=call.name, response={"result": result}
                    )
                )
            contents.append(types.Content(role="user", parts=response_parts))

        # Iteration budget exhausted — force a final text answer with tools disabled.
        final_config = types.GenerateContentConfig(system_instruction=instructions)
        response = self._generate(client, used_model, contents, final_config)
        return AgentResult(output=response.text or "", model=used_model, tool_calls=invocations)

    # -- helpers --------------------------------------------------------------

    def _generate(self, client, model, contents, config):
        try:
            return client.models.generate_content(model=model, contents=contents, config=config)
        except Exception as exc:  # noqa: BLE001 — provider/transport failure
            logger.warning("gemini agent step failed: %s", _safe_error_detail(exc))
            raise ProviderError("The AI provider could not be reached.") from exc

    def _tools_config(self, enabled_tools, types):
        """Gemini ``Tool`` list for the enabled tools, or ``None`` when there are none."""
        specs = function_specs(enabled_tools)
        if not specs:
            return None
        declarations = [
            types.FunctionDeclaration(
                name=spec["name"],
                description=spec["description"],
                parameters=spec["parameters"],
            )
            for spec in specs
        ]
        return [types.Tool(function_declarations=declarations)]
