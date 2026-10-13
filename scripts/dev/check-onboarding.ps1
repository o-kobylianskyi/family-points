# DEV-only consolidated onboarding check. Does not access or migrate any database.
param([switch]$WithTests)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $root
try {
    Write-Host '=== FamilyPoints DEV onboarding checkpoint ==='
    & git status --short
    if ($LASTEXITCODE -ne 0) { throw 'git status failed' }

    $migrations = Get-ChildItem 'src/main/resources/db/migration' -Filter 'V*__*.sql'
    $versions = @($migrations | ForEach-Object {
        if ($_.Name -match '^V([0-9]+)__') { [int]$Matches[1] }
    } | Sort-Object)
    if ($versions.Count -eq 0) { throw 'No Flyway migrations found' }
    if ($versions[-1] -lt 27) { throw 'Expected V27 invitation tables' }
    if (@($versions | Group-Object | Where-Object Count -gt 1).Count -gt 0) {
        throw 'Duplicate Flyway versions found'
    }
    Write-Host ('Flyway SQL: ' + $versions.Count + ' versions; latest V' + $versions[-1])

    $wrapper = '.\mvnw.cmd'
    if (-not (Test-Path $wrapper)) { throw 'Maven wrapper missing' }
    Write-Host 'Compiling Backend without accessing DEV PostgreSQL...'
    & $wrapper '-DskipTests' 'compile'
    if ($LASTEXITCODE -ne 0) { throw 'Backend compilation failed' }

    Write-Host 'Building React frontend...'
    Push-Location (Join-Path $root 'frontend')
    try {
        if (-not (Test-Path 'node_modules')) {
            Write-Host 'Installing locked frontend dependencies...'
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw 'Frontend npm ci failed' }
        }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    } finally {
        Pop-Location
    }

    if ($WithTests) {
        Write-Host 'Running existing unit tests (integration tests may require Docker)...'
        & $wrapper 'test'
        if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' }
    }
    Write-Host 'PASS: static Flyway inventory and backend compilation'
} finally {
    Pop-Location
}
