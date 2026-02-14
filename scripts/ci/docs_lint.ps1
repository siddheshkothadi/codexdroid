param(
    [string]$Root = "."
)

$ErrorActionPreference = "Stop"

function Add-Error {
    param(
        [System.Collections.Generic.List[string]]$Errors,
        [string]$Message
    )
    $Errors.Add($Message) | Out-Null
}

function Test-RelativeMarkdownLinks {
    param(
        [string]$FilePath,
        [string]$Content,
        [System.Collections.Generic.List[string]]$Errors
    )

    $matches = [regex]::Matches($Content, "\[[^\]]+\]\(([^)]+)\)")
    foreach ($match in $matches) {
        $target = $match.Groups[1].Value.Trim()
        if ($target -eq "") { continue }
        if ($target.StartsWith("http://") -or $target.StartsWith("https://") -or $target.StartsWith("mailto:")) { continue }
        if ($target.StartsWith("#")) { continue }

        $pathOnly = $target.Split("#")[0]
        if ($pathOnly -eq "") { continue }

        $resolved = Join-Path (Split-Path -Parent $FilePath) $pathOnly
        if (-not (Test-Path $resolved)) {
            Add-Error -Errors $Errors -Message "Broken link in $FilePath -> $target"
        }
    }
}

$errors = New-Object System.Collections.Generic.List[string]
$rootPath = (Resolve-Path $Root).Path
$docsRoot = Join-Path $rootPath "docs/harness"
$specRoot = Join-Path $docsRoot "specs"
$playbookRoot = Join-Path $docsRoot "playbooks"
$specIndexPath = Join-Path $specRoot "index.md"

if (-not (Test-Path $docsRoot)) {
    Write-Error "Harness docs root not found: $docsRoot"
}

$requiredDocs = @(
    "docs/harness/ARCHITECTURE.md",
    "docs/harness/RELIABILITY.md",
    "docs/harness/SECURITY.md",
    "docs/harness/QUALITY_SCORECARD.md",
    "docs/harness/specs/index.md"
)

foreach ($rel in $requiredDocs) {
    $full = Join-Path $rootPath $rel
    if (-not (Test-Path $full)) {
        Add-Error -Errors $errors -Message "Missing required harness doc: $rel"
    }
}

if (Test-Path $specIndexPath) {
    $specIndexContent = Get-Content $specIndexPath -Raw
    if ($specIndexContent -notmatch "Last updated:\s*\d{4}-\d{2}-\d{2}") {
        Add-Error -Errors $errors -Message "Missing or invalid Last updated metadata: docs/harness/specs/index.md"
    }
    if ($specIndexContent -notmatch "## Spec Traceability") {
        Add-Error -Errors $errors -Message "Missing required heading in docs/harness/specs/index.md: ## Spec Traceability"
    }
}
else {
    $specIndexContent = ""
}

$specFiles = Get-ChildItem $specRoot -Filter "*.md" | Where-Object { $_.Name -ne "index.md" }
foreach ($spec in $specFiles) {
    $content = Get-Content $spec.FullName -Raw
    $rel = "docs/harness/specs/$($spec.Name)"
    if ($content -notmatch "Last updated:\s*\d{4}-\d{2}-\d{2}") {
        Add-Error -Errors $errors -Message "Missing or invalid Last updated metadata: $rel"
    }
    foreach ($requiredHeading in @("## Goal", "## Contract", "## Acceptance checks")) {
        if ($content -notmatch [regex]::Escape($requiredHeading)) {
            Add-Error -Errors $errors -Message "Missing required heading in ${rel}: $requiredHeading"
        }
    }
    if ($specIndexContent -notmatch [regex]::Escape($rel)) {
        Add-Error -Errors $errors -Message "Spec not mapped in docs/harness/specs/index.md: $rel"
    }
}

$playbookFiles = Get-ChildItem $playbookRoot -Filter "*.md"
foreach ($playbook in $playbookFiles) {
    $content = Get-Content $playbook.FullName -Raw
    $rel = "docs/harness/playbooks/$($playbook.Name)"
    if ($content -notmatch "Last updated:\s*\d{4}-\d{2}-\d{2}") {
        Add-Error -Errors $errors -Message "Missing or invalid Last updated metadata: $rel"
    }
}

$allMarkdown = Get-ChildItem $docsRoot -Recurse -Filter "*.md"
foreach ($doc in $allMarkdown) {
    $content = Get-Content $doc.FullName -Raw
    Test-RelativeMarkdownLinks -FilePath $doc.FullName -Content $content -Errors $errors
}

if ($errors.Count -gt 0) {
    Write-Host "[docs-lint] Found $($errors.Count) issue(s):"
    foreach ($err in $errors) {
        Write-Host " - $err"
    }
    exit 1
}

Write-Host "[docs-lint] OK"
