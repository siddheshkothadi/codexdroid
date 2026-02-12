# Feature Spec: Notification Deep Link

## Goal
Drive background completion notifications back into the correct thread context.

## Contract
- `turn/completed` emits a local Android notification when app is backgrounded.
- Tap action deep-links to `(connectionId, threadId, turnId)`.

## Acceptance checks
- Foreground mode suppresses completion notifications.
- Notification intent routes to selected thread and scroll target.
