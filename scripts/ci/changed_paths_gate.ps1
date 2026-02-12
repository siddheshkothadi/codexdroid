param(
    [string]$BaseRef = "",
    [string]$HeadRef = "HEAD"
)

$ErrorActionPreference = 'Stop'

function Resolve-BaseRef {
    param([string]$ExplicitBaseRef)

    if ($ExplicitBaseRef -ne "") {
        git fetch --no-tags --depth=1 origin $ExplicitBaseRef | Out-Null
        return "origin/$ExplicitBaseRef"
    }

    if ($env:GITHUB_BASE_REF) {
        git fetch --no-tags --depth=1 origin $env:GITHUB_BASE_REF | Out-Null
        return "origin/$($env:GITHUB_BASE_REF)"
    }

    return "HEAD~1"
}

$resolvedBase = Resolve-BaseRef -ExplicitBaseRef $BaseRef
$changed = @(git diff --name-only "$resolvedBase...$HeadRef")
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to compute changed files between $resolvedBase and $HeadRef"
}

if ($changed.Count -eq 0) {
    $changed = @(git diff --name-only HEAD~1)
}

$needsProtocol = $false
$protocolRegexes = @(
    '^app/src/main/java/.*/codex/',
    '^app/src/main/java/.*/data/',
    '^app/src/main/java/.*/ui/session/',
    '^app/src/test/.*/codex/',
    '^harness/',
    '^docs/harness/specs/reducer_contract\.md$'
)

foreach ($file in $changed) {
    foreach ($pattern in $protocolRegexes) {
        if ($file -match $pattern) {
            $needsProtocol = $true
            break
        }
    }
    if ($needsProtocol) { break }
}

$suites = @('smoke')
if ($needsProtocol) {
    $suites += 'protocol'
}

$suitesCsv = $suites -join ','
Write-Host "Detected suites: $suitesCsv"
Write-Host "Changed files:"
$changed | ForEach-Object { Write-Host "  - $_" }

if ($env:GITHUB_OUTPUT) {
    "suites=$suitesCsv" | Out-File -Append -FilePath $env:GITHUB_OUTPUT -Encoding utf8
}
