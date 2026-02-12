# Fast Loop Playbook

1. Implement the smallest behavior slice for a feature spec.
2. Run unit tests: `./gradlew testDebugUnitTest`.
3. Run smoke harness: `python harness/runners/cli.py eval --suite smoke`.
4. Build debug APK: `./gradlew assembleDebug`.
5. Push after all fast checks pass.

Use `scripts/dev/start_fast_loop.ps1` to run the same sequence.
