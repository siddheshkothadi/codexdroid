# Harness Specs Index

Last updated: 2026-02-14

## Purpose
Trace every feature spec to machine-checkable harness scenarios and fixtures.

## Spec Traceability
| Spec | Scenarios | Fixtures |
| --- | --- | --- |
| `docs/harness/specs/approval_request_response.md` | `protocol.server_requests_has_core_types`, `protocol.server_requests_unknown_method_present` | `harness/fixtures/server_requests.json` |
| `docs/harness/specs/effort_selection_persistence.md` | `smoke.spec_effort_selection_persistence_shape`, `protocol.source_has_effort_fallback_logic` | `docs/harness/specs/effort_selection_persistence.md` |
| `docs/harness/specs/model_selection_persistence.md` | `smoke.spec_model_selection_persistence_shape`, `protocol.source_has_model_config_fallback_logic` | `docs/harness/specs/model_selection_persistence.md` |
| `docs/harness/specs/notification_deep_link.md` | `smoke.spec_notification_deep_link_shape`, `protocol.source_contains_notification_deeplink_contract` | `docs/harness/specs/notification_deep_link.md` |
| `docs/harness/specs/plan_update_rendering.md` | `smoke.spec_plan_update_rendering_shape`, `protocol.notifications_methods_present`, `protocol.plan_update_without_threadid_fixture` | `harness/fixtures/protocol_notifications.json` |
| `docs/harness/specs/reducer_contract.md` | `smoke.reducer_contract_documented`, `protocol.reducer_source_handles_key_methods`, `protocol.reducer_source_unknown_method_tolerance` | `app/src/main/java/me/siddheshkothadi/codexdroid/codex/ThreadEventReducer.kt` |
| `docs/harness/specs/thread_start_resume.md` | `smoke.spec_thread_start_resume_shape`, `protocol.source_contains_thread_start_and_resume` | `app/src/main/java/me/siddheshkothadi/codexdroid/codex/CodexApiService.kt` |
| `docs/harness/specs/turn_start_interrupt.md` | `smoke.spec_turn_start_interrupt_shape`, `protocol.source_contains_turn_interrupt_path` | `app/src/main/java/me/siddheshkothadi/codexdroid/codex/CodexApiService.kt` |
| `docs/harness/specs/user_input_request_response.md` | `protocol.server_requests_has_core_types`, `protocol.source_parses_user_input_request` | `harness/fixtures/server_requests.json` |
