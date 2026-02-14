$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path "$PSScriptRoot/../..").Path
$hooksPath = Join-Path $repoRoot ".githooks"
$prePushPath = Join-Path $hooksPath "pre-push"

if (-not (Test-Path $hooksPath)) {
    New-Item -ItemType Directory -Path $hooksPath | Out-Null
}

if (-not (Test-Path $prePushPath)) {
    Write-Error "Expected hook file not found: $prePushPath"
}

git config core.hooksPath .githooks
Write-Host "[hooks] Installed core.hooksPath=.githooks"
Write-Host "[hooks] pre-push will run scripts/dev/push_main_guard.ps1"
