# Release Review Playbook

1. Run nightly harness suites (`smoke` + `protocol`).
2. Review failures by scenario id and fixture trace references.
3. Confirm protocol-affecting changes include fixture/test updates.
4. Publish weekly harness KPI summary:
   - smoke pass rate
   - protocol pass rate
   - median scenario latency
   - flaky scenarios
