param(
    [ValidateSet('menu','setup','status','build','install','logs','publish','all')][string]$Action = 'menu',
    [int]$VersionCode = 0
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$configPath = Join-Path $PSScriptRoot 'release-config.local.json'
$templatePath = Join-Path $PSScriptRoot 'release-config.example.json'
function Expand-ConfigPath([string]$value) { return [Environment]::ExpandEnvironmentVariables($value) }
function Read-Config {
    if (-not (Test-Path -LiteralPath $configPath)) {
        Copy-Item -LiteralPath $templatePath -Destination $configPath
        Write-Host "Created PRIVATE config: $configPath"
        $clientId = Read-Host 'Enter Google OAuth WEB Client ID (once; not Android Client ID)'
        if ($clientId -notmatch '^[0-9]+-[A-Za-z0-9_-]+\.apps\.googleusercontent\.com
    }
    $config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($config.googleWebClientId -notmatch '^[0-9]+-[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$') {
        throw "Edit googleWebClientId in $configPath (WEB client, not Android OAuth client)."
    }
    return $config
}
function Get-PublicVersion($config) {
    $response = Invoke-WebRequest -Uri $config.manifestUrl -UseBasicParsing -Headers @{'Accept'='application/json';'Cache-Control'='no-cache'} -TimeoutSec 20
    $public = $response.Content | ConvertFrom-Json
    if ($null -eq $public.PSObject.Properties['versionCode'] -or [int]$public.versionCode -lt 1) { throw 'Published manifest has no valid versionCode.' }
    return [int]$public.versionCode
}
function Setup-Environment($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $store = Expand-ConfigPath $config.keystoreFile
    if (-not (Test-Path (Join-Path $sdk 'platform-tools/adb.exe'))) { throw "Android SDK unavailable: $sdk" }
    if (-not (Test-Path -LiteralPath $store -PathType Leaf)) { throw "Release keystore missing: $store" }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    $env:FP_ANDROID_KEYSTORE_FILE = $store
    $env:FP_ANDROID_KEY_ALIAS = $config.keyAlias
    $env:FP_ANDROID_GOOGLE_WEB_CLIENT_ID = $config.googleWebClientId
    $hash = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    $keytool = 'keytool.exe'
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) { $keytool = "$env:JAVA_HOME\bin\keytool.exe" }
    if (-not $hash -or $hash.Length -ne 64) { throw 'expectedCertificateSha256 must contain 64 hex characters.' }
    # Signing password is never saved to disk. For local build, prompt through SecureString.
    $password = Read-Host 'Release keystore password' -AsSecureString
    $env:FP_ANDROID_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
    $password = Read-Host 'Release key password (repeat if same)' -AsSecureString
    $env:FP_ANDROID_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
}
function Assert-Certificate($config, [string]$apk) {
    $verifier = Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Directory |
        Sort-Object { try {[version]($_.Name -replace '-.*$','')} catch {[version]'0.0'} } -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $verifier) { throw 'apksigner.bat not found' }
    $output = & $verifier verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature invalid' }
    $match = [regex]::Match(($output -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)')
    if (-not $match.Success) { throw 'APK signing certificate fingerprint unavailable' }
    $expected = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    if ($match.Groups[1].Value.ToLowerInvariant() -ne $expected) { throw 'APK signed with UNEXPECTED key; refusing to proceed.' }
    Write-Host 'Release certificate fingerprint VERIFIED.'
}
function Build-Apk($config) {
    $published = Get-PublicVersion $config
    $code = if ($VersionCode -gt 0) { $VersionCode } else { $published + 1 }
    if ($code -le $published) { throw "New versionCode $code must exceed published $published" }
    $name = "$($config.versionPrefix).$code"
    Write-Host "Build $name (versionCode $code). Published versionCode: $published"
    Setup-Environment $config
    & (Join-Path $PSScriptRoot 'build-release-apk.ps1') -VersionCode $code -VersionName $name -GoogleWebClientId $config.googleWebClientId
    if (-not $?) { throw 'Build command failed.' }
    Assert-Certificate $config (Join-Path $root 'android-release-output/familypoints-beta.apk')
    Write-Host "BETA APK READY: $name"
}
function Read-LocalManifest {
    $path = Join-Path $root 'android-release-output/familypoints-beta.json'
    if (-not (Test-Path $path)) { throw 'Build APK first; manifest missing.' }
    return (Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json)
}
function Install-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    if (-not (Test-Path $apk)) { throw 'Build APK first.' }
    Assert-Certificate $config $apk
    $adb = Join-Path $sdk 'platform-tools/adb.exe'
    Write-Host "Installing on device serial: $($config.deviceSerial)"
    & $adb -s $config.deviceSerial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Device install FAILED. Check signing mismatch, do NOT auto-uninstall and erase offline data.' }
    Write-Host 'Installed. Manually verify login and main screens.'
}
function Publish-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    Assert-Certificate $config $apk
    $local = Read-LocalManifest
    $published = Get-PublicVersion $config
    if ([int]$local.versionCode -le $published) { throw "Local APK code $($local.versionCode) is not newer than published $published" }
    $answer = Read-Host "Publish $($local.versionName) to LIVE BETA? Type PUBLISH"
    if ($answer -cne 'PUBLISH') { throw 'Cancelled; no publication.' }
    & (Join-Path $PSScriptRoot 'publish-to-contabo.ps1') -HostName $config.contaboHost -UserName $config.contaboUser -SshKey (Expand-ConfigPath $config.sshKey)
    if (-not $?) { throw 'Publisher failed; inspect public manifest before retrying.' }
}
$config = Read-Config
if ($Action -eq 'menu') {
    Write-Host ''
    Write-Host 'FamilyPoints Android BETA release'
    Write-Host '  1 - Status (published version and local APK)'
    Write-Host '  2 - Build next signed APK'
    Write-Host '  3 - Install APK on configured phone'
    Write-Host '  4 - Google Sign-In diagnostics'
    Write-Host '  5 - Publish tested APK to Contabo'
    Write-Host '  6 - Build + install + publish (with confirmations)'
    $selection = Read-Host 'Select 1-6'
    $actions = @('status','build','install','logs','publish','all')
    if ($selection -notmatch '^[1-6]
    setup { Write-Host "Config is ready: $configPath"; Write-Host 'No passwords are stored in configuration.' }
    status {
        $remote = Get-PublicVersion $config
        Write-Host "Public beta versionCode: $remote"
        Write-Host "Android SDK: $(Expand-ConfigPath $config.androidSdk)"
        Write-Host "Keystore exists: $(Test-Path (Expand-ConfigPath $config.keystoreFile))"
        Write-Host "SSH key exists: $(Test-Path (Expand-ConfigPath $config.sshKey))"
        $localPath = Join-Path $root 'android-release-output/familypoints-beta.json'
        if (Test-Path $localPath) { $local = Read-LocalManifest; Write-Host "Local APK: $($local.versionName) (code $($local.versionCode))" }
    }
    build { Build-Apk $config }
    install { Install-Apk $config }
    logs {
        $adb = Join-Path (Expand-ConfigPath $config.androidSdk) 'platform-tools/adb.exe'
        & $adb -s $config.deviceSerial logcat -d -s FamilyPointsGoogleAuth:I '*:S'
    }
    publish { Publish-Apk $config }
    all { Build-Apk $config; Install-Apk $config; Publish-Apk $config }
}
) {
            throw "Invalid Web Client ID; edit $configPath manually and rerun."
        }
        $newConfig = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $newConfig.googleWebClientId = $clientId.Trim()
        $newConfig | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $configPath -Encoding UTF8
        Write-Host 'Private release config saved. No passwords were stored.'
    }
    $config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($config.googleWebClientId -notmatch '^[0-9]+-[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$') {
        throw "Edit googleWebClientId in $configPath (WEB client, not Android OAuth client)."
    }
    return $config
}
function Get-PublicVersion($config) {
    $response = Invoke-WebRequest -Uri $config.manifestUrl -UseBasicParsing -Headers @{'Accept'='application/json';'Cache-Control'='no-cache'} -TimeoutSec 20
    $public = $response.Content | ConvertFrom-Json
    if ($null -eq $public.PSObject.Properties['versionCode'] -or [int]$public.versionCode -lt 1) { throw 'Published manifest has no valid versionCode.' }
    return [int]$public.versionCode
}
function Setup-Environment($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $store = Expand-ConfigPath $config.keystoreFile
    if (-not (Test-Path (Join-Path $sdk 'platform-tools/adb.exe'))) { throw "Android SDK unavailable: $sdk" }
    if (-not (Test-Path -LiteralPath $store -PathType Leaf)) { throw "Release keystore missing: $store" }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    $env:FP_ANDROID_KEYSTORE_FILE = $store
    $env:FP_ANDROID_KEY_ALIAS = $config.keyAlias
    $env:FP_ANDROID_GOOGLE_WEB_CLIENT_ID = $config.googleWebClientId
    $hash = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    $keytool = 'keytool.exe'
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) { $keytool = "$env:JAVA_HOME\bin\keytool.exe" }
    if (-not $hash -or $hash.Length -ne 64) { throw 'expectedCertificateSha256 must contain 64 hex characters.' }
    # Signing password is never saved to disk. For local build, prompt through SecureString.
    $password = Read-Host 'Release keystore password' -AsSecureString
    $env:FP_ANDROID_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
    $password = Read-Host 'Release key password (repeat if same)' -AsSecureString
    $env:FP_ANDROID_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
}
function Assert-Certificate($config, [string]$apk) {
    $verifier = Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Directory |
        Sort-Object { try {[version]($_.Name -replace '-.*$','')} catch {[version]'0.0'} } -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $verifier) { throw 'apksigner.bat not found' }
    $output = & $verifier verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature invalid' }
    $match = [regex]::Match(($output -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)')
    if (-not $match.Success) { throw 'APK signing certificate fingerprint unavailable' }
    $expected = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    if ($match.Groups[1].Value.ToLowerInvariant() -ne $expected) { throw 'APK signed with UNEXPECTED key; refusing to proceed.' }
    Write-Host 'Release certificate fingerprint VERIFIED.'
}
function Build-Apk($config) {
    $published = Get-PublicVersion $config
    $code = if ($VersionCode -gt 0) { $VersionCode } else { $published + 1 }
    if ($code -le $published) { throw "New versionCode $code must exceed published $published" }
    $name = "$($config.versionPrefix).$code"
    Write-Host "Build $name (versionCode $code). Published versionCode: $published"
    Setup-Environment $config
    & (Join-Path $PSScriptRoot 'build-release-apk.ps1') -VersionCode $code -VersionName $name -GoogleWebClientId $config.googleWebClientId
    if (-not $?) { throw 'Build command failed.' }
    Assert-Certificate $config (Join-Path $root 'android-release-output/familypoints-beta.apk')
    Write-Host "BETA APK READY: $name"
}
function Read-LocalManifest {
    $path = Join-Path $root 'android-release-output/familypoints-beta.json'
    if (-not (Test-Path $path)) { throw 'Build APK first; manifest missing.' }
    return (Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json)
}
function Install-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    if (-not (Test-Path $apk)) { throw 'Build APK first.' }
    Assert-Certificate $config $apk
    $adb = Join-Path $sdk 'platform-tools/adb.exe'
    Write-Host "Installing on device serial: $($config.deviceSerial)"
    & $adb -s $config.deviceSerial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Device install FAILED. Check signing mismatch, do NOT auto-uninstall and erase offline data.' }
    Write-Host 'Installed. Manually verify login and main screens.'
}
function Publish-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    Assert-Certificate $config $apk
    $local = Read-LocalManifest
    $published = Get-PublicVersion $config
    if ([int]$local.versionCode -le $published) { throw "Local APK code $($local.versionCode) is not newer than published $published" }
    $answer = Read-Host "Publish $($local.versionName) to LIVE BETA? Type PUBLISH"
    if ($answer -cne 'PUBLISH') { throw 'Cancelled; no publication.' }
    & (Join-Path $PSScriptRoot 'publish-to-contabo.ps1') -HostName $config.contaboHost -UserName $config.contaboUser -SshKey (Expand-ConfigPath $config.sshKey)
    if (-not $?) { throw 'Publisher failed; inspect public manifest before retrying.' }
}
$config = Read-Config
switch ($Action) {
    setup { Write-Host "Config is ready: $configPath"; Write-Host 'No passwords are stored in configuration.' }
    status {
        $remote = Get-PublicVersion $config
        Write-Host "Public beta versionCode: $remote"
        Write-Host "Android SDK: $(Expand-ConfigPath $config.androidSdk)"
        Write-Host "Keystore exists: $(Test-Path (Expand-ConfigPath $config.keystoreFile))"
        Write-Host "SSH key exists: $(Test-Path (Expand-ConfigPath $config.sshKey))"
        $localPath = Join-Path $root 'android-release-output/familypoints-beta.json'
        if (Test-Path $localPath) { $local = Read-LocalManifest; Write-Host "Local APK: $($local.versionName) (code $($local.versionCode))" }
    }
    build { Build-Apk $config }
    install { Install-Apk $config }
    logs {
        $adb = Join-Path (Expand-ConfigPath $config.androidSdk) 'platform-tools/adb.exe'
        & $adb -s $config.deviceSerial logcat -d -s FamilyPointsGoogleAuth:I '*:S'
    }
    publish { Publish-Apk $config }
    all { Build-Apk $config; Install-Apk $config; Publish-Apk $config }
}
) { throw 'Invalid selection' }
    $Action = $actions[[int]$selection - 1]
}
switch ($Action) {
    setup { Write-Host "Config is ready: $configPath"; Write-Host 'No passwords are stored in configuration.' }
    status {
        $remote = Get-PublicVersion $config
        Write-Host "Public beta versionCode: $remote"
        Write-Host "Android SDK: $(Expand-ConfigPath $config.androidSdk)"
        Write-Host "Keystore exists: $(Test-Path (Expand-ConfigPath $config.keystoreFile))"
        Write-Host "SSH key exists: $(Test-Path (Expand-ConfigPath $config.sshKey))"
        $localPath = Join-Path $root 'android-release-output/familypoints-beta.json'
        if (Test-Path $localPath) { $local = Read-LocalManifest; Write-Host "Local APK: $($local.versionName) (code $($local.versionCode))" }
    }
    build { Build-Apk $config }
    install { Install-Apk $config }
    logs {
        $adb = Join-Path (Expand-ConfigPath $config.androidSdk) 'platform-tools/adb.exe'
        & $adb -s $config.deviceSerial logcat -d -s FamilyPointsGoogleAuth:I '*:S'
    }
    publish { Publish-Apk $config }
    all { Build-Apk $config; Install-Apk $config; Publish-Apk $config }
}
) {
            throw "Invalid Web Client ID; edit $configPath manually and rerun."
        }
        $newConfig = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $newConfig.googleWebClientId = $clientId.Trim()
        $newConfig | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $configPath -Encoding UTF8
        Write-Host 'Private release config saved. No passwords were stored.'
    }
    $config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($config.googleWebClientId -notmatch '^[0-9]+-[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$') {
        throw "Edit googleWebClientId in $configPath (WEB client, not Android OAuth client)."
    }
    return $config
}
function Get-PublicVersion($config) {
    $response = Invoke-WebRequest -Uri $config.manifestUrl -UseBasicParsing -Headers @{'Accept'='application/json';'Cache-Control'='no-cache'} -TimeoutSec 20
    $public = $response.Content | ConvertFrom-Json
    if ($null -eq $public.PSObject.Properties['versionCode'] -or [int]$public.versionCode -lt 1) { throw 'Published manifest has no valid versionCode.' }
    return [int]$public.versionCode
}
function Setup-Environment($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $store = Expand-ConfigPath $config.keystoreFile
    if (-not (Test-Path (Join-Path $sdk 'platform-tools/adb.exe'))) { throw "Android SDK unavailable: $sdk" }
    if (-not (Test-Path -LiteralPath $store -PathType Leaf)) { throw "Release keystore missing: $store" }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    $env:FP_ANDROID_KEYSTORE_FILE = $store
    $env:FP_ANDROID_KEY_ALIAS = $config.keyAlias
    $env:FP_ANDROID_GOOGLE_WEB_CLIENT_ID = $config.googleWebClientId
    $hash = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    $keytool = 'keytool.exe'
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) { $keytool = "$env:JAVA_HOME\bin\keytool.exe" }
    if (-not $hash -or $hash.Length -ne 64) { throw 'expectedCertificateSha256 must contain 64 hex characters.' }
    # Signing password is never saved to disk. For local build, prompt through SecureString.
    $password = Read-Host 'Release keystore password' -AsSecureString
    $env:FP_ANDROID_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
    $password = Read-Host 'Release key password (repeat if same)' -AsSecureString
    $env:FP_ANDROID_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    Remove-Variable password
}
function Assert-Certificate($config, [string]$apk) {
    $verifier = Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Directory |
        Sort-Object { try {[version]($_.Name -replace '-.*$','')} catch {[version]'0.0'} } -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $verifier) { throw 'apksigner.bat not found' }
    $output = & $verifier verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature invalid' }
    $match = [regex]::Match(($output -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)')
    if (-not $match.Success) { throw 'APK signing certificate fingerprint unavailable' }
    $expected = ($config.expectedCertificateSha256 -replace '[^0-9a-fA-F]','').ToLowerInvariant()
    if ($match.Groups[1].Value.ToLowerInvariant() -ne $expected) { throw 'APK signed with UNEXPECTED key; refusing to proceed.' }
    Write-Host 'Release certificate fingerprint VERIFIED.'
}
function Build-Apk($config) {
    $published = Get-PublicVersion $config
    $code = if ($VersionCode -gt 0) { $VersionCode } else { $published + 1 }
    if ($code -le $published) { throw "New versionCode $code must exceed published $published" }
    $name = "$($config.versionPrefix).$code"
    Write-Host "Build $name (versionCode $code). Published versionCode: $published"
    Setup-Environment $config
    & (Join-Path $PSScriptRoot 'build-release-apk.ps1') -VersionCode $code -VersionName $name -GoogleWebClientId $config.googleWebClientId
    if (-not $?) { throw 'Build command failed.' }
    Assert-Certificate $config (Join-Path $root 'android-release-output/familypoints-beta.apk')
    Write-Host "BETA APK READY: $name"
}
function Read-LocalManifest {
    $path = Join-Path $root 'android-release-output/familypoints-beta.json'
    if (-not (Test-Path $path)) { throw 'Build APK first; manifest missing.' }
    return (Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json)
}
function Install-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    if (-not (Test-Path $apk)) { throw 'Build APK first.' }
    Assert-Certificate $config $apk
    $adb = Join-Path $sdk 'platform-tools/adb.exe'
    Write-Host "Installing on device serial: $($config.deviceSerial)"
    & $adb -s $config.deviceSerial install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Device install FAILED. Check signing mismatch, do NOT auto-uninstall and erase offline data.' }
    Write-Host 'Installed. Manually verify login and main screens.'
}
function Publish-Apk($config) {
    $sdk = Expand-ConfigPath $config.androidSdk
    $env:ANDROID_HOME = $sdk
    $apk = Join-Path $root 'android-release-output/familypoints-beta.apk'
    Assert-Certificate $config $apk
    $local = Read-LocalManifest
    $published = Get-PublicVersion $config
    if ([int]$local.versionCode -le $published) { throw "Local APK code $($local.versionCode) is not newer than published $published" }
    $answer = Read-Host "Publish $($local.versionName) to LIVE BETA? Type PUBLISH"
    if ($answer -cne 'PUBLISH') { throw 'Cancelled; no publication.' }
    & (Join-Path $PSScriptRoot 'publish-to-contabo.ps1') -HostName $config.contaboHost -UserName $config.contaboUser -SshKey (Expand-ConfigPath $config.sshKey)
    if (-not $?) { throw 'Publisher failed; inspect public manifest before retrying.' }
}
$config = Read-Config
switch ($Action) {
    setup { Write-Host "Config is ready: $configPath"; Write-Host 'No passwords are stored in configuration.' }
    status {
        $remote = Get-PublicVersion $config
        Write-Host "Public beta versionCode: $remote"
        Write-Host "Android SDK: $(Expand-ConfigPath $config.androidSdk)"
        Write-Host "Keystore exists: $(Test-Path (Expand-ConfigPath $config.keystoreFile))"
        Write-Host "SSH key exists: $(Test-Path (Expand-ConfigPath $config.sshKey))"
        $localPath = Join-Path $root 'android-release-output/familypoints-beta.json'
        if (Test-Path $localPath) { $local = Read-LocalManifest; Write-Host "Local APK: $($local.versionName) (code $($local.versionCode))" }
    }
    build { Build-Apk $config }
    install { Install-Apk $config }
    logs {
        $adb = Join-Path (Expand-ConfigPath $config.androidSdk) 'platform-tools/adb.exe'
        & $adb -s $config.deviceSerial logcat -d -s FamilyPointsGoogleAuth:I '*:S'
    }
    publish { Publish-Apk $config }
    all { Build-Apk $config; Install-Apk $config; Publish-Apk $config }
}
