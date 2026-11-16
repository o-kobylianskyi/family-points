# FamilyPoints — Release / DevOps runbook (2026-11-16)

> **State:** Source-side release tooling only. The currently dispatched P0 Recovery work is **BETA HOLD**: no new Backend/Web release until BKL-002 owner-bound, protective backup, verified restore, safe execute and QA are accepted. A green build is insufficient. This document and the scripts below do NOT authorize deployment, database deletion, live data testing or a Flyway migration.

## Roles and entry points

| Component | Command / file | Effect |
| --- | --- | --- |
| CI / release gate | `scripts/beta/release-gate.ps1` | Read-only public smoke, exact-SHA manual four-job CI gate and local evidence export |
| CI workflow | `.github/workflows/ci.yml` | Manual dispatch runs all 4 jobs, isolated from push cancellation |
| Android independent CI | `.github/workflows/android-p0.yml` | Android P0 targeted cases, independent concurrency, full suite optional |
| Android manager | `scripts/android/release.bat` | status/build/install/logs/publish |
| Backend+Web deployment | `scripts/beta/deploy-release.sh` on Contabo | **Explicitly authorized only** annotated-tag deployment; potential Flyway migrations |
| Postdeployment internal smoke | `scripts/beta/smoke.sh` | Local Web+backend HTTP checks |

### Recommended Windows sequence (non-destructive until explicitly choosing publish/deploy)

```powershell
cd C:\Users\Oleh\IdeaProjects\family-points
git status --short
git pull --ff-only origin main

# Public HTTP 200 Web + API + Android APK and manifest version/hash.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action status

# Require installed/authenticated GitHub CLI ("gh auth login").
# Enqueue *manual full* four-job CI at current origin/main.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action ci -DispatchFullCi

# After manual CI has completed GREEN, verify four *executed*, not skipped, jobs
# for the exact local SHA.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action ci

# ONLY after Architecture/Backend/QA permit a release:
# Requires a real annotated tag pointing to the EXACT local SHA.
# Produces an ignored JSON evidence file; no deploy.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action prepare -Tag vX.Y.Z-beta.N

# Android path (separate; signature and installed app tests required):
.\scripts\android\release.bat status
.\scripts\android\release.bat build
.\scripts\android\release.bat install
.\scripts\android\release.bat logs
# Publish ONLY after device, OAuth and Backend API compatibility smoke.
.\scripts\android\release.bat publish
```

The gate requires **`gh`** for CI and uses `curl.exe` for public HTTP. Windows PowerShell 5.1 `Invoke-WebRequest` has previously misparsed valid JSON, therefore public verification intentionally uses curl. `release-evidence/` is ignored and contains NO credentials. It records source SHA, annotated tag, successful *manual full* CI run, public APK version and hash, and explicitly records unverified deployment/Flyway/BKL-002 readiness as `false`. The evidence is not proof of actual runtime Git revision.

**Important CI nuance:** automated push CI uses changed-path routing. A GitHub Actions run with a green badge may have **skipped** one or more component test steps, so it is NOT an all-four-component release gate. Use the manual `workflow_dispatch` workflow; `release-gate.ps1` checks the run SHA AND the actual run steps for Maven, Android Gradle, Web build, Compose configuration. Android independent P0 reports are tracked separately; do not treat a pending or skipped job as PASS. An exact-SHA success can become stale when another chat updates main; rerun on the chosen final release SHA.

## Actual server deployment — separate step, no automatic execution

The existing reviewed server entry point remains authoritative, **only after a fresh explicit user approval of the particular version and data risk**:

```bash
cd /opt/familypoints-beta
bash scripts/beta/deploy-release.sh vX.Y.Z-beta.N
```

The server script requires clean checkout, existing `.env`, annotated tag, confirms backup, builds Backend+Web, checks local `/healthz`, SPA, `/api/client-health` and public API. It does NOT update Android APK. The alternative `--skip-backup` requires additional explicit approval and server confirmation `DEPLOY-NO-BACKUP`; do not infer authorization from the user's earlier beta database backup waiver. Forward-only Flyway means binary rollback cannot reverse migration changes. **Never reset, truncate, rebuild or migrate real family data to satisfy QA without the user's separate scoped permission.**

After authorized deployment record independently: exact tag+SHA; `docker compose -f compose.beta.yml ps`; health; Flyway schema history and failure state; public HTTP; all relevant onboarding, rewards, ledger, tenant smoke; public Android manifest version, SHA256, endpoint. A successful shell command by itself is not enough for compatibility or data integrity acceptance.

## Android signed APK and signing continuity

The tested Android Manager automatically chooses public `versionCode+1`; locally saved, gitignored `release-config.local.json` holds non-password paths and the **Web application** Google OAuth Client ID. Build asks key passwords interactively. It verifies pinned release signing cert SHA-256 `167d63a05676a8fc9a1671be11a6d7c1246ff9d97286efca30cb5e104c152d4f`. Do not replace the existing `familypoints-release.jks`, silently uninstall Android or discard unsynced offline data. Public beta.9 used a different signing key.

For the previous beta.11, operator reported local build+install+Google login PASS and Contabo server printed `APK published`. Postpublish Windows JSON verifier failed; helper source was fixed. **No independently supplied public-manifest verification was present in the release log at last checkpoint; obtain current public evidence before claiming published hash is confirmed.**

## Handoff and remaining gates

1. **Android P0 issue #19:** the isolated IPv4 MockWebServer fix is in source; verify CI XML for three tests and full Android run on release SHA.
2. **Backend P0 BKL-002 issue #15:** protective **verified** backup/restore with owner proof, physical retention, atomic scoped FULL/SELECTIVE restore, safe HTTP execute and disposable PostgreSQL acceptance. Existing destructive execute is HTTP 423.
3. **Web/QA issue #18:** seven-step authenticated onboarding and full disposable cross-tenant QA. A prior 47/47 local Playwright PASS is not current-SHA restore acceptance.
4. **Release:** obtain manual all-four-job exact-SHA CI PASS plus separate Android P0 outcome, final architecture/QA acceptance, then explicit user approval of version and host operation. Keep BETA HOLD until then.

Update `docs/coordination/logs/release.md` with every evidence SHA, test counts, CI run links, blockers and next actions. Never equate source committed with tested, tested with deployed, or deployed with verified recoverability.
