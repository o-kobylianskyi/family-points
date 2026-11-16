param(
  [ValidateSet('status','ci','prepare','qa','post-deploy')][string]$Action = 'status',
  [string]$Tag = '',
  [string]$BaseUrl = '',
  [switch]$AllowPublicSmoke
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
function Http200([string]$url) {
  $status = ( @(Run 'curl.exe' @('--silent','--show-error','--max-redirs','0','--max-time','20','--output','NUL','--write-out','%{http_code}','--',$url)) -join '' ).Trim()
  if ($status -ne '200') { throw "Unexpected HTTP $status at $url" }
  Write-Host "HTTP 200 $url"
}
function Test-QaBase([string]$url) {
  if (-not $url) { throw 'QA check requires explicit -BaseUrl https://<isolated-test-host>; no production default.' }
  $uri = $null
  if (-not [Uri]::TryCreate($url,[UriKind]::Absolute,[ref]$uri) -or
      $uri.Scheme -notin @('http','https') -or
      -not $uri.Host -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
    throw 'Invalid QA URL. Use an explicit http(s) origin without credentials, query or fragment.'
  }
  $hostName = $uri.Host.ToLowerInvariant()
  $blockedHosts = @('family-point.com','www.family-point.com','api.family-point.com','184.174.37.206')
  if (($hostName -in $blockedHosts) -or $hostName.EndsWith('.family-point.com')) {
    throw 'Production/BETA host is forbidden for QA. Use an isolated QA endpoint.'
  }
  # The domain alone is not proof of isolation; operator must choose an authorized disposable QA instance.
  Write-Host "Explicit isolated QA target: $($uri.GetLeftPart([UriPartial]::Authority))"
  return $uri.GetLeftPart([UriPartial]::Authority).TrimEnd('/')
}
function Show-CiStatus([string]$sha) {
  Write-Host "CI SHA: $sha"
  Write-Host "GitHub Actions: https://github.com/$repo/actions/workflows/ci.yml"
  Write-Host 'CI UNVERIFIED: read the actual jobs for this SHA in GitHub Actions.'
  Write-Host 'Required: Backend Maven, Android Gradle, Web build and Compose validation all executed successfully.'
  Write-Host 'No GitHub CLI or GitHub authentication is needed for this local command.'
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
if ($Action -eq 'ci') {
  Show-CiStatus $sha
  exit 0
}
if ($Action -eq 'prepare') {
  if ($Tag -notmatch '^v[0-9]+[.][0-9]+[.][0-9]+-beta[.][0-9]+$') { throw 'Use -Tag vX.Y.Z-beta.N' }
  Run 'git' @('fetch','--no-tags','origin',("refs/tags/{0}:refs/tags/{0}" -f $Tag)) | Out-Null
  $tagType = (@(Run 'git' @('cat-file','-t',("refs/tags/{0}" -f $Tag))) -join '').Trim()
  if ($tagType -ne 'tag') { throw 'Tag must be annotated.' }
  $tagSha = (@(Run 'git' @('rev-parse',("refs/tags/{0}^{{commit}}" -f $Tag))) -join '').Trim()
  if ($sha -ne $tagSha) { throw "Tag target $tagSha differs from local $sha" }
}
if ($Action -eq 'status') {
  Write-Host 'LOCAL STATUS ONLY: no network request to PROD/BETA.'
  Write-Host 'Use -Action qa -BaseUrl https://<isolated-QA-host> for explicitly authorized QA checks.'
}
if ($Action -eq 'qa') {
  $qaOrigin = Test-QaBase $BaseUrl
  Http200 ($qaOrigin + '/')
  Http200 ($qaOrigin + '/api/client-health')
  Write-Host 'QA HTTP SMOKE PASS. This is not proof of functional or restore acceptance.'
}
if ($Action -eq 'post-deploy') {
  if (-not $AllowPublicSmoke) { throw 'Post-deploy production/BETA smoke is opt-in: specify -AllowPublicSmoke after an approved deployment.' }
  if ($BaseUrl) { throw 'post-deploy uses pinned public host; -BaseUrl is not accepted.' }
  Http200 'https://family-point.com/'
  Http200 'https://family-point.com/api/client-health'
  Http200 'https://family-point.com/downloads/familypoints-beta.apk'
  Write-Host 'PUBLIC POST-DEPLOY HTTP SMOKE PASS (read-only).'
}
if ($Action -eq 'prepare') {
  Show-CiStatus $sha
  $dir = Join-Path (Get-Location).Path 'release-evidence'
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  $record = [ordered]@{
    recordedUtc = (Get-Date).ToUniversalTime().ToString('o')
    sourceSha = $sha
    tag = $Tag
    tagSha = $tagSha
    fullCiRun = $null
    fullCiVerified = $false
    publicSmokePerformed = $false
    qaSmokePerformed = $false
    qaIntegrationAccepted = $false
    deploymentExecuted = $false
    backendWebRuntimeShaVerified = $false
    flywayVerified = $false
    bkl002RestorationAccepted = $false
  }
  $file = Join-Path $dir ('release-gate-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.json')
  $record | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $file -Encoding UTF8
  Write-Host "Gate record: $file"
  Write-Host 'DRAFT ONLY: CI NOT VERIFIED. No release approval, PROD/BETA request, deployment, migration, DB operation, tag creation, or APK publication.'
}
