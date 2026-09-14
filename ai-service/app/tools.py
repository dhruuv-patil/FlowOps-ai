"""Allowlisted tool registry for AI agents.

An LLM may only ever invoke a tool from this registry, and only those a given
agent has explicitly enabled. Every call goes through the same pipeline:
permission (allowlisted AND enabled) -> validate (args match the schema) ->
execute (safe, local-only handler) -> sanitize (coerce + bound the result).

Deliberately there is NO shell, filesystem, or network egress here: the only
tools are pure, local computations. Adding a tool means adding a schema + a
handler that upholds the same guarantees.
"""

import ast
import operator
from datetime import datetime, timezone
from typing import Any, Callable
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

MAX_RESULT_CHARS = 4000


class Tool:
    def __init__(
        self,
        name: str,
        description: str,
        parameters: dict[str, Any],
        handler: Callable[[dict[str, Any]], str],
    ) -> None:
        self.name = name
        self.description = description
        self.parameters = parameters
        self.handler = handler


# ------------------------------------------------------------- safe arithmetic

_BINOPS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
}
_UNARYOPS = {ast.UAdd: operator.pos, ast.USub: operator.neg}


def _eval_node(node: ast.AST) -> float:
    if isinstance(node, ast.Constant):
        if isinstance(node.value, bool) or not isinstance(node.value, (int, float)):
            raise ValueError("only numeric literals are allowed")
        return node.value
    if isinstance(node, ast.BinOp) and type(node.op) in _BINOPS:
        left = _eval_node(node.left)
        right = _eval_node(node.right)
        if isinstance(node.op, ast.Pow) and (abs(right) > 100 or abs(left) > 1_000_000):
            raise ValueError("exponent out of the allowed range")
        return _BINOPS[type(node.op)](left, right)
    if isinstance(node, ast.UnaryOp) and type(node.op) in _UNARYOPS:
        return _UNARYOPS[type(node.op)](_eval_node(node.operand))
    raise ValueError("unsupported expression")


def _calculate(args: dict[str, Any]) -> str:
    expression = args["expression"]
    tree = ast.parse(expression, mode="eval")  # rejects statements/assignments
    result = _eval_node(tree.body)
    # Present integers without a trailing ".0".
    if isinstance(result, float) and result.is_integer():
        result = int(result)
    return str(result)


def _get_current_time(args: dict[str, Any]) -> str:
    tz_name = args.get("timezone")
    if tz_name:
        try:
            tz = ZoneInfo(tz_name)
        except (ZoneInfoNotFoundError, ValueError):
            raise ValueError(f"unknown timezone: {tz_name}")
    else:
        tz = timezone.utc
    return datetime.now(tz).isoformat()


TOOLS: dict[str, Tool] = {
    "get_current_time": Tool(
        name="get_current_time",
        description="Return the current date and time as an ISO-8601 string. "
        "Defaults to UTC; pass an IANA timezone name (e.g. 'America/New_York') to localise.",
        parameters={
            "type": "object",
            "properties": {
                "timezone": {
                    "type": "string",
                    "description": "IANA timezone name. Optional; defaults to UTC.",
                }
            },
            "required": [],
            "additionalProperties": False,
        },
        handler=_get_current_time,
    ),
    "calculate": Tool(
        name="calculate",
        description="Evaluate a basic arithmetic expression (+, -, *, /, %, **, "
        "parentheses) over numbers and return the result.",
        parameters={
            "type": "object",
            "properties": {
                "expression": {
                    "type": "string",
                    "description": "The arithmetic expression, e.g. '(3 + 4) * 2'.",
                }
            },
            "required": ["expression"],
            "additionalProperties": False,
        },
        handler=_calculate,
    ),
}


def available_tool_names() -> list[str]:
    return list(TOOLS.keys())


def function_specs(enabled: list[str]) -> list[dict]:
    """Provider-neutral function specs for the enabled tools (that exist).

    Each spec is ``{name, description, parameters}`` where ``parameters`` is a
    JSON-Schema object. ``additionalProperties`` is dropped because provider
    function-declaration schemas (e.g. Gemini's OpenAPI subset) reject it;
    argument allowlisting is still enforced at call time by
    :func:`execute_tool` / :func:`_validate_args`, which read the full schema.
    """
    specs: list[dict] = []
    for name in enabled:
        tool = TOOLS.get(name)
        if tool is None:
            continue
        parameters: dict[str, Any] = {
            "type": tool.parameters.get("type", "object"),
            "properties": tool.parameters.get("properties", {}),
        }
        required = tool.parameters.get("required")
        if required:
            parameters["required"] = required
        specs.append(
            {"name": tool.name, "description": tool.description, "parameters": parameters}
        )
    return specs


def _validate_args(tool: Tool, args: dict[str, Any]) -> None:
    schema = tool.parameters
    props: dict[str, Any] = schema.get("properties", {})
    for key in schema.get("required", []):
        if key not in args:
            raise ValueError(f"missing required argument: {key}")
    if not schema.get("additionalProperties", True):
        for key in args:
            if key not in props:
                raise ValueError(f"unexpected argument: {key}")
    for key, value in args.items():
        expected = props.get(key, {}).get("type")
        if expected == "string" and not isinstance(value, str):
            raise ValueError(f"argument '{key}' must be a string")
        if expected == "number" and (isinstance(value, bool) or not isinstance(value, (int, float))):
            raise ValueError(f"argument '{key}' must be a number")


def execute_tool(name: str, args: dict[str, Any], enabled: list[str]) -> str:
    """Run one tool through permission -> validate -> execute -> sanitize.

    Never raises: any failure is returned as a short ``Error: ...`` string so the
    model can recover on the next turn. The result is always a bounded string.
    """
    # 1. permission — must be a real tool AND enabled for this agent.
    if name not in TOOLS or name not in enabled:
        return f"Error: tool '{name}' is not available."
    tool = TOOLS[name]
    if not isinstance(args, dict):
        return "Error: tool arguments must be a JSON object."
    # 2. validate
    try:
        _validate_args(tool, args)
    except ValueError as exc:
        return f"Error: {exc}"
    # 3. execute + 4. sanitize
    try:
        result = tool.handler(args)
    except Exception as exc:  # noqa: BLE001 — surface a safe message, never a trace
        return f"Error: {exc}"
    text = str(result)
    return text[:MAX_RESULT_CHARS]
