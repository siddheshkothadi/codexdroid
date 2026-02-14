# Release Review Playbook

Last updated: 2026-02-14

1. Run nightly harness suites (`smoke` + `protocol`) with threshold enforcement.
2. Review failures by scenario id and fixture trace references.
3. If `main` is red after a direct push, apply immediate fix-forward commit.
4. Confirm feature changes include spec + scenario updates.
5. Publish weekly harness KPI summary:
   - smoke pass rate
   - protocol pass rate
   - median scenario latency
   - flaky scenarios
