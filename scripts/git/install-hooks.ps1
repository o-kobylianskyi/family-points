# Install FP-SAFE-001 for this checkout (Git for Windows / Git Bash).
$ErrorActionPreference = "Stop"
$root = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or -not $root) { throw "Run inside the FamilyPoints repository." }
$hooksPath = (& git -C $root rev-parse --git-path hooks).Trim()
if (-not [System.IO.Path]::IsPathRooted($hooksPath)) { $hooksPath = Join-Path $root $hooksPath }
New-Item -ItemType Directory -Force -Path $hooksPath | Out-Null
$hook = Join-Path $hooksPath "pre-commit"
if (Test-Path $hook) {
    $current = [System.IO.File]::ReadAllText($hook)
    if (-not $current.Contains("FP-SAFE-001")) {
        throw "An existing pre-commit hook was found. Integrate manually; it was not overwritten: $hook"
    }
}
$script = @'
#!/usr/bin/env bash
# FP-SAFE-001
exec bash "$(git rev-parse --show-toplevel)/scripts/git/safety-check.sh"
'@
[System.IO.File]::WriteAllText($hook, ($script -replace "`r`n","`n") + "`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "FP-SAFE-001 installed: $hook"
Write-Host "Rules: protect critical paths; allow at most 20 staged file deletions per commit."
