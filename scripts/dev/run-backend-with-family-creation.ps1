# Start local DEV backend with additional-family creation enabled for this process only.
# Inherits DB_PASSWORD, JWT_SECRET and other DEV settings from this terminal.
# Never run this helper on BETA/PROD. Stop with Ctrl+C to end the process.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

if (-not $env:DB_PASSWORD -or -not $env:JWT_SECRET) {
    throw 'Set DEV DB_PASSWORD and JWT_SECRET in this terminal first. No defaults or BETA credentials.'
}

$env:FAMILYPOINTS_ADDITIONAL_FAMILY_ENABLED = 'true'
Write-Host 'Starting local FamilyPoints DEV with additional-family creation enabled for this process.'
Write-Host 'No Contabo deployment or production configuration is changed.'

Push-Location $repo
try {
    & '.\mvnw.cmd' 'spring-boot:run'
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
    Remove-Item Env:FAMILYPOINTS_ADDITIONAL_FAMILY_ENABLED -ErrorAction SilentlyContinue
}
