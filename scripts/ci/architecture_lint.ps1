param()

$ErrorActionPreference = "Stop"

function Assert-NoMatches {
    param(
        [string]$Label,
        [string]$Pattern,
        [string[]]$Targets
    )

    $rg = Get-Command rg -ErrorAction SilentlyContinue
    if ($rg) {
        $results = & rg -n --pcre2 $Pattern @Targets
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[architecture-lint] Violation: $Label" -ForegroundColor Red
            Write-Host $results
            exit 1
        }
        if ($LASTEXITCODE -gt 1) {
            throw "Failed to run architecture lint for $Label"
        }
        return
    }

    $matches = @()
    foreach ($target in $Targets) {
        if (Test-Path $target -PathType Container) {
            $files = Get-ChildItem -Path $target -Recurse -File -Filter *.kt
            foreach ($file in $files) {
                $hits = Select-String -Path $file.FullName -Pattern $Pattern -AllMatches
                $matches += $hits
            }
        } elseif (Test-Path $target -PathType Leaf) {
            $hits = Select-String -Path $target -Pattern $Pattern -AllMatches
            $matches += $hits
        }
    }

    if ($matches.Count -gt 0) {
        Write-Host "[architecture-lint] Violation: $Label" -ForegroundColor Red
        $matches | ForEach-Object { Write-Host "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
        exit 1
    }
}

$uiTargets = @(
    "app/src/main/java/me/siddheshkothadi/codexdroid/feature",
    "app/src/main/java/me/siddheshkothadi/codexdroid/ui/navigation",
    "app/src/main/java/me/siddheshkothadi/codexdroid/MainViewModel.kt"
)

$domainTargets = @(
    "app/src/main/java/me/siddheshkothadi/codexdroid/domain"
)

Assert-NoMatches `
    -Label "UI layer importing data layer types" `
    -Pattern "import\\s+me\\.siddheshkothadi\\.codexdroid\\.data\\." `
    -Targets $uiTargets

Assert-NoMatches `
    -Label "UI layer importing CodexApiService directly" `
    -Pattern "import\\s+me\\.siddheshkothadi\\.codexdroid\\.codex\\.CodexApiService" `
    -Targets $uiTargets

Assert-NoMatches `
    -Label "Domain layer importing UI layer types" `
    -Pattern "import\\s+me\\.siddheshkothadi\\.codexdroid\\.(ui|feature\\..*\\.ui)\\." `
    -Targets $domainTargets

Write-Host "[architecture-lint] Passed"
