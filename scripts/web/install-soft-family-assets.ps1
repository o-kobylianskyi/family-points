# Installs approved FamilyPoints WebP artwork from the original project ZIP.
# Run: .\scripts\web\install-soft-family-assets.ps1 [-ZipPath 'C:\...\assets.zip']
# Never overwrites unrelated files or modifies Android, backend, or deployment config.
[CmdletBinding()]
param(
    [string] $ZipPath,
    [switch] $StageForGit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$destination = Join-Path $projectRoot 'frontend\public\assets\soft-family'

if ([string]::IsNullOrWhiteSpace($ZipPath)) {
    $folders = @(
        (Join-Path $env:USERPROFILE 'Downloads'),
        (Join-Path $env:USERPROFILE 'Desktop'),
        (Join-Path $env:USERPROFILE 'Documents'),
        (Join-Path $env:USERPROFILE 'OneDrive\Downloads'),
        (Join-Path $env:USERPROFILE 'OneDrive\Desktop')
    ) | Select-Object -Unique
    $zip = $folders | ForEach-Object {
        if (Test-Path -LiteralPath $_ -PathType Container) {
            Get-ChildItem -LiteralPath $_ -File -Filter 'familypoints_soft_family_web_assets*.zip' -ErrorAction SilentlyContinue
        }
    } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($null -eq $zip) {
        throw 'Archive is not on this PC in Downloads/Desktop/Documents. Download the ZIP from the ChatGPT reply, then retry or pass -ZipPath with the exact file path.'
    }
    $ZipPath = $zip.FullName
}
if (-not (Test-Path -LiteralPath $ZipPath -PathType Leaf)) {
    throw "ZIP archive not found: $ZipPath"
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$expected = [ordered]@{
    'family-hero.webp'  = '70660570d8605a6951970e4621c7899eb9d35c108221c292aedbb795b220894b'
    'family-promo.webp' = '4f184a0e5dc8a2f12e211b69a01bb1823f2a6862046e7a4baba9f1d57736adee'
}
$results = @()
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ZipPath).Path)
try {
    foreach ($filename in $expected.Keys) {
        $nameInsideArchive = "frontend/public/assets/soft-family/$filename"
        $entry = $archive.GetEntry($nameInsideArchive)
        if ($null -eq $entry) { throw "ZIP is missing required artwork: $nameInsideArchive" }
        $source = $entry.Open()
        $buffer = New-Object System.IO.MemoryStream
        try {
            $source.CopyTo($buffer)
            $bytes = $buffer.ToArray()
        } finally {
            $source.Dispose()
            $buffer.Dispose()
        }
        $digest = [System.Security.Cryptography.SHA256]::Create()
        try {
            $actualHash = ([BitConverter]::ToString($digest.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
        } finally { $digest.Dispose() }
        if ($actualHash -ne $expected[$filename]) {
            throw "Unexpected artwork hash for $filename: $actualHash. Use the approved ZIP from the FamilyPoints design handoff."
        }
        $results += [PSCustomObject]@{ Name = $filename; Bytes = $bytes }
    }
} finally { $archive.Dispose() }

New-Item -ItemType Directory -Path $destination -Force | Out-Null
foreach ($result in $results) {
    $file = Join-Path $destination $result.Name
    [System.IO.File]::WriteAllBytes($file, $result.Bytes)
    Write-Host "Installed: $file ($($result.Bytes.Length) bytes)" -ForegroundColor Green
}

if ($StageForGit) {
    & git -C $projectRoot add -- 'frontend/public/assets/soft-family/family-hero.webp' 'frontend/public/assets/soft-family/family-promo.webp'
    if ($LASTEXITCODE -ne 0) { throw 'git add failed; inspect git status before committing.' }
    Write-Host 'WebP files staged for git. Review git diff --cached --stat and commit in the Web branch.' -ForegroundColor Yellow
}
Write-Host 'Soft Family artwork installed and SHA-256 verified.' -ForegroundColor Green
