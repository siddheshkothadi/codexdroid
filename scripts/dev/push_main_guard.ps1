param(
    [switch]$SkipBuild,
    [switch]$UsePathGateForProtocol
)

$ErrorActionPreference = "Stop"

function Run-Step {
    param(
        [string]$Label,
        [scriptblock]$Action
    )
    Write-Host "[push-guard] $Label"
    & $Action
}

function Should-RunProtocolSuite {
    if (-not $UsePathGateForProtocol) {
        return $true
    }

    $gateOutput = & "$PSScriptRoot/../ci/changed_paths_gate.ps1"
    $gateText = ($gateOutput | Out-String)
    return $gateText -match "Detected suites:\s*smoke,protocol"
}

Run-Step -Label "Unit tests" -Action { ./gradlew testDebugUnitTest }

Run-Step -Label "Smoke harness (threshold enforced)" -Action {
    python harness/runners/cli.py eval --suite smoke --enforce-thresholds --report harness/reports/local_smoke_report.json
}

if (Should-RunProtocolSuite) {
    Run-Step -Label "Protocol harness (threshold enforced)" -Action {
        python harness/runners/cli.py eval --suite protocol --enforce-thresholds --report harness/reports/local_protocol_report.json
    }
}
else {
    Write-Host "[push-guard] Skipping protocol suite (path gate not triggered)"
}

Run-Step -Label "Harness docs lint" -Action { & "$PSScriptRoot/../ci/docs_lint.ps1" }
Run-Step -Label "Architecture lint" -Action { & "$PSScriptRoot/../ci/architecture_lint.ps1" }

if (-not $SkipBuild) {
    Run-Step -Label "Assemble debug APK" -Action { ./gradlew assembleDebug }
}

Write-Host "[push-guard] Completed successfully"
