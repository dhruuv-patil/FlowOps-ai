"""The node catalogue used to steer workflow generation.

Mirrors the backend ``NodeRegistry`` (the single source of truth). The backend
re-validates every generated graph against its own registry and drops anything
unknown, so a drift here can only ever *reduce* quality, never produce an
invalid workflow. Keep the two in sync when node types change.
"""

# Each entry: type, one-line purpose, output handles, and the config keys the
# generator may set. Only these keys survive the backend's sanitisation.
NODE_CATALOG: list[dict] = [
    {
        "type": "manual_trigger",
        "purpose": "Starts the workflow when a user clicks Run. Use as the entry point.",
        "outputs": ["out"],
        "config": {},
        "trigger": True,
    },
    {
        "type": "webhook_trigger",
        "purpose": "Starts the workflow when an HTTP request hits its webhook URL.",
        "outputs": ["out"],
        "config": {"method": "one of POST | GET | PUT (optional, default POST)"},
        "trigger": True,
    },
    {
        "type": "http_request",
        "purpose": "Calls an external HTTP API.",
        "outputs": ["out"],
        "config": {
            "method": "one of GET | POST | PUT | PATCH | DELETE (required)",
            "url": "the request URL (required)",
            "headers": "JSON object of request headers (optional)",
            "body": "request body text (optional)",
        },
        "trigger": False,
    },
    {
        "type": "condition",
        "purpose": "Branches the flow. Wire the two outputs with sourceHandle "
        '"true" and "false".',
        "outputs": ["true", "false"],
        "config": {"expression": "a boolean expression, e.g. {{http_request.status}} == 200 (required)"},
        "trigger": False,
    },
    {
        "type": "transform",
        "purpose": "Reshapes data into a new object for downstream nodes.",
        "outputs": ["out"],
        "config": {"mapping": "JSON object describing the output shape (required)"},
        "trigger": False,
    },
    {
        "type": "delay",
        "purpose": "Pauses the flow for a fixed number of seconds.",
        "outputs": ["out"],
        "config": {"seconds": "integer seconds to wait (required)"},
        "trigger": False,
    },
    {
        "type": "notification",
        "purpose": "Sends a notification message.",
        "outputs": ["out"],
        "config": {
            "channel": "one of email | slack | webhook (required)",
            "message": "the message body (required)",
        },
        "trigger": False,
    },
    {
        "type": "human_approval",
        "purpose": "Pauses for a human decision. Wire the two outputs with "
        'sourceHandle "approved" and "rejected".',
        "outputs": ["approved", "rejected"],
        "config": {"prompt": "the question shown to the approver (required)"},
        "trigger": False,
    },
    {
        "type": "ai_agent",
        "purpose": "Runs an AI completion. For generated workflows, put the system "
        "prompt in `instructions`.",
        "outputs": ["out"],
        "config": {
            "instructions": "system instructions for the AI (required here)",
            "input": "the user/content input, may reference {{variables}} (optional)",
        },
        "trigger": False,
    },
]


def catalog_prompt() -> str:
    """Render the catalogue as a compact spec for the generation system prompt."""
    lines: list[str] = []
    for node in NODE_CATALOG:
        role = "TRIGGER" if node["trigger"] else "STEP"
        lines.append(f"- {node['type']} [{role}] — {node['purpose']}")
        lines.append(f"    outputs: {', '.join(node['outputs'])}")
        if node["config"]:
            for key, desc in node["config"].items():
                lines.append(f"    config.{key}: {desc}")
        else:
            lines.append("    config: (none)")
    return "\n".join(lines)
