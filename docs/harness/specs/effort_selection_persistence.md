# Feature Spec: Effort Selection Persistence

## Goal
Persist reasoning effort preferences per thread.

## Contract
- Selected effort persists into local thread metadata.
- Turn start payload includes selected effort when set.

## Acceptance checks
- Reopening thread restores effort preference.
- Unsupported efforts are rejected in favor of model-supported options.
