"""
Google Gemini provider (google-genai SDK).

FlowOps uses Gemini in two separate modes:

1. Structured JSON generation
   - Used for workflow generation.
   - No tools.
   - No automatic function calling.
   - Uses models.generate_content().

2. AI agent execution
   - Uses Gemini Chat API.
   - Uses chat.send_message().
   - Tool execution remains fully controlled by FlowOps.
   - Gemini only requests a tool call.
   - FlowOps executes it through execute_tool().
   - Automatic function calling is explicitly disabled.

This separation avoids Google's AFC warning around using tools with
models.generate_content().
"""

from __future__ import annotations

import json
import logging
import time
from typing import Any

from app.config import get_settings
from app.providers.base import AgentResult, ProviderError, ToolInvocation
from app.tools import execute_tool, function_specs


logger = logging.getLogger("flowops.ai")


# Keep provider logs intentionally narrow.
# Never log full Gemini exception messages because they may contain
# user input, schemas, model information, or other request data.
_MAX_LOG_CHARS = 256


def _safe_error_detail(exc: Exception) -> str:
    """Return a narrow and safe provider error description."""

    parts: list[str] = [
        f"type={type(exc).__name__}",
    ]

    for attr in ("code", "status"):
        value = getattr(exc, attr, None)

        if value not in (None, ""):
            parts.append(
                f"{attr}={value}"
            )

    if len(parts) == 1:
        parts.append(
            "error=<unstructured>"
        )

    return " ".join(parts)[:_MAX_LOG_CHARS]


def _error_code(exc: Exception) -> Any:
    """Return Gemini error code when available."""

    return getattr(
        exc,
        "code",
        None,
    )


def _error_status(exc: Exception) -> Any:
    """Return Gemini error status when available."""

    return getattr(
        exc,
        "status",
        None,
    )


def _is_transient_error(exc: Exception) -> bool:
    """
    Identify provider failures that are reasonable to retry.

    Supported transient conditions:

        429 RESOURCE_EXHAUSTED
        499 CANCELLED
        500 INTERNAL
        502 BAD_GATEWAY
        503 UNAVAILABLE
        504 DEADLINE_EXCEEDED
    """

    code = _error_code(exc)
    status = _error_status(exc)

    code_string = str(code).upper()
    status_string = str(status).upper()

    transient_codes = {
        "429",
        "499",
        "500",
        "502",
        "503",
        "504",
    }

    transient_statuses = {
        "RESOURCE_EXHAUSTED",
        "CANCELLED",
        "INTERNAL",
        "BAD_GATEWAY",
        "UNAVAILABLE",
        "DEADLINE_EXCEEDED",
    }

    if code_string in transient_codes:
        return True

    if status_string in transient_statuses:
        return True

    return False


def _retry_delay(attempt: int) -> float:
    """
    Exponential backoff.

    attempt 0 -> 1 second
    attempt 1 -> 2 seconds
    attempt 2 -> 4 seconds
    """

    return float(
        2**attempt
    )


