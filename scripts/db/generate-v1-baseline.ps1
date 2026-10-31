# Build initial Flyway V1 from v0.3-workspace, without accessing DEV database.
# Windows PowerShell + Java 21 + Docker Desktop + Git required.
# Generates V1 SQL from the historical pre-V2 Hibernate entities, and validates V1-V20.
param([switch]$KeepTemporaryFiles, [switch]$ValidateExisting)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$output = Join-Path $repo "src\main\resources\db\migration\V1__baseline_schema.sql"
if ($ValidateExisting) {
    if (-not (Test-Path -LiteralPath $output)) {
        throw "No V1 exists to validate: $output"
    }
} elseif (Test-Path -LiteralPath $output) {
    throw "V1 exists already. Use -ValidateExisting to check it without overwriting."
}
foreach ($exe in @("git", "docker", "java")) {
    if (-not (Get-Command $exe -ErrorAction SilentlyContinue)) { throw "Missing $exe" }
}
& docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Docker Desktop is not running" }
$id = [guid]::NewGuid().ToString("N").Substring(0, 12)
$temp = Join-Path $env:TEMP "fp-v1-$id"
$oldTree = Join-Path $temp "old"
$container = "fp-v1-$id"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$secret = [BitConverter]::ToString($bytes).Replace("-", "")
$rng.GetBytes($bytes)
$jwt = [BitConverter]::ToString($bytes).Replace("-", "")
$running = $false
$treeAdded = $false
$validated = $false
$createdV1 = $false
New-Item -ItemType Directory -Path $temp -Force | Out-Null

function Check-Exit([string]$what) {
    if ($LASTEXITCODE -ne 0) { throw "$what failed (exit $LASTEXITCODE)" }
}

function Build-Jar([string]$folder) {
    Push-Location $folder
    try {
        # PowerShell forwards all uncaptured native stdout as the function result.
        # If Maven output is not consumed here, Build-Jar returns the entire
        # build log alongside the JAR path, causing Start-Process -jar to fail
        # with Windows ERROR_FILENAME_EXCED_RANGE (filename too long).
        & .\mvnw.cmd "-Dmaven.test.skip=true" package | Out-Host
        Check-Exit "Maven package"
        $jar = Get-ChildItem target -Filter "*.jar" | Where-Object { $_.Name -notmatch 'original|sources|javadoc' } | Select-Object -First 1
        if ($null -eq $jar) { throw "No executable JAR in $folder" }
        # This must be the ONLY value emitted on the success stream.
        return [string]$jar.FullName
    } finally { Pop-Location }
}

function Boot-And-Stop([string]$jar, [string]$db, [bool]$generate, [int]$port) {
    $keys = @("DB_URL","DB_USERNAME","DB_PASSWORD","JWT_SECRET","SPRING_FLYWAY_ENABLED","SPRING_FLYWAY_BASELINE_ON_MIGRATE","SPRING_JPA_HIBERNATE_DDL_AUTO","SERVER_PORT")
    $before = @{}
    foreach ($k in $keys) { $before[$k] = [Environment]::GetEnvironmentVariable($k,"Process") }
    $out = Join-Path $temp "$db.stdout.log"
    $err = Join-Path $temp "$db.stderr.log"
    $proc = $null
    try {
        $env:DB_URL = "jdbc:postgresql://127.0.0.1:$port/$db"
        $env:DB_USERNAME = "fp_schema"
        $env:DB_PASSWORD = $secret
        $env:JWT_SECRET = $jwt
        $env:SPRING_FLYWAY_ENABLED = if($generate){"false"}else{"true"}
        $env:SPRING_FLYWAY_BASELINE_ON_MIGRATE = "false"
        $env:SPRING_JPA_HIBERNATE_DDL_AUTO = if($generate){"create"}else{"validate"}
        $env:SERVER_PORT = "0"
        $proc = Start-Process -FilePath "java.exe" -ArgumentList @("-jar", ('"' + $jar + '"')) -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden
        for ($i=0; $i -lt 100; $i++) {
            Start-Sleep -Seconds 2
            $proc.Refresh()
            if ((Test-Path $out) -and ((Get-Content $out -Raw) -match "Started FamilyPointsApplication")) {
                Write-Host "Successful startup against $db"
                return
            }
            if ($proc.HasExited) { throw "Backend exited. See $out and $err" }
        }
        throw "Timed out waiting for backend. See $out and $err"
    } finally {
        if ($null -ne $proc) {
            $proc.Refresh()
            if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
        }
        foreach ($k in $keys) { [Environment]::SetEnvironmentVariable($k,$before[$k],"Process") }
    }
}

