from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from harness.runners.loader import load_suite
from harness.scoring.scorer import evaluate_suite, render_markdown, write_report

SUPPORTED_SUITES = ("smoke", "protocol")


def _run_suite(suite_name: str, report_path: Path | None) -> int:
    suite_doc = load_suite(ROOT, suite_name)
    report = evaluate_suite(ROOT, suite_name, suite_doc)
    summary = report["summary"]
    print(f"[{suite_name}] total={summary['total']} passed={summary['passed']} failed={summary['failed']}")

    if report_path is not None:
        write_report(report, report_path)
        print(f"Wrote report: {report_path}")

    return 0 if summary["failed"] == 0 else 1


def cmd_eval(args: argparse.Namespace) -> int:
    suite = args.suite
    if suite == "all":
        exit_codes = []
        for suite_name in SUPPORTED_SUITES:
            suite_report_path = None
            if args.report:
                report_path = Path(args.report)
                suite_report_path = report_path.with_name(f"{suite_name}_{report_path.name}")
            exit_codes.append(_run_suite(suite_name, suite_report_path))
        return 0 if all(code == 0 for code in exit_codes) else 1

    report_path = Path(args.report) if args.report else None
    return _run_suite(suite, report_path)


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
