# Harness Reliability

Last updated: 2026-02-14

## Reliability Objectives
1. Keep `main` continuously releasable.
2. Detect regressions in protocol routing, turn lifecycle, and session orchestration before release.
3. Eliminate flaky harness behavior (`max_flaky = 0`).

## SLO Targets
1. Harness pass-rate: `>= 98%`.
2. Flake budget: `0`.
3. Fast loop completion: local push guard should remain practical for daily usage.

## Failure Handling
1. If local guard fails, do not push.
2. If push CI fails on `main`, apply immediate fix-forward commit.
3. Nightly failures are triaged next working session and linked to a follow-up spec/scenario update.

## Regression Containment
1. Protocol fixtures include malformed and out-of-order examples.
2. Reducer contract invariants are covered by both unit tests and scenario checks.
3. Path-based CI gate escalates to protocol suite when high-risk files change.
