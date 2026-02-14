# Reducer Contract

Last updated: 2026-02-14

## Goal
Define non-negotiable correctness invariants for `ThreadEventReducer`.

## Contract
`ThreadEventReducer` must satisfy these invariants:

1. **Thread safety by id**
   Notification handlers may mutate a thread only when payload `threadId` maps to the current thread.

2. **Turn upsert semantics**
   Turn lifecycle notifications (`turn/started`, `turn/completed`) must upsert by turn id and preserve existing streamed items when payload turns are sparse.

3. **Item idempotency**
   Applying the same item update notification multiple times must not duplicate items with the same `item.id`.

4. **Monotonic deltas**
   Delta notifications (`agentMessage`, `reasoning`, `commandExecution`, `fileChange`, `mcpToolCall`) append state but never truncate previously accumulated content.

5. **Plan update visibility**
   `turn/plan/updated` and `codex/event/plan_update` must produce a `ThreadItem.PlanUpdate` item addressable by stable keys.

6. **Unknown method tolerance**
   Unsupported methods must no-op and return the original thread.

## Acceptance checks
- Unit tests cover each invariant with deterministic fixtures.
- Protocol harness scenarios include out-of-order and threadId-missing plan update examples.
