"""Provider-agnostic LLM interface.

A provider turns FlowOps' two AI needs — JSON workflow generation and a
tool-calling agent run — into calls against a concrete LLM, and normalises the
result back into the plain dataclasses defined here. The routes depend only on
this interface, so adding another backend (OpenAI, Anthropic, a local gateway)
is a drop-in: implement :class:`LLMProvider` and register it in
:func:`app.providers.get_provider`.

Invariants every provider must uphold (the NO FAKE FUNCTIONALITY mandate):

* Never fabricate output. A provider or transport failure raises
  :class:`ProviderError` (the route maps it to a 502) rather than returning a
  made-up completion.
* The API key is read from settings, never logged, never returned.
* Tools are executed only through :func:`app.tools.execute_tool` (allowlist ->
  validate -> execute -> sanitize); a provider merely wires the model's
  function calls to that pipeline.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Protocol


class ProviderError(Exception):
    """A provider or transport failure. Carries only a safe, non-sensitive message."""


@dataclass
class ToolInvocation:
    """One tool the model invoked during a run, surfaced to the UI for transparency."""

    name: str
    arguments: dict
    result: str


@dataclass
class AgentResult:
    """Normalised result of an agent run, mapped 1:1 onto ``AgentRunResponse``."""

    output: str
    model: str
    tool_calls: list[ToolInvocation] = field(default_factory=list)


class LLMProvider(Protocol):
    """The contract the routes rely on. Implementations live in sibling modules."""

    name: str

    def is_configured(self) -> bool:
        """True when this provider has credentials. Never inspects/returns the key itself."""
        ...

    def default_model(self) -> str:
        """The model used when a request doesn't specify one."""
        ...

    def generate_json(self, system_prompt: str, user_prompt: str, model: str | None = None) -> str:
        """Return the model's raw JSON *text* (the caller parses it). Raises ProviderError."""
        ...

    def run_agent(
        self,
        instructions: str,
        model: str | None,
        user_input: str,
        enabled_tools: list[str],
    ) -> AgentResult:
        """Run a bounded tool-calling loop and return the final completion.

        Raises :class:`ProviderError` on any provider/transport failure — never
        returns fabricated output.
        """
        ...
