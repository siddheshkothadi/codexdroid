from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from harness.runners.loader import load_suite
from harness.scoring.scorer import GateThresholds, evaluate_suite, render_markdown, write_report

SUPPORTED_SUITES = ("smoke", "protocol")
DEFAULT_GATES_PATH = ROOT / "harness" / "config" / "gates.json"


def _load_thresholds(gates_config_path: Path) -> GateThresholds:
    if not gates_config_path.exists():
        raise FileNotFoundError(f"Gates config not found: {gates_config_path}")
    with gates_config_path.open("r", encoding="utf-8") as f:
        payload: dict[str, Any] = json.load(f)
    min_pass_rate = float(payload.get("min_pass_rate", 98))
    max_flaky = int(payload.get("max_flaky", 0))
    return GateThresholds(min_pass_rate=min_pass_rate, max_flaky=max_flaky)


def _run_suite(
    suite_name: str,
    report_path: Path | None,
    thresholds: GateThresholds | None = None,
    enforce_thresholds: bool = False,
) -> int:
    suite_doc = load_suite(ROOT, suite_name)
    report = evaluate_suite(ROOT, suite_name, suite_doc, thresholds=thresholds)
    summary = report["summary"]
    print(
        f"[{suite_name}] total={summary['total']} passed={summary['passed']} "
        f"failed={summary['failed']} pass_rate={summary['pass_rate']}% gate={report['gate_status']}"
    )

    if report_path is not None:
        write_report(report, report_path)
        print(f"Wrote report: {report_path}")

    if enforce_thresholds:
        return 0 if report["gate_status"] == "pass" else 1
    return 0 if summary["failed"] == 0 else 1


def cmd_eval(args: argparse.Namespace) -> int:
    thresholds = None
    if args.enforce_thresholds:
        gates_path = Path(args.gates_config) if args.gates_config else DEFAULT_GATES_PATH
        thresholds = _load_thresholds(gates_path)

    suite = args.suite
    if suite == "all":
        exit_codes = []
        for suite_name in SUPPORTED_SUITES:
            suite_report_path = None
            if args.report:
                report_path = Path(args.report)
                suite_report_path = report_path.with_name(f"{suite_name}_{report_path.name}")
            exit_codes.append(
                _run_suite(
                    suite_name,
                    suite_report_path,
                    thresholds=thresholds,
                    enforce_thresholds=args.enforce_thresholds,
                )
            )
        return 0 if all(code == 0 for code in exit_codes) else 1

    report_path = Path(args.report) if args.report else None
    return _run_suite(
        suite,
        report_path,
        thresholds=thresholds,
        enforce_thresholds=args.enforce_thresholds,
    )


def cmd_report(args: argparse.Namespace) -> int:
    input_path = Path(args.input)
    if not input_path.exists():
        print(f"Input report not found: {input_path}", file=sys.stderr)
        return 1

    with input_path.open("r", encoding="utf-8") as f:
        report = json.load(f)

    markdown = render_markdown(report)
    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(markdown, encoding="utf-8")
        print(f"Wrote markdown report: {output_path}")
    else:
        print(markdown)

    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="CodexDroid harness runner")
    sub = parser.add_subparsers(dest="command", required=True)

    eval_parser = sub.add_parser("eval", help="Run a harness suite")
    eval_parser.add_argument("--suite", required=True, choices=[*SUPPORTED_SUITES, "all"])
    eval_parser.add_argument("--report", required=False, help="Write JSON report to this path")
    eval_parser.add_argument(
        "--enforce-thresholds",
        action="store_true",
        help="Apply gates from --gates-config (or default harness/config/gates.json).",
    )
    eval_parser.add_argument(
        "--gates-config",
        required=False,
        help="Path to gate threshold JSON (used only with --enforce-thresholds).",
    )
    eval_parser.set_defaults(func=cmd_eval)

    report_parser = sub.add_parser("report", help="Render JSON report as markdown")
    report_parser.add_argument("--input", required=True, help="Path to a JSON report")
    report_parser.add_argument("--output", required=False, help="Optional markdown output path")
    report_parser.set_defaults(func=cmd_report)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