try {
    if (-not $ValidateExisting) {
        & git -C $repo rev-parse --verify refs/tags/v0.3-workspace | Out-Null
        Check-Exit "Finding historical tag"
        & git -C $repo worktree add --detach $oldTree v0.3-workspace
        Check-Exit "Adding temporary worktree"
        $treeAdded = $true
    }

    & docker run -d --rm --name $container -e "POSTGRES_USER=fp_schema" -e "POSTGRES_DB=fp_v1" -e "POSTGRES_PASSWORD=$secret" -p "127.0.0.1::5432" postgres:17-alpine | Out-Null
    Check-Exit "Starting temporary PostgreSQL"
    $running = $true
    $ports = (& docker port $container 5432/tcp | Out-String)
    Check-Exit "Docker port inspection"
    if ($ports -notmatch '127\.0\.0\.1:(\d+)') { throw "Unknown mapped port: $ports" }
    $port = [int]$Matches[1]
    $ready = $false
    for ($i=0;$i -lt 45;$i++) {
        & docker exec $container pg_isready -U fp_schema -d fp_v1 *> $null
        if ($LASTEXITCODE -eq 0) {$ready = $true; break}
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw "Temporary PostgreSQL did not start" }

    if ($ValidateExisting) {
        Write-Host "Validating existing V1; historical source generation skipped."
        $existing = [IO.File]::ReadAllText($output)
        $fixed = [regex]::Replace(
            $existing, '(?m)^[ \t]*CREATE SCHEMA public;[ \t]*\r?$',
            'CREATE SCHEMA IF NOT EXISTS public;'
        )
        if ($fixed -ne $existing) {
            $backup = Join-Path $temp "V1__baseline_schema.before-public-fix.sql"
            Copy-Item -LiteralPath $output -Destination $backup
            [IO.File]::WriteAllText($output, $fixed, (New-Object Text.UTF8Encoding $false))
            Write-Host "Adjusted CREATE SCHEMA public for PostgreSQL. Temporary backup: $backup"
        }
    } else {
        Write-Host "Building original v0.3-workspace schema without DEV records..."
        $oldJar = Build-Jar $oldTree
        Boot-And-Stop $oldJar "fp_v1" $true $port

        $dump = (& docker exec $container pg_dump -U fp_schema -d fp_v1 --schema-only --no-owner --no-privileges --schema=public | Out-String)
        Check-Exit "pg_dump schema-only"
        $sql = (($dump -split "\r?\n") | Where-Object { $_ -notmatch '^\s*\\' }) -join [Environment]::NewLine
        $sql = [regex]::Replace(
            $sql, '(?m)^[ \t]*CREATE SCHEMA public;[ \t]*\r?$',
            'CREATE SCHEMA IF NOT EXISTS public;'
        )
        foreach ($table in @("families","family_members","family_roles","task_definitions","group_roles","user_accounts")) {
            if ($sql -notmatch ("(?m)^CREATE TABLE public\." + $table + "\b")) { throw "Original schema is missing $table" }
        }
        if ($sql -match '(?m)^CREATE TABLE public\.task_audit_events\b') { throw "V1 contains post-V1 audit table" }
        $header = "-- PRE-V2 baseline generated using the v0.3-workspace JPA mappings; no user data." + [Environment]::NewLine
        [IO.File]::WriteAllText($output, $header + $sql + [Environment]::NewLine, (New-Object Text.UTF8Encoding $false))
        $createdV1 = $true
        Write-Host "Created $output"
    }

    & docker exec $container psql -U fp_schema -d postgres -v "ON_ERROR_STOP=1" -c "CREATE DATABASE fp_validate OWNER fp_schema"
    Check-Exit "New validation database"
    Write-Host "Compiling latest branch and validating all Flyway migrations..."
    $nowJar = Build-Jar $repo
    Boot-And-Stop $nowJar "fp_validate" $false $port
    $count = (& docker exec $container psql -U fp_schema -d fp_validate -t -A -v "ON_ERROR_STOP=1" -c "SELECT count(*) FROM flyway_schema_history WHERE success=true" | Out-String).Trim()
    Check-Exit "Flyway history"
    if ($count -ne "20") { throw "Expected V1..V20 (20 migrations), got $count" }
    $validated = $true
    Write-Host "PASS: clean database migrated V1..V20 and JPA schema validated."
}
catch {
    # Save the original failure. A Windows git worktree cleanup error must
    # never hide the PostgreSQL, Maven, Flyway or JPA validation error.
    Write-Warning ("Original failure: " + $_.Exception.Message)
    throw
}
finally {
    if (-not $validated -and $createdV1 -and (Test-Path -LiteralPath $output)) {
        # Do not leave an UNVALIDATED V1 in the migration directory. It would
        # also make the next script execution fail the pre-existing V1 guard.
        try {
            $unvalidated = Join-Path $temp "V1__baseline_schema.UNVALIDATED.sql"
            Move-Item -LiteralPath $output -Destination $unvalidated -ErrorAction Stop
            Write-Warning "Unvalidated V1 moved to $unvalidated"
        } catch {
            Write-Warning ("Could not move unvalidated V1: " + $_.Exception.Message)
        }
    }
    if ($running) {
        try {
            # Ignore only cleanup errors, not build errors.
            & docker rm -f $container 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Could not remove disposable container $container"
            }
        } catch {
            Write-Warning ("Docker cleanup: " + $_.Exception.Message)
        }
    }
    if ($treeAdded) {
        try {
            # git worktree remove can fail on Windows when a file/index is
            # briefly locked (e.g., after Maven or Java). Keep this nonfatal.
            $originalPreference = $ErrorActionPreference
            try {
                $ErrorActionPreference = "Continue"
                & git -C $repo worktree remove --force $oldTree 2>$null | Out-Null
                $worktreeExit = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $originalPreference
            }
            if ($worktreeExit -ne 0) {
                Write-Warning "Git could not remove temporary worktree $oldTree (nonfatal)"
            }
        } catch {
            Write-Warning ("Git worktree cleanup: " + $_.Exception.Message)
        }
        # Safely remove stale administrative worktree entries when possible.
        try {
            $originalPreference = $ErrorActionPreference
            try {
                $ErrorActionPreference = "Continue"
                & git -C $repo worktree prune 2>$null | Out-Null
            } finally {
                $ErrorActionPreference = $originalPreference
            }
        } catch {
            Write-Warning ("Git worktree prune: " + $_.Exception.Message)
        }
    }
    if ($validated -and -not $KeepTemporaryFiles) {
        try {
            Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction Stop
        } catch {
            Write-Warning ("Temporary folder remains: " + $temp)
        }
    } else {
        Write-Host "Diagnostic logs are in $temp"
    }
}
