# Feature Spec: Approval Request Response

## Goal
Process server approval prompts with explicit user decisions.

## Contract
- Approval requests enqueue as pending attention.
- Respond via JSON result `{ "decision": "accept" | "decline" }`.

## Acceptance checks
- Approve and decline both clear active approval prompt.
- Queued approval requests are handled FIFO.
