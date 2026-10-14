# Read-only preflight: inspect configured DEV values without exposing credentials.
param([switch]$RequireGoogle, [switch]$RequireSmtp)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$passed = $true
function Test-Variable([string]$name) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Host ("MISSING: " + $name) -ForegroundColor Yellow
        return $false
    }
    Write-Host ("SET: " + $name + " (value hidden)") -ForegroundColor Green
    return $true
}
Write-Host '=== FamilyPoints DEV auth environment ==='
if ($RequireSmtp) {
    foreach ($name in @('FAMILYPOINTS_MAIL_FROM', 'FAMILYPOINTS_WEB_URL', 'SMTP_HOST', 'SMTP_USERNAME', 'SMTP_PASSWORD')) {
        if (-not (Test-Variable $name)) { $passed = $false }
    }
    $webUrl = [Environment]::GetEnvironmentVariable('FAMILYPOINTS_WEB_URL', 'Process')
    if ($webUrl -and -not ($webUrl -match '^https://|^http://localhost(?::\d+)?/?$|^http://127\.0\.0\.1(?::\d+)?/?$')) {
        Write-Host 'Check callback URL: HTTPS is required outside local DEV' -ForegroundColor Yellow
        $passed = $false
    }
}
if ($RequireGoogle) {
    if (-not (Test-Variable 'GOOGLE_CLIENT_ID')) { $passed = $false }
    $viteEnv = Join-Path $root 'frontend\.env.local'
    if (Test-Path $viteEnv) {
        $clientLine = Get-Content $viteEnv | Where-Object { $_ -match '^\s*VITE_GOOGLE_CLIENT_ID\s*=' } | Select-Object -First 1
        if (-not $clientLine) { Write-Host 'MISSING: VITE_GOOGLE_CLIENT_ID in frontend/.env.local'; $passed = $false }
        else { Write-Host 'SET: frontend/.env.local Google client ID (value hidden)' }
    } else {
        Write-Host 'No frontend/.env.local found; set VITE_GOOGLE_CLIENT_ID in Vite environment instead.'
    }
}
if (-not $RequireSmtp -and -not $RequireGoogle) {
    Write-Host 'Use -RequireSmtp -RequireGoogle to check provider settings.'
}
if (-not $passed) { exit 1 }
Write-Host 'Environment preflight OK. This does not test delivery or live OAuth.' -ForegroundColor Green
