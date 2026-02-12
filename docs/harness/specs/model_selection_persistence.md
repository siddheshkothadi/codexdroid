# Feature Spec: Model Selection Persistence

## Goal
Keep model preference sticky at thread scope.

## Contract
- Selected model id persists into local thread metadata.
- New turns inherit persisted model id.

## Acceptance checks
- Reopening thread restores previously selected model id.
- If selected model is unavailable, fallback selection is deterministic.
