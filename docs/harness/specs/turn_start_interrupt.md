# Feature Spec: Turn Start/Interrupt

Last updated: 2026-02-14

## Goal
Support fast, predictable turn lifecycle transitions.

## Contract
- `turn/start` sends input plus optional `cwd`, `model`, and `effort`.
- `turn/interrupt` is available while a turn is in progress.
- UI sending indicator clears on terminal turn states.

## Acceptance checks
- Turn id from `turn/start` is stored as active turn.
- Interrupt requests use current `threadId` + `turnId`.
