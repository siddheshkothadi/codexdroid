# CodexDroid

CodexDroid is an Android client for OpenAI Codex. It connects to the stream of events from `codex app-server` via WebSocket.

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
  <img src="https://github.com/user-attachments/assets/34045392-e12f-4b01-b6b0-a0c3e2ecbcdf" alt="Setup" width="260" />
  <img src="https://github.com/user-attachments/assets/e00e1626-c2f3-4b3e-a9dc-47cd5b29fdcf" alt="Drawer" width="260" />
  <img src="https://github.com/user-attachments/assets/8e4dcf61-c170-4bd7-b56d-649a4cdd5ead" alt="Thread" width="260" />
</p>


