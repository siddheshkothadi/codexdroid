# Fast Loop Playbook

Last updated: 2026-02-14

1. Implement the smallest behavior slice for a feature spec.
2. Run unit tests: `./gradlew testDebugUnitTest`.
3. Run smoke harness with thresholds: `python harness/runners/cli.py eval --suite smoke --enforce-thresholds`.
4. Run protocol harness with thresholds for protocol/lifecycle changes: `python harness/runners/cli.py eval --suite protocol --enforce-thresholds`.
5. Run docs lint: `scripts/ci/docs_lint.ps1`.
6. Build debug APK: `./gradlew assembleDebug`.
7. Push to `main` only after all checks pass.

Use `scripts/dev/start_fast_loop.ps1` to run the same sequence.
Use `scripts/dev/push_main_guard.ps1` for strict pre-push gating.
