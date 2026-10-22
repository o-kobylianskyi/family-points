[CmdletBinding()]
param([string]$ZipPath = '')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $ZipPath) {
  $zip = Get-ChildItem (Join-Path $env:USERPROFILE 'Downloads') -Filter 'family-points-dashboard-reference-assets*.zip' -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
  if (-not $zip) { throw 'Download family-points-dashboard-reference-assets.zip from the chat and retry, or pass -ZipPath.' }
  $ZipPath = $zip.FullName
}
if (-not (Test-Path -LiteralPath $ZipPath -PathType Leaf)) { throw "Missing ZIP: $ZipPath" }
$expected = @('brand-symbol.webp','hero-family.webp','sidebar-family.webp','icon-coins.webp','icon-star.webp','icon-checklist.webp','icon-gift.webp','quick-plus.webp','quick-family.webp','quick-gift.webp','quick-points.webp')
Add-Type -AssemblyName System.IO.Compression
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ZipPath).Path)
try {
  $targetDir = Join-Path $root 'frontend\public\assets\soft-family\reference'
  New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
  foreach ($name in $expected) {
    $entry = $archive.GetEntry("frontend/public/assets/soft-family/reference/$name")
    if (-not $entry) { throw "Archive does not contain expected image $name" }
    $dst = Join-Path $targetDir $name
    $inputStream = $entry.Open()
    $outputStream = [IO.File]::Create($dst)
    try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose(); $inputStream.Dispose() }
    Write-Host "Installed $name ($($entry.Length) bytes)"
  }
} finally { $archive.Dispose() }
& git -C $root add -- 'frontend/public/assets/soft-family/reference'
if ($LASTEXITCODE -ne 0) { throw 'git add failed' }
Write-Host '11 reference images installed and staged; commit and push them with your other work.' -ForegroundColor Green
