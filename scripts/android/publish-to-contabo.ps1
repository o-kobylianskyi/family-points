param(
    [string]$HostName = "184.174.37.206",
    [string]$UserName = "adminfp",
    [string]$SshKey = (Join-Path $env:USERPROFILE ".ssh\contabo_admin_ed25519"),
    [string]$ReleaseDirectory = "",
    [switch]$SkipPublicVerification
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
if (-not $ReleaseDirectory) { $ReleaseDirectory = Join-Path $root "android-release-output" }
$apk = Join-Path $ReleaseDirectory "familypoints-beta.apk"
$jsonFile = Join-Path $ReleaseDirectory "familypoints-beta.json"
foreach ($item in @($apk, $jsonFile, $SshKey)) {
    if (-not (Test-Path -LiteralPath $item -PathType Leaf)) { throw "Missing file: $item" }
}
$metadata = Get-Content -LiteralPath $jsonFile -Raw -Encoding UTF8 | ConvertFrom-Json
if ($metadata.applicationId -ne "com.olehkobylianskyi.familypoints.android" -or
    $metadata.file -ne "familypoints-beta.apk" -or
    $metadata.downloadUrl -ne "https://family-point.com/downloads/familypoints-beta.apk" -or
    $metadata.versionCode -lt 1 -or
    $metadata.versionName -notmatch '^[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$') {
    throw "Release manifest is invalid"
}
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$size = (Get-Item -LiteralPath $apk).Length
if ($hash -ne $metadata.sha256 -or $size -ne $metadata.sizeBytes) {
    throw "APK does not match JSON (hash or size). Rebuild before publishing."
}
Write-Host "Local release VERIFIED: $($metadata.versionName) (code $($metadata.versionCode))"
Write-Host "SHA-256: $hash"
$answer = Read-Host "Upload this release to Contabo? Type UPLOAD"
if ($answer -cne "UPLOAD") { throw "Cancelled (no upload)" }

$dest = "$UserName@$HostName"
$tag = "fp-apk-$($metadata.versionCode)"
$remoteApk = "/tmp/$tag.apk"
$remoteJson = "/tmp/$tag.json"
$sshOptions = @('-i', $SshKey, '-o', 'ServerAliveInterval=30', '-o', 'ServerAliveCountMax=3')
& scp @sshOptions -- $apk "${dest}:${remoteApk}"
if ($LASTEXITCODE -ne 0) { throw "APK upload failed" }
& scp @sshOptions -- $jsonFile "${dest}:${remoteJson}"
if ($LASTEXITCODE -ne 0) { throw "JSON upload failed" }

# Deliberately interactive: publisher repeats validation and requires PUBLISH.
# Single known command, no arbitrary input interpolated from manifest into shell.
$remoteCommand = "cd /opt/familypoints-beta && bash scripts/android/publish-apk.sh $remoteApk $remoteJson"
Write-Host "Running publisher on Contabo. Enter PUBLISH when prompted."
& ssh @sshOptions -tt $dest $remoteCommand
if ($LASTEXITCODE -ne 0) { throw "Remote publisher failed or was cancelled" }

if (-not $SkipPublicVerification) {
    # Windows PowerShell 5.1 Invoke-WebRequest.Content is not reliably decoded here.
    # curl.exe was independently verified on the operator's Windows installation.
    $manifestUrl = "https://family-point.com/downloads/familypoints-beta.json"
    $jsonLines = @(& curl.exe --fail --silent --show-error --location --max-time 20 --header 'Accept: application/json' --header 'Cache-Control: no-cache' -- $manifestUrl)
    if ($LASTEXITCODE -ne 0) { throw "Could not fetch public release manifest. APK may ALREADY be published; inspect $manifestUrl" }
    $raw = ($jsonLines -join [Environment]::NewLine).Trim().TrimStart([char]0xFEFF)
    if (-not $raw.StartsWith('{')) { throw "Public manifest response is not a JSON object; inspect $manifestUrl (APK may ALREADY be published)" }
    try { $public = ConvertFrom-Json -InputObject $raw -ErrorAction Stop }
    catch { throw "Published manifest JSON parsing failed; inspect $manifestUrl (APK may ALREADY be published)" }
    if ($null -eq $public -or $null -eq $public.PSObject.Properties['versionCode'] -or
        $null -eq $public.PSObject.Properties['sha256']) {
        throw "Published manifest is missing versionCode or sha256; inspect $manifestUrl"
    }
    if ([int]$public.versionCode -ne [int]$metadata.versionCode -or
        [string]$public.sha256 -ne $hash) {
        throw "Published manifest does not match uploaded APK; check Contabo!"
    }
    Write-Host "Public manifest verified: $($public.versionName) / code $($public.versionCode)"
}
Write-Host "PUBLISH COMPLETE. Existing Backend, Web, Docker and DB were not touched."
