# AGENTS Index

This file is the agent-facing map of the repository, aligned with harness-first engineering.

## Project map

1. Android app code: `app/src/main/java/me/siddheshkothadi/codexdroid/`
2. Session UI + orchestration: `app/src/main/java/me/siddheshkothadi/codexdroid/ui/session/`
3. Codex protocol + routing: `app/src/main/java/me/siddheshkothadi/codexdroid/codex/`
4. Data layer (Room/repositories): `app/src/main/java/me/siddheshkothadi/codexdroid/data/`
5. Domain layer (models/use cases): `app/src/main/java/me/siddheshkothadi/codexdroid/domain/`
6. Dependency injection: `app/src/main/java/me/siddheshkothadi/codexdroid/di/`

## Architecture rules

1. Layering is strict: `UI -> Domain -> Data`.
2. UI (`ui/*` + `MainViewModel`) must not import `data/*` or `CodexApiService` directly.
3. UI talks to domain via use cases.
4. Room is source of truth for `connections` and `threads`.
5. DataStore is only for lightweight app preference/migration state.
6. Compose state collection should use lifecycle-aware APIs (`collectAsStateWithLifecycle`).
7. Coroutines dispatchers are provided through DI qualifiers in `di/CoroutineModule.kt`.

## Architecture guardrails

1. Architecture lint script: `scripts/ci/architecture_lint.ps1`
2. Local guard script includes architecture lint: `scripts/dev/push_main_guard.ps1`
3. Fast loop includes architecture lint: `scripts/dev/start_fast_loop.ps1`
4. CI workflows run architecture lint before harness evaluations.

## Harness map

1. Harness specs: `docs/harness/specs/`
2. Harness playbooks: `docs/harness/playbooks/`
3. Harness architecture + governance docs: `docs/harness/ARCHITECTURE.md`, `docs/harness/RELIABILITY.md`, `docs/harness/SECURITY.md`, `docs/harness/QUALITY_SCORECARD.md`
4. Harness runner: `harness/runners/cli.py`
5. Scenarios: `harness/scenarios/`
6. Fixtures: `harness/fixtures/`
7. Gate config: `harness/config/gates.json`
8. Report schema: `harness/reports/schema.json`

## CI map

1. Fast guardrails: `.github/workflows/android_fast.yml`
2. Protocol checks: `.github/workflows/android_protocol.yml`
3. Nightly checks: `.github/workflows/android_nightly.yml`

## Fast developer loop

1. `./gradlew testDebugUnitTest`
2. `python harness/runners/cli.py eval --suite smoke`
3. `scripts/ci/docs_lint.ps1`
4. `scripts/ci/architecture_lint.ps1`
5. `./gradlew assembleDebug`

Use `scripts/dev/start_fast_loop.ps1` to run the same sequence.
Use `scripts/dev/push_main_guard.ps1` before direct pushes to `main`.

## Path-based harness gating

Use `scripts/ci/changed_paths_gate.ps1` to detect whether `protocol` suite is required in addition to `smoke`.
