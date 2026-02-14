# CodexDroid

CodexDroid is an Android client for OpenAI Codex. It connects to the stream of events from `codex app-server` via WebSocket.

## Features

- Workspace-first session list with folder-level grouping (by `cwd`).
- Session/thread management directly from the drawer:
  - rename sessions/threads
  - delete sessions/threads
  - search sessions and folders
- Multiple Codex connections support:
  - save multiple servers
  - switch active connection
  - edit/delete connections
- Text-to-speech for assistant responses:
  - Sarvam AI TTS integration (Bulbul v3)
  - configurable voice and speech controls in Settings
  - Android TTS fallback when Sarvam is unavailable

## Architecture

The app follows layered architecture with explicit dependency flow:

- `UI -> Domain -> Data`
- UI (`ViewModel` + Compose screens) depends only on domain use cases and UI models.
- Domain exposes use cases, models, and repository contracts (`domain/repository`).
- Data owns persistence/network implementations and repository internals that implement domain contracts.

### Directory map

- `app/src/main/java/me/siddheshkothadi/codexdroid/feature/session/ui/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/feature/history/ui/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/feature/setup/ui/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/feature/shared/ui/components/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/ui/navigation/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/domain/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/domain/repository/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/data/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/data/source/`
- `app/src/main/java/me/siddheshkothadi/codexdroid/codex/` (transport/runtime wiring)
- `app/src/main/java/me/siddheshkothadi/codexdroid/di/`

### Data strategy

- Room is the source of truth for structured app data:
  - `connections`
  - `threads`
- Repository implementations use explicit local/remote data sources:
  - `data/source/local/*`
  - `data/source/remote/*`
- DataStore is now limited to lightweight preference/state use:
  - legacy connection import marker
  - migration bridge data (one-time import path)
- Legacy DataStore connections are migrated to Room at app startup.

### State and lifecycle

- Screen state is exposed as `StateFlow` from `ViewModel`.
- Compose uses lifecycle-aware collection via `collectAsStateWithLifecycle()`.
- Network/database operations run through domain use cases from `ViewModel`.

## Run

- Set the value of `CODEX_HTTP_SECRET` environment variable
- Start the server: `npx codex-app-server@latest`
- Open the app and enter `endpoint`, `secret`, and `name`, then get started

## Fast Loop

- Run fast local checks: `scripts/dev/start_fast_loop.ps1`
- Run strict local push gate (recommended before direct pushes to `main`): `scripts/dev/push_main_guard.ps1`
- Run smoke harness directly: `python harness/runners/cli.py eval --suite smoke --enforce-thresholds`
- Run protocol harness directly: `python harness/runners/cli.py eval --suite protocol --enforce-thresholds`
- Install pre-push hook (optional): `scripts/dev/install_githooks.ps1`
- Run architecture boundary checks directly: `scripts/ci/architecture_lint.ps1`

## Harness

- Specs and playbooks: `docs/harness/`
- Architecture/reliability/security scorecards:
  - `docs/harness/ARCHITECTURE.md`
  - `docs/harness/RELIABILITY.md`
  - `docs/harness/SECURITY.md`
  - `docs/harness/QUALITY_SCORECARD.md`
- Harness runner: `harness/runners/cli.py`
- Scenario suites: `harness/scenarios/`
- Deterministic fixtures: `harness/fixtures/`

## CI Workflows

- Fast required checks: `.github/workflows/android_fast.yml`
- Protocol-focused checks: `.github/workflows/android_protocol.yml`
- Nightly trend checks: `.github/workflows/android_nightly.yml`

## Screenshots

<p>
  <img src="https://github.com/user-attachments/assets/91d01f8d-a439-4cea-b41b-93d53304f1e1" alt="Setup" width="260" />
  <img src="https://github.com/user-attachments/assets/4f2bffa8-775f-4880-8c68-939fac8156b6" alt="Thread" width="260" />
  <img src="https://github.com/user-attachments/assets/ae8d7439-a9ec-457c-ad39-c859119955d7" alt="Thread" width="260" />
  <img src="https://github.com/user-attachments/assets/45f552a7-78a2-40aa-9d3c-5c4fcb3feda5" alt="Drawer" width="260" />
</p>


