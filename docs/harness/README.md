# Harness Docs

Last updated: 2026-02-14

This folder captures the feature contracts and operating playbooks for the harness-first delivery loop.

## Layout
- `specs/`: behavior contracts and acceptance checks.
- `playbooks/`: operational workflows for fast-loop development and releases.
- `ARCHITECTURE.md`: harness system boundaries, flow, and policy.
- `RELIABILITY.md`: SLOs and regression containment policy.
- `SECURITY.md`: protocol/data handling safety policy.
- `QUALITY_SCORECARD.md`: KPI thresholds and check matrix.

## Core policy
- New behavior must be accompanied by a small, machine-checkable spec.
- Every direct push to `main` must pass local fast loop + harness checks.
- Protocol-affecting changes should run the protocol suite.
- Docs lint is a blocking check for push CI.
