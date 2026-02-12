param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

Write-Host "[fast-loop] Running unit tests"
./gradlew testDebugUnitTest

Write-Host "[fast-loop] Running smoke harness suite"
python harness/runners/cli.py eval --suite smoke --report harness/reports/fast_smoke_report.json

if (-not $SkipBuild) {
    Write-Host "[fast-loop] Building debug APK"
    ./gradlew assembleDebug
}

Write-Host "[fast-loop] Completed"
