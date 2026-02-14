# Feature Spec: Thread Start/Resume

Last updated: 2026-02-14

## Goal
Guarantee deterministic session bootstrapping for new and existing threads.

## Contract
- New session uses `thread/start` with `source=appServer` and optional `cwd`.
- Existing session uses `thread/resume` before `turn/start`.
- Thread metadata is persisted locally after start/resume.

## Acceptance checks
- New thread path creates one local thread row keyed by `(connectionId, threadId)`.
- Resume path does not create duplicate threads.
