param(
    [ValidateSet('menu','setup','status','build','install','logs','publish','all')]
    [string]$Action = 'menu',
    [int]$VersionCode = 0
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$configPath = Join-Path $PSScriptRoot 'release-config.local.json'
$templatePath = Join-Path $PSScriptRoot 'release-config.example.json'

function Expand-Setting([string]$value) {
    return [Environment]::ExpandEnvironmentVariables($value)
}
function Read-Config {
    if (-not (Test-Path -LiteralPath $configPath)) {
        Copy-Item -LiteralPath $templatePath -Destination $configPath
        Write-Host "Created local config: $configPath"
        Write-Host 'Edit the config with the OAuth WEB Client ID. Passwords must not be saved there.'
    }
    $config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($config.googleWebClientId -notmatch '^[0-9]+-[A-Za-z0-9_-]+[.]apps[.]googleusercontent[.]com$') {
        Write-Host "Configuration needs your Google OAuth WEB Client ID: $configPath"
        $id = Read-Host 'Paste WEB Client ID (not Android client)'
        if ($id -notmatch '^[0-9]+-[A-Za-z0-9_-]+[.]apps[.]googleusercontent[.]com$') {
            throw 'Invalid WEB Client ID; no build or publication performed.'
        }
        $config.googleWebClientId = $id.Trim()
        $config | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $configPath -Encoding UTF8
    }
    return $config
}
function Get-PublishedVersion($config) {
    # Windows PowerShell 5.1 can expose IWR.Content unexpectedly in a pipeline.
    # curl.exe avoids object/string coercion and works with this site's JSON endpoint.
    $lines = @(& curl.exe --fail --silent --show-error --location --max-time 20 --header 'Accept: application/json' --header 'Cache-Control: no-cache' -- $config.manifestUrl)
    if ($LASTEXITCODE -ne 0) { throw 'Could not read the published APK manifest (curl.exe failed).' }
    $raw = ($lines -join [Environment]::NewLine).Trim().TrimStart([char]0xFEFF)
    if (-not $raw.StartsWith('{')) {
        throw 'Published APK manifest did not return a JSON object. Check URL and site routing.'
    }
    try { $parsed = ConvertFrom-Json -InputObject $raw -ErrorAction Stop }
    catch { throw 'Public APK manifest contains invalid JSON. Check server response.' }
    if ($null -eq $parsed -or $null -eq $parsed.PSObject.Properties['versionCode']) {
        throw 'Public Android manifest has no versionCode; cannot choose release number.'
    }
    $code = [int]$parsed.versionCode
    if ($code -le 0) { throw 'Published versionCode must be positive.' }
    return $code
}
function Set-AndroidSdk($config) {
    $sdk = Expand-Setting $config.androidSdk
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) { throw "Android SDK/ADB missing: $adb" }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
}
function Get-ApkSigner {
    $directory = Join-Path $env:ANDROID_HOME 'build-tools'
    $tools = @(Get-ChildItem $directory -Directory -ErrorAction Stop |
      Sort-Object { try { [version]($_.Name -replace '-.*$','') } catch { [version]'0.0' } } -Descending |
      ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
      Where-Object { Test-Path -LiteralPath $_ })
    if ($tools.Count -eq 0) { throw 'Android apksigner.bat is missing.' }
    return $tools[0]
}
function Test-ApkCertificate($config) {
    Set-AndroidSdk $config
    $apk = Join-Path $root 'android-release-output\familypoints-beta.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw "Build APK first: $apk" }
    $signer = Get-ApkSigner
    $result = & $signer verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $match = [regex]::Match(($result -join [Environment]::NewLine), 'certificate SHA-256 digest:\s*([0-9A-Fa-f]+)')
    if (-not $match.Success) { throw 'APK certificate fingerprint not found.' }
    $expected = ([string]$config.expectedCertificateSha256 -replace '[^0-9A-Fa-f]', '').ToLowerInvariant()
    if ($expected.Length -ne 64 -or $match.Groups[1].Value.ToLowerInvariant() -ne $expected) {
        throw 'Incorrect signing certificate. Publication is blocked.'
    }
    Write-Host 'APK signing certificate VERIFIED.'
}
function Read-LocalManifest {
    $file = Join-Path $root 'android-release-output\familypoints-beta.json'
    if (-not (Test-Path -LiteralPath $file)) { throw 'Missing local manifest. Build first.' }
    return (Get-Content -LiteralPath $file -Raw -Encoding UTF8 | ConvertFrom-Json)
}
function Build-Release($config) {
    $published = Get-PublishedVersion $config
    $code = if ($VersionCode -gt 0) { $VersionCode } else { $published + 1 }
    if ($code -le $published) { throw "Requested code $code must exceed public version $published" }
    Set-AndroidSdk $config
    $store = Expand-Setting $config.keystoreFile
    if (-not (Test-Path -LiteralPath $store -PathType Leaf)) { throw "Keystore not found: $store" }
    $env:FP_ANDROID_KEYSTORE_FILE = $store
    $env:FP_ANDROID_KEY_ALIAS = [string]$config.keyAlias
    $env:FP_ANDROID_GOOGLE_WEB_CLIENT_ID = [string]$config.googleWebClientId
    try {
        $secureStore = Read-Host 'Keystore password' -AsSecureString
        $env:FP_ANDROID_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $secureStore).Password
        $secureKey = Read-Host 'Key password (usually same)' -AsSecureString
        $env:FP_ANDROID_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $secureKey).Password
        $name = "$($config.versionPrefix).$code"
        Write-Host "Building $name (code $code)"
        & (Join-Path $PSScriptRoot 'build-release-apk.ps1') -VersionCode $code -VersionName $name -GoogleWebClientId $config.googleWebClientId
        if (-not $?) { throw 'APK build failed.' }
    }
    finally {
        Remove-Item Env:\FP_ANDROID_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:\FP_ANDROID_KEY_PASSWORD -ErrorAction SilentlyContinue
    }
    Test-ApkCertificate $config
    Write-Host "Signed release READY, not published: $name"
}
function Install-Release($config) {
    Test-ApkCertificate $config
    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    $apk = Join-Path $root 'android-release-output\familypoints-beta.apk'
    & $adb -s $config.deviceSerial install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw 'Install failed. Do not uninstall the previous version automatically: offline data may be lost.'
    }
    Write-Host 'Install complete. Test app launch and login on the phone before publishing.'
}
function Publish-Release($config) {
    Test-ApkCertificate $config
    $local = Read-LocalManifest
    $published = Get-PublishedVersion $config
    if ([int]$local.versionCode -le $published) { throw 'APK is not newer than public version; publication blocked.' }
    $answer = Read-Host "Publish $($local.versionName) to public BETA? Type PUBLISH"
    if ($answer -cne 'PUBLISH') { throw 'Cancelled.' }
    & (Join-Path $PSScriptRoot 'publish-to-contabo.ps1') -HostName $config.contaboHost -UserName $config.contaboUser -SshKey (Expand-Setting $config.sshKey)
    if (-not $?) { throw 'Publisher error. Inspect public manifest before retrying; APK may already be published.' }
}
$config = Read-Config
if ($Action -eq 'menu') {
    Write-Host ''
    Write-Host 'FamilyPoints Android BETA'
    Write-Host '  1 Status   2 Build   3 Install   4 Google logs   5 Publish   6 All'
    $selection = Read-Host 'Select 1-6'
    $available = @('status','build','install','logs','publish','all')
    if ($selection -notmatch '^[1-6]$') { throw 'Invalid selection.' }
    $Action = $available[([int]$selection - 1)]
}
switch ($Action) {
    'setup' { Write-Host "Private config ready: $configPath" }
    'status' {
        $published = Get-PublishedVersion $config
        Write-Host "Public version code: $published"
        Write-Host "SDK found: $(Test-Path (Join-Path (Expand-Setting $config.androidSdk) 'platform-tools\adb.exe'))"
        Write-Host "Keystore found: $(Test-Path (Expand-Setting $config.keystoreFile))"
        $localPath = Join-Path $root 'android-release-output\familypoints-beta.json'
        if (Test-Path $localPath) {
            $local = Read-LocalManifest
            Write-Host "Local APK: $($local.versionName) (code $($local.versionCode))"
        }
    }
    'build' { Build-Release $config }
    'install' { Install-Release $config }
    'logs' {
        Set-AndroidSdk $config
        $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
        & $adb -s $config.deviceSerial logcat -d -s 'FamilyPointsGoogleAuth:I' '*:S'
        if ($LASTEXITCODE -ne 0) { throw 'ADB logcat failed.' }
    }
    'publish' { Publish-Release $config }
    'all' { Build-Release $config; Install-Release $config; Publish-Release $config }
}
