# Harness Security

Last updated: 2026-02-14

## Security Posture
Harness checks focus on protocol handling safety and local data hygiene for the Android client.

## Controls
1. Unknown request methods must route to safe unknown-attention handling.
2. Unknown or malformed notification payloads must no-op instead of corrupting state.
3. Approval and user-input request payload parsing must be strict enough to reject invalid shapes.
4. Secrets are not stored in plaintext in harness fixtures.

## Verification
1. Unit tests validate request parsing and queue semantics.
2. Protocol scenarios validate expected request/notification method coverage.
3. Docs and specs are required for security-impacting behavior changes.

## Change Management
1. Any new tool/request method requires:
   - spec update
   - fixture update
   - harness scenario update
2. Security-relevant regressions are addressed via immediate fix-forward on `main`.
