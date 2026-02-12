# Feature Spec: Plan Update Rendering

## Goal
Render plan updates in-session without dropping status transitions.

## Contract
- `turn/plan/updated` and `codex/event/plan_update` map to `ThreadItem.PlanUpdate`.
- Plan rows preserve `step` + `status`.

## Acceptance checks
- Plan items appear in thread turn timeline.
- Subsequent updates overwrite by stable plan item key.
