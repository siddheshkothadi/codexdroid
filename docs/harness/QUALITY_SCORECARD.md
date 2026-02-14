# Harness Quality Scorecard

Last updated: 2026-02-14

## Gate Policy
1. Minimum pass-rate: `98%`
2. Maximum flaky scenarios: `0`
3. Gate config source: `harness/config/gates.json`

## Required Checks by Flow
1. Local pre-push guard:
   - `./gradlew testDebugUnitTest`
   - `python harness/runners/cli.py eval --suite smoke --enforce-thresholds`
   - `python harness/runners/cli.py eval --suite protocol --enforce-thresholds`
   - `./gradlew assembleDebug`
   - `scripts/ci/docs_lint.ps1`
2. Push CI (`main`):
   - Android fast checks
   - smoke harness
   - docs lint
3. Nightly:
   - lint + unit + build
   - all harness suites
   - markdown KPI summary artifact

## KPI Review Cadence
1. Daily: observe nightly pass/fail signals.
2. Weekly: review pass-rate trend, failure clusters, and stale specs.

## Definition of Healthy Main
1. All push workflows green.
2. Nightly summary generated.
3. No unresolved flaky scenario.
