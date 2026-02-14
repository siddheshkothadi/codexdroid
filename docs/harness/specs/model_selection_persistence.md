# Feature Spec: Model Selection Persistence

Last updated: 2026-02-14

## Goal
Keep model preference sticky at thread scope.

## Contract
- Selected model id persists into local thread metadata.
- New turns inherit persisted model id.

## Acceptance checks
- Reopening thread restores previously selected model id.
- If selected model is unavailable, fallback selection is deterministic.
