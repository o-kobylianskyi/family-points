param(
    [Parameter(Mandatory=$true)][ValidateRange(1,2147483647)][int]$VersionCode,
    [Parameter(Mandatory=$true)][ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$')][string]$VersionName,
    [string]$GoogleWebClientId = $env:FP_ANDROID_GOOGLE_WEB_CLIENT_ID,
    [string]$OutputDirectory = ""
)
$ErrorActionPreference = 'Stop'

$GoogleWebClientId = if ($GoogleWebClientId) { $GoogleWebClientId.Trim() } else { '' }
if ($GoogleWebClientId -notmatch '^[0-9]+-[a-zA-Z0-9_-]+\.apps\.googleusercontent\.com$') {
    throw 'Missing or invalid Google Web OAuth client ID. Supply -GoogleWebClientId or FP_ANDROID_GOOGLE_WEB_CLIENT_ID (same as VITE_GOOGLE_CLIENT_ID).'
}

Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$android = Join-Path $root 'android'
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root 'android-release-output' }
foreach ($name in @('FP_ANDROID_KEYSTORE_FILE','FP_ANDROID_KEYSTORE_PASSWORD','FP_ANDROID_KEY_ALIAS','FP_ANDROID_KEY_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing $name. Configure release signing outside Git; never publish secrets."
    }
}
$store = [Environment]::GetEnvironmentVariable('FP_ANDROID_KEYSTORE_FILE')
if (-not (Test-Path -LiteralPath $store -PathType Leaf)) { throw "Keystore not found: $store" }
$wrapper = Join-Path $android 'gradlew.bat'
if (-not (Test-Path $wrapper)) { throw 'Gradle wrapper not found' }
Push-Location $android
try {
    $args = @(':app:assembleRelease', "--console=plain", "-PfamilyPointsVersionCode=$VersionCode",
        "-PfamilyPointsVersionName=$VersionName", '-PfamilyPointsReleaseApiUrl=https://family-point.com/')
    if ($GoogleWebClientId) { $args += "-PfamilyPointsGoogleWebClientId=$GoogleWebClientId" }
    & $wrapper @args
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)" }
} finally { Pop-Location }
$apk = Join-Path $android 'app/build/outputs/apk/release/app-release.apk'
if (-not (Test-Path $apk -PathType Leaf)) { throw 'Signed APK missing: app-release.apk (check signing)' }
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
if (-not $androidHome) { throw 'ANDROID_HOME or ANDROID_SDK_ROOT required to verify the release signature' }
$verifier = Get-ChildItem (Join-Path $androidHome 'build-tools') -Directory |
    Sort-Object { [version]($_.Name -replace '-.*$','') } -Descending |
    ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
    Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $verifier) { throw 'apksigner.bat not found in Android build-tools' }
& $verifier verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$dest = Join-Path $OutputDirectory 'familypoints-beta.apk'
Copy-Item -LiteralPath $apk -Destination $dest -Force
$sha = (Get-FileHash -Algorithm SHA256 -LiteralPath $dest).Hash.ToLowerInvariant()
$size = (Get-Item -LiteralPath $dest).Length
$manifest = [ordered]@{
    versionCode = $VersionCode
    versionName = $VersionName
    file = 'familypoints-beta.apk'
    sha256 = $sha
    sizeBytes = $size
    applicationId = 'com.olehkobylianskyi.familypoints.android'
    minSdk = 26
    downloadUrl = 'https://family-point.com/downloads/familypoints-beta.apk'
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'familypoints-beta.json') -Encoding utf8
"$sha  familypoints-beta.apk" | Set-Content -LiteralPath (Join-Path $OutputDirectory 'familypoints-beta.apk.sha256') -Encoding ascii
Write-Host "SIGNED APK: $dest"
Write-Host "SHA256: $sha"
Write-Host "MANIFEST: $(Join-Path $OutputDirectory 'familypoints-beta.json')"
Write-Host 'IMPORTANT: retain the keystore forever for future in-place upgrades.'
