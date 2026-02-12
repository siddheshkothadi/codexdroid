from __future__ import annotations

import hashlib
import json
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def _trace_ref(payload: dict[str, Any]) -> str:
    serialized = json.dumps(payload, sort_keys=True, separators=(",", ":"))
    digest = hashlib.sha256(serialized.encode("utf-8")).hexdigest()
    return f"sha256:{digest[:16]}"


def _read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def _check_paths_exist(root: Path, scenario: dict[str, Any]) -> tuple[list[str], str | None]:
    missing: list[str] = []
    for rel in scenario.get("paths", []):
        if not (root / rel).exists():
            missing.append(rel)
    if missing:
        return [], f"Missing required paths: {', '.join(missing)}"
    return ["All required paths exist."], None


def _check_json_required_keys(root: Path, scenario: dict[str, Any]) -> tuple[list[str], str | None]:
    rel = scenario.get("file", "")
    required = scenario.get("required_keys", [])
    path = root / rel
    if not path.exists():
        return [], f"JSON file not found: {rel}"
    data = _read_json(path)
    if not isinstance(data, dict):
        return [], f"Expected object at top-level for {rel}"
    missing = [k for k in required if k not in data]
    if missing:
        return [], f"Missing required keys in {rel}: {', '.join(missing)}"
    return [f"Required keys present in {rel}."], None


def _check_contains_text(root: Path, scenario: dict[str, Any]) -> tuple[list[str], str | None]:
    rel = scenario.get("file", "")
    fragments: list[str] = scenario.get("fragments", [])
    path = root / rel
    if not path.exists():
        return [], f"Text file not found: {rel}"
    content = path.read_text(encoding="utf-8")
    missing = [fragment for fragment in fragments if fragment not in content]
    if missing:
        return [], f"Missing required fragments in {rel}: {', '.join(missing)}"
    return [f"All required fragments present in {rel}."], None


def _check_fixture_methods_include(root: Path, scenario: dict[str, Any], key: str) -> tuple[list[str], str | None]:
    rel = scenario.get("file", "")
    expected: list[str] = scenario.get("methods", [])
    path = root / rel
    if not path.exists():
        return [], f"Fixture file not found: {rel}"
    data = _read_json(path)
    entries = data.get(key, []) if isinstance(data, dict) else []
    methods = {entry.get("method") for entry in entries if isinstance(entry, dict)}
    missing = [method for method in expected if method not in methods]
    if missing:
        return [], f"Missing methods in {rel}: {', '.join(missing)}"
    return [f"Fixture contains expected methods in {rel}."], None


def _check_event_has_missing_thread_id(root: Path, scenario: dict[str, Any]) -> tuple[list[str], str | None]:
    rel = scenario.get("file", "")
    method = scenario.get("method", "")
    path = root / rel
    if not path.exists():
        return [], f"Fixture file not found: {rel}"
    data = _read_json(path)
    events = data.get("events", []) if isinstance(data, dict) else []
    for event in events:
        if not isinstance(event, dict):
            continue
        if event.get("method") != method:
            continue
        params = event.get("params")
        if isinstance(params, dict) and "threadId" not in params and "thread_id" not in params:
            return [f"Found {method} event without thread id (router mapping scenario)."], None
    return [], f"No {method} event missing thread id found in {rel}"


def evaluate_scenario(root: Path, scenario: dict[str, Any]) -> dict[str, Any]:
    start = time.perf_counter()
    scenario_id = scenario.get("id", "unknown")
    kind = scenario.get("kind", "")

    assertions: list[str] = []
    failure_reason: str | None = None

    if kind == "paths_exist":
        assertions, failure_reason = _check_paths_exist(root, scenario)
    elif kind == "json_required_keys":
        assertions, failure_reason = _check_json_required_keys(root, scenario)
    elif kind == "contains_text":
        assertions, failure_reason = _check_contains_text(root, scenario)
    elif kind == "fixture_methods_include":
        assertions, failure_reason = _check_fixture_methods_include(root, scenario, key="events")
    elif kind == "fixture_request_methods_include":
        assertions, failure_reason = _check_fixture_methods_include(root, scenario, key="requests")
    elif kind == "fixture_event_has_missing_thread_id":
        assertions, failure_reason = _check_event_has_missing_thread_id(root, scenario)
    else:
        failure_reason = f"Unsupported scenario kind: {kind}"

    status = "pass" if failure_reason is None else "fail"
    latency_ms = int((time.perf_counter() - start) * 1000)
    trace_ref = _trace_ref({"id": scenario_id, "status": status, "failure_reason": failure_reason})

    return {
        "scenario_id": scenario_id,
        "status": status,
        "latency_ms": latency_ms,
        "assertions": assertions,
        "failure_reason": failure_reason,
        "trace_ref": trace_ref,
    }


def evaluate_suite(root: Path, suite_name: str, suite_doc: dict[str, Any]) -> dict[str, Any]:
    results = [evaluate_scenario(root, scenario) for scenario in suite_doc.get("scenarios", [])]
    passed = sum(1 for r in results if r["status"] == "pass")
    failed = len(results) - passed
    return {
        "suite": suite_name,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "summary": {
            "total": len(results),
            "passed": passed,
            "failed": failed,
        },
        "results": results,
    }


def write_report(report: dict[str, Any], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
        f.write("\n")


def render_markdown(report: dict[str, Any]) -> str:
    summary = report.get("summary", {})
    lines = [
        f"# Harness Report: {report.get('suite', 'unknown')}",
        "",
        f"- Generated: {report.get('generated_at', '')}",
        f"- Total: {summary.get('total', 0)}",
        f"- Passed: {summary.get('passed', 0)}",
        f"- Failed: {summary.get('failed', 0)}",
        "",
        "## Results",
    ]

    for row in report.get("results", []):
        status = row.get("status", "fail")
        icon = "PASS" if status == "pass" else "FAIL"
        lines.append(f"- {icon} `{row.get('scenario_id', 'unknown')}` ({row.get('latency_ms', 0)} ms)")
        reason = row.get("failure_reason")
        if reason:
            lines.append(f"  - Reason: {reason}")
        trace_ref = row.get("trace_ref")
        if trace_ref:
            lines.append(f"  - Trace: `{trace_ref}`")
    lines.append("")
    return "\n".join(lines)
