param(
    [switch]$SkipBuild,
    [switch]$IncludeProtocol
)

$ErrorActionPreference = 'Stop'

Write-Host "[fast-loop] Running unit tests"
./gradlew testDebugUnitTest

Write-Host "[fast-loop] Running smoke harness suite"
python harness/runners/cli.py eval --suite smoke --enforce-thresholds --report harness/reports/fast_smoke_report.json

if ($IncludeProtocol) {
    Write-Host "[fast-loop] Running protocol harness suite"
    python harness/runners/cli.py eval --suite protocol --enforce-thresholds --report harness/reports/fast_protocol_report.json
}

Write-Host "[fast-loop] Linting harness docs"
& "$PSScriptRoot/../ci/docs_lint.ps1"

Write-Host "[fast-loop] Linting architecture boundaries"
& "$PSScriptRoot/../ci/architecture_lint.ps1"

if (-not $SkipBuild) {
    Write-Host "[fast-loop] Building debug APK"
    ./gradlew assembleDebug
}

Write-Host "[fast-loop] Completed"
