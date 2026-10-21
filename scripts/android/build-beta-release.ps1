param(
  [Parameter(Mandatory=$true)][int]$VersionCode,
  [Parameter(Mandatory=$true)][string]$VersionName,
  [string]$ApiUrl = "https://family-point.com/"
)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "../../android")
$required = @("FP_ANDROID_KEYSTORE_FILE","FP_ANDROID_KEYSTORE_PASSWORD","FP_ANDROID_KEY_ALIAS","FP_ANDROID_KEY_PASSWORD")
$missing = @($required | Where-Object { -not [Environment]::GetEnvironmentVariable($_) })
if ($missing.Count -gt 0) { throw "Missing signing environment variables: $($missing -join ', ')" }
if ($VersionCode -lt 1) { throw "VersionCode must be >=1 and increase on every installable release" }
if (-not $ApiUrl.StartsWith("https://")) { throw "Release API endpoint must be HTTPS" }
& .\gradlew.bat :app:assembleRelease "-PfamilyPointsVersionCode=$VersionCode" "-PfamilyPointsVersionName=$VersionName" "-PfamilyPointsReleaseApiUrl=$ApiUrl"
if ($LASTEXITCODE -ne 0) { throw "Android release build failed" }
$apk = Join-Path (Get-Location) "app/build/outputs/apk/release/app-release.apk"
if (!(Test-Path $apk)) { throw "APK was not generated" }
Write-Host "Installable signed APK: $apk"
Write-Host "SHA256: $((Get-FileHash -Algorithm SHA256 $apk).Hash)"
Write-Host "For phone updates, reuse the SAME signing key and increase VersionCode."
