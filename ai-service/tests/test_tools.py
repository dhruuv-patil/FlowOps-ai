"""The allowlisted tool registry must be safe: no eval, no names, no calls,
and unknown/disabled tools are refused. Every failure comes back as a bounded
``Error: ...`` string rather than raising.
"""

from datetime import datetime

from app.tools import execute_tool

ENABLED = ["get_current_time", "calculate"]


def test_calculate_basic():
    assert execute_tool("calculate", {"expression": "(3 + 4) * 2"}, ENABLED) == "14"


def test_calculate_division_is_float():
    assert execute_tool("calculate", {"expression": "7 / 2"}, ENABLED) == "3.5"


def test_calculate_rejects_names():
    out = execute_tool("calculate", {"expression": "__import__('os')"}, ENABLED)
    assert out.startswith("Error")


def test_calculate_rejects_calls():
    out = execute_tool("calculate", {"expression": "len([1, 2, 3])"}, ENABLED)
    assert out.startswith("Error")


def test_calculate_rejects_attribute_access():
    out = execute_tool("calculate", {"expression": "(1).__class__"}, ENABLED)
    assert out.startswith("Error")


def test_calculate_bounds_exponent():
    # A huge exponent must be refused, not computed (DoS guard).
    out = execute_tool("calculate", {"expression": "10 ** 99999"}, ENABLED)
    assert out.startswith("Error")


def test_calculate_missing_required_arg():
    assert execute_tool("calculate", {}, ENABLED).startswith("Error")


def test_calculate_rejects_unexpected_arg():
    out = execute_tool("calculate", {"expression": "1 + 1", "evil": "x"}, ENABLED)
    assert out.startswith("Error")


def test_unknown_tool_refused():
    assert execute_tool("run_shell", {"cmd": "ls"}, ENABLED).startswith("Error")


def test_disabled_tool_refused():
    # 'calculate' exists but is not enabled for this agent.
    out = execute_tool("calculate", {"expression": "1 + 1"}, ["get_current_time"])
    assert out.startswith("Error")


def test_get_current_time_returns_tz_aware_iso():
    out = execute_tool("get_current_time", {}, ENABLED)
    parsed = datetime.fromisoformat(out)
    assert parsed.tzinfo is not None


def test_get_current_time_rejects_unknown_timezone():
    out = execute_tool("get_current_time", {"timezone": "Mars/Phobos"}, ENABLED)
    assert out.startswith("Error")