class GeminiProvider:
    """
    LLM provider backed by the Google Gemini Developer API.
    """

    name = "gemini"

    # ------------------------------------------------------------------
    # Configuration
    # ------------------------------------------------------------------

    def is_configured(self) -> bool:
        """
        Return whether Gemini API credentials are configured.
        """

        return bool(
            get_settings()
            .gemini_api_key
            .strip()
        )

    def default_model(self) -> str:
        """
        Return the configured default Gemini model.
        """

        return get_settings().gemini_model

    # ------------------------------------------------------------------
    # SDK
    # ------------------------------------------------------------------

    def _genai_modules(self):
        """
        Import google-genai lazily.

        This keeps the service usable without the Gemini SDK installed
        when Gemini is not configured.
        """

        try:
            from google import genai
            from google.genai import types

        except ImportError as exc:
            logger.error(
                "google-genai is not installed"
            )

            raise ProviderError(
                "The AI provider SDK is not installed."
            ) from exc

        return genai, types

    def _build_client(self):
        """
        Construct the Gemini client.

        API credentials are never logged.
        """

        genai, types = self._genai_modules()

        settings = get_settings()

        # google-genai HttpOptions.timeout is milliseconds.
        timeout_ms = int(
            settings.provider_timeout_seconds * 1000
        )

        try:
            return genai.Client(
                api_key=settings.gemini_api_key,
                http_options=types.HttpOptions(
                    timeout=timeout_ms,
                ),
            )

        except Exception as exc:
            logger.warning(
                "gemini client init failed: %s",
                _safe_error_detail(exc),
            )

            raise ProviderError(
                "The AI provider could not be initialised."
            ) from exc

    # ------------------------------------------------------------------
    # Structured JSON generation
    # ------------------------------------------------------------------

    def generate_json(
        self,
        system_prompt,
        user_prompt,
        model=None,
    ):
        """
        Generate structured JSON.

        IMPORTANT:

        This path intentionally has:

            - no tools
            - no function declarations
            - no automatic function calling
            - no Chat session

        This is the path used by FlowOps workflow generation.
        """

        _, types = self._genai_modules()

        client = self._build_client()

        used_model = (
            model
            or self.default_model()
        )

        config = types.GenerateContentConfig(
            system_instruction=system_prompt,
            response_mime_type="application/json",
        )

        max_attempts = 3

        for attempt in range(max_attempts):

            try:
                response = (
                    client.models.generate_content(
                        model=used_model,
                        contents=user_prompt,
                        config=config,
                    )
                )

                text = (
                    response.text
                    or ""
                )

                if not text.strip():
                    raise ProviderError(
                        "The AI provider returned an empty response."
                    )

                # Validate that Gemini actually returned JSON.
                try:
                    json.loads(text)

                except json.JSONDecodeError as exc:
                    logger.warning(
                        "gemini returned invalid JSON"
                    )

                    raise ProviderError(
                        "The AI provider returned invalid workflow JSON."
                    ) from exc

                return text

            except ProviderError:
                raise

            except Exception as exc:

                if (
                    not _is_transient_error(exc)
                    or attempt == max_attempts - 1
                ):
                    logger.warning(
                        "gemini generate_content failed: %s",
                        _safe_error_detail(exc),
                    )

                    raise ProviderError(
                        "The AI provider could not be reached."
                    ) from exc

                delay = _retry_delay(
                    attempt
                )

                logger.warning(
                    "gemini temporarily unavailable; "
                    "retrying in %ss (attempt %s/%s)",
                    int(delay),
                    attempt + 1,
                    max_attempts,
                )

                time.sleep(delay)

        raise ProviderError(
            "The AI provider could not be reached."
        )

    # ------------------------------------------------------------------
    # AI AGENT
    # ------------------------------------------------------------------

    def run_agent(
        self,
        instructions,
        model,
        user_input,
        enabled_tools,
    ):
        """
        Run a FlowOps AI agent.

        IMPORTANT:

        This uses the Gemini Chat API instead of:

            client.models.generate_content()

        because the agent has tools.

        FlowOps manually controls tool execution:

            Gemini
                ↓
            function call
                ↓
            execute_tool()
                ↓
            function response
                ↓
            Gemini
                ↓
            final answer

        Gemini automatic function calling is disabled.
        """

        _, types = self._genai_modules()

        client = self._build_client()

        used_model = (
            model
            or self.default_model()
        )

        tools_config = self._tools_config(
            enabled_tools,
            types,
        )

        config = types.GenerateContentConfig(
            system_instruction=instructions,
            tools=tools_config,

            # FlowOps owns the tool execution loop.
            automatic_function_calling=(
                types.AutomaticFunctionCallingConfig(
                    disable=True,
                )
            ),
        )

        # --------------------------------------------------------------
        # IMPORTANT PRODUCTION CHANGE
        #
        # Tools are now handled through Gemini Chat API.
        #
        # This avoids:
        #
        # "Direct use of automatic function calling (AFC) in
        # Models.generate_content is not recommended..."
        # --------------------------------------------------------------

        chat = client.chats.create(
            model=used_model,
            config=config,
        )

        invocations: list[ToolInvocation] = []

        max_iterations = (
            get_settings()
            .agent_max_iterations
        )

        current_message = (
            user_input
            or "Proceed with your instructions."
        )

        for _ in range(max_iterations):

            response = self._chat_send(
                chat=chat,
                message=current_message,
            )

            calls = (
                response.function_calls
                or []
            )

            # ----------------------------------------------------------
            # No function calls = final agent answer
            # ----------------------------------------------------------

            if not calls:

                return AgentResult(
                    output=(
                        response.text
                        or ""
                    ),
                    model=used_model,
                    tool_calls=invocations,
                )

            # ----------------------------------------------------------
            # Execute every requested tool through FlowOps.
            # ----------------------------------------------------------

            response_parts = []

            for call in calls:

                args = dict(
                    call.args
                    or {}
                )

                result = execute_tool(
                    call.name,
                    args,
                    enabled_tools,
                )

                invocations.append(
                    ToolInvocation(
                        name=call.name,
                        arguments=args,
                        result=result,
                    )
                )

                response_parts.append(
                    types.Part.from_function_response(
                        name=call.name,
                        response={
                            "result": result,
                        },
                    )
                )

            # ----------------------------------------------------------
            # Send the tool results back into the same Chat session.
            #
            # Chat maintains the conversation state for us.
            # ----------------------------------------------------------

            current_message = types.Content(
                role="user",
                parts=response_parts,
            )

        # ----------------------------------------------------------------
        # Agent iteration budget exhausted.
        # ----------------------------------------------------------------

        logger.warning(
            "gemini agent iteration limit reached"
        )

        return AgentResult(
            output=(
                "The AI agent reached its execution "
                "limit before completing the task."
            ),
            model=used_model,
            tool_calls=invocations,
        )

    # ------------------------------------------------------------------
    # Chat request
    # ------------------------------------------------------------------

    def _chat_send(
        self,
        chat,
        message,
    ):
        """
        Send one message through Gemini Chat API.

        This is intentionally separate from models.generate_content().
        """

        max_attempts = 3

        for attempt in range(max_attempts):

            try:
                return chat.send_message(
                    message=message,
                )

            except Exception as exc:

                if (
                    not _is_transient_error(exc)
                    or attempt == max_attempts - 1
                ):
                    logger.warning(
                        "gemini chat request failed: %s",
                        _safe_error_detail(exc),
                    )

                    raise ProviderError(
                        "The AI provider could not be reached."
                    ) from exc

                delay = _retry_delay(
                    attempt
                )

                logger.warning(
                    "gemini chat temporarily unavailable; "
                    "retrying in %ss (attempt %s/%s)",
                    int(delay),
                    attempt + 1,
                    max_attempts,
                )

                time.sleep(delay)

        raise ProviderError(
            "The AI provider could not be reached."
        )

    # ------------------------------------------------------------------
    # Tool configuration
    # ------------------------------------------------------------------

    def _tools_config(
        self,
        enabled_tools,
        types,
    ):
        """
        Build Gemini tool declarations.

        Returns None when no tools are enabled.
        """

        specs = function_specs(
            enabled_tools
        )

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

        return [
            types.Tool(
                function_declarations=declarations,
            )
        ]