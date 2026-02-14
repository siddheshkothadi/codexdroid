# Harness Architecture

Last updated: 2026-02-14

## Purpose
Define the harness-first delivery architecture for CodexDroid as a single-maintainer, direct-to-main project.

## Components
1. Specs: `docs/harness/specs/` are the behavioral source of truth.
2. Scenarios: `harness/scenarios/*.json` are machine-checkable contracts mapped to specs.
3. Fixtures: `harness/fixtures/*.json` provide deterministic protocol/request samples.
4. Runner: `harness/runners/cli.py` executes suites and writes reports.
5. Scoring: `harness/scoring/scorer.py` computes pass/fail, pass-rate, and gate status.
6. Gates: `harness/config/gates.json` defines threshold policy.

## Suite Strategy
1. `smoke`: fast guardrails required for every push to `main`.
2. `protocol`: protocol + turn-lifecycle safety checks, path-gated in CI and mandatory in local guard.
3. `nightly`: runs all suites and renders KPI summary artifacts.

## Governance
1. Every feature change requires:
   - one spec update/addition in `docs/harness/specs/`
   - at least one scenario update/addition in `harness/scenarios/`
2. Docs lint is blocking for push CI.
3. Thresholds are enforced via `--enforce-thresholds` for KPI gate evaluation.

## Related Docs
- `docs/harness/RELIABILITY.md`
- `docs/harness/SECURITY.md`
- `docs/harness/QUALITY_SCORECARD.md`
- `docs/harness/specs/index.md`
