param(
  [ValidateSet('status','ci','prepare')][string]$Action = 'status',
  [string]$Tag = '',
  [switch]$DispatchFullCi
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$repo = 'o-kobylianskyi/family-points'
function Run([string]$exe,[string[]]$commandArgs) {
  # $args is an automatic PowerShell variable; do not use it as a parameter.
  $out = @(& $exe @commandArgs)
  $exitCode = $LASTEXITCODE
  if ($exitCode -ne 0) {
    throw ("External command failed: {0} {1}; exit code {2}" -f $exe,($commandArgs -join ' '),$exitCode)
  }
  return $out
}
function Read-Public {
  $url = 'https://family-point.com/downloads/familypoints-beta.json'
  $result = @(Run 'curl.exe' @('--fail','--silent','--show-error','--location','--max-time','20','--',$url))
  $raw = ($result -join [Environment]::NewLine).Trim().TrimStart([char]0xFEFF)
  if (-not $raw.StartsWith('{')) { throw 'Public manifest is not JSON.' }
  $json = ConvertFrom-Json -InputObject $raw
  if ([int]$json.versionCode -le 0 -or [string]$json.sha256 -notmatch '^[a-fA-F0-9]{64}$') {
    throw 'Public manifest has invalid version or digest.'
  }
  return $json
}
function Http200([string]$url) {
  $status = ( @(Run 'curl.exe' @('--silent','--show-error','--location','--max-time','20','--output','NUL','--write-out','%{http_code}','--',$url)) -join '' ).Trim()
  if ($status -ne '200') { throw "Unexpected HTTP $status at $url" }
  Write-Host "HTTP 200 $url"
}
function ExactShaFullCi([string]$sha) {
  if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'GitHub CLI gh required for exact-SHA CI gate. Install/authenticate gh first.' }
  $response = @(Run 'gh' @('api',("repos/{0}/actions/workflows/ci.yml/runs?head_sha={1}&per_page=50" -f $repo,$sha))) -join [Environment]::NewLine
  $runs = @((ConvertFrom-Json -InputObject $response).workflow_runs | Where-Object {
    $_.head_sha -eq $sha -and $_.event -eq 'workflow_dispatch' -and $_.status -eq 'completed' -and $_.conclusion -eq 'success'
  } | Sort-Object run_number -Descending)
  if ($runs.Count -eq 0) { throw "No successful complete manually dispatched CI for exact SHA $sha" }
  foreach ($run in $runs) {
    $jobsResponse = @(Run 'gh' @('api',("repos/{0}/actions/runs/{1}/jobs?per_page=100" -f $repo,$run.id))) -join [Environment]::NewLine
    $jobs = @((ConvertFrom-Json -InputObject $jobsResponse).jobs)
    $required = @{
      'Java 21 / Maven / PostgreSQL Testcontainers' = 'Run all backend tests'
      'Android / Gradle debug build' = 'Compile Android and run unit tests'
      'Web / Node / Vite production build' = 'Compile frontend'
      'BETA / Docker Compose configuration' = 'Verify compose and secrets placeholders'
    }
    $valid = $true
    foreach ($name in $required.Keys) {
      $matches = @($jobs | Where-Object { $_.name -eq $name -and $_.conclusion -eq 'success' })
      if ($matches.Count -ne 1) { $valid = $false; break }
      $step = @($matches[0].steps | Where-Object { $_.name -eq $required[$name] -and $_.conclusion -eq 'success' })
      if ($step.Count -ne 1) { $valid = $false; break }
    }
    if ($valid) { Write-Host "Exact-SHA FULL CI PASS: $($run.html_url)"; return $run.html_url }
  }
  throw "No CI run has all FOUR required executed PASS jobs for $sha. Skips do not qualify."
}
$sha = (@(Run 'git' @('rev-parse','HEAD')) -join '').Trim()
# CI dispatch and read-only CI verification operate on the immutable remote
# commit SHA, not on the local worktree contents. Local QA reports and untracked
# files must not block those safe operations. Only release preparation, which
# records local release evidence, requires a completely clean worktree.
if ($Action -eq 'prepare') {
  $dirty = @(Run 'git' @('status','--porcelain'))
  if ($dirty.Count -gt 0) { throw 'Dirty working tree. Preserve changes, never reset/clean automatically.' }
}
Write-Host "Local SHA: $sha"
if ($Action -eq 'ci' -and $DispatchFullCi) {
  if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'GitHub CLI gh is required.' }
  $origin = (@(Run 'git' @('ls-remote','origin','refs/heads/main')) -join '').Split([char]9)[0]
  if ($origin -ne $sha) { throw "Local $sha differs from origin/main $origin; refusing a mismatched CI dispatch." }
  Run 'gh' @('workflow','run','ci.yml','--repo',$repo,'--ref','main') | Out-Null
  Write-Host "Dispatched full CI. Results are pending. Run -Action ci later."
  exit 0
}
if ($Action -eq 'ci' -or $Action -eq 'prepare') { $ciUrl = ExactShaFullCi $sha }
if ($Action -eq 'prepare') {
  if ($Tag -notmatch '^v[0-9]+[.][0-9]+[.][0-9]+-beta[.][0-9]+$') { throw 'Use -Tag vX.Y.Z-beta.N' }
  Run 'git' @('fetch','--no-tags','origin',("refs/tags/{0}:refs/tags/{0}" -f $Tag)) | Out-Null
  $tagType = (@(Run 'git' @('cat-file','-t',("refs/tags/{0}" -f $Tag))) -join '').Trim()
  if ($tagType -ne 'tag') { throw 'Tag must be annotated.' }
  $tagSha = (@(Run 'git' @('rev-parse',("refs/tags/{0}^{{commit}}" -f $Tag))) -join '').Trim()
  if ($sha -ne $tagSha) { throw "Tag target $tagSha differs from local $sha" }
}
Http200 'https://family-point.com/'
Http200 'https://family-point.com/api/client-health'
Http200 'https://family-point.com/downloads/familypoints-beta.apk'
$manifest = Read-Public
Write-Host "Public Android: $($manifest.versionName), code $($manifest.versionCode), SHA-256 $($manifest.sha256)"
if ($Action -eq 'prepare') {
  $dir = Join-Path (Get-Location).Path 'release-evidence'
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  $record = [ordered]@{
    recordedUtc = (Get-Date).ToUniversalTime().ToString('o')
    sourceSha = $sha
    tag = $Tag
    tagSha = $tagSha
    fullCiRun = $ciUrl
    publicAndroidVersionCode = [int]$manifest.versionCode
    publicAndroidVersionName = [string]$manifest.versionName
    publicAndroidSha256 = [string]$manifest.sha256
    deploymentExecuted = $false
    backendWebRuntimeShaVerified = $false
    flywayVerified = $false
    bkl002RestorationAccepted = $false
  }
  $file = Join-Path $dir ('release-gate-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.json')
  $record | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $file -Encoding UTF8
  Write-Host "Gate record: $file"
  Write-Host 'PREPARED ONLY. No deployment, migration, DB operation, tag creation, or APK publication.'
}
