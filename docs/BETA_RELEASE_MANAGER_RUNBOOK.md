# FamilyPoints — Release / DevOps runbook (2026-11-16)

> **State:** Source-side release tooling only. The currently dispatched P0 Recovery work is **BETA HOLD**: no new Backend/Web release until BKL-002 owner-bound, protective backup, verified restore, safe execute and QA are accepted. A green build is insufficient. This document and the scripts below do NOT authorize deployment, database deletion, live data testing or a Flyway migration.

## Roles and entry points

| Component | Command / file | Effect |
| --- | --- | --- |
| Local Release Gate | `scripts/beta/release-gate.ps1` | Local SHA/status, optional isolated QA smoke, *unverified* tagged evidence draft, opt-in post-deploy smoke |
| GitHub CI | `.github/workflows/ci.yml` | Push CI (scoped) or full CI on `[full-ci]` push / manual Actions workflow dispatch |
| Android independent CI | `.github/workflows/android-p0.yml` | P0 Android tests |
| Android manager | `scripts/android/release.bat` | status/build/install/logs/publish |
| Backend/Web deploy | `scripts/beta/deploy-release.sh` on Contabo | **Explicit approval required**; deploy can perform Flyway migrations |

### Normal IntelliJ workflow — no GitHub CLI required

Use IntelliJ Git to pull/push as usual. CI runs automatically on push; a `[full-ci]` commit or a manual run started from GitHub Actions executes all four jobs. **A scoped green push is not full release acceptance.** The Release coordinator can inspect GitHub Actions through the connected repository access and record the *exact SHA, run URL, job names, and executed steps* in `docs/coordination/logs/release.md`.

```powershell
# Local SHA only, no GitHub authentication or PROD/BETA request
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action status

# Show the exact SHA and Actions link; CI remains explicitly UNVERIFIED here
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action ci

# Only once Architecture + QA explicitly approve and an annotated tag exists:
# Writes an evidence DRAFT; fullCiVerified=false, never a release approval
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\beta\release-gate.ps1 -Action prepare -Tag vX.Y.Z-beta.N
```

**No `gh` installation or login is needed.** The Release Gate intentionally never reports CI PASS from local `ci`/ `prepare`; it cannot independently authenticate private repository CI results from a local checkout. A release is **HOLD** until verified exact-SHA GitHub Actions evidence and BKL-002/Web/QA/Android acceptance are recorded. Never equate a `prepare` draft with release approval.

### Reliable per-push GitHub Actions reports

`FamilyPoints CI` now uses a **unique concurrency group per Actions run** and `cancel-in-progress: false`. A new push no longer automatically cancels or replaces the previous push CI, including runs from simultaneous Backend/Web/Android chats. Trade-off: back-to-back pushes may queue more CI work; runner capacity and GitHub limits still apply, so this does not guarantee immediate completion.

The `CI / per-commit outcome report` job runs with `always()` after component jobs, even when any component failed or was skipped. Its run summary and 14-day `ci-push-result` JSON artifact record exact SHA, event, changed-file scope, selected components, actual job conclusions, and whether it is **scoped CI PASS** versus **full CI verified**. For docs-only pushes the summary explicitly states **no component tests selected / NOT FULL CI**, instead of misleadingly implying all four builds passed. If the scope evaluation fails or a selected job fails/cancels, the report job fails. This report cannot prevent pre-job GitHub infrastructure / workflow-parse failures; those must be diagnosed separately in Actions.

### Automated exact-SHA full CI evidence (no local `gh`)

The `FamilyPoints CI` GitHub workflow now includes job **`Full CI / exact-SHA evidence`**. On runs where change routing selects all four components, it checks the **job conclusions** for Backend, Android, Web, and Compose. A non-success job makes the evidence job fail; only four successes produce a JSON `full-ci-evidence` artifact (30-day retention), and the run summary records the exact tested commit SHA and run URL. For scoped runs where any component is intentionally unselected, the attestation job is skipped — **this is not full-CI acceptance**.

Inspect the workflow directly in the GitHub Actions UI from IntelliJ/browser; no GitHub CLI login is needed. The Release coordinator must still confirm that the evidence job and all four build/test steps ran on the same SHA, then record that evidence in the Release log. The artifact explicitly contains `qaAccepted=false`, `releaseApproved=false`, and `deploymentExecuted=false`; it cannot authorize a production/BETA deployment or replace BKL-002/QA acceptance.

### Environment isolation

- `-Action status`: local git only.
- `-Action ci`: displays exact SHA and GitHub Actions link; **does not run or verify CI**.
- `-Action prepare -Tag …`: checks clean git and annotated tag at exact SHA and writes a local *unverified* evidence JSON; **no** CI pass claim.
- `-Action qa -BaseUrl https://YOUR-ISOLATED-QA-HOST`: explicit read-only GET smoke for an operator-approved disposable QA host, with public FamilyPoints domains blocked and redirects disabled. Hostname alone does not prove data isolation.
- `-Action post-deploy -AllowPublicSmoke`: **only after a separately authorized publication**, read-only public Web/API/APK smoke. Never part of readiness.

The public Android version manifest is still read by the separate signed Android release manager to enforce monotonically increasing version codes, and is not part of CI.

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

For the previous beta.11, operator reported local build+install+Google login PASS and Contabo server printed `APK published`. Postpublish Windows JSON verifier failed; helper source was fixed. **Public beta.11 manifest and matching checksum were subsequently confirmed by the operator via `release-gate.ps1 -Action status` at source SHA `4d70145e`; this was a legacy GET-only postpublication check, since moved to explicit `post-deploy` mode.**

## Handoff and remaining gates

1. **Android P0 issue #19:** the isolated IPv4 MockWebServer fix is in source; verify CI XML for three tests and full Android run on release SHA.
2. **Backend P0 BKL-002 issue #15:** protective **verified** backup/restore with owner proof, physical retention, atomic scoped FULL/SELECTIVE restore, safe HTTP execute and disposable PostgreSQL acceptance. Existing destructive execute is HTTP 423.
3. **Web/QA issue #18:** seven-step authenticated onboarding and full disposable cross-tenant QA. A prior 47/47 local Playwright PASS is not current-SHA restore acceptance.
4. **Release:** obtain manual all-four-job exact-SHA CI PASS plus separate Android P0 outcome, final architecture/QA acceptance, then explicit user approval of version and host operation. Keep BETA HOLD until then.

Update `docs/coordination/logs/release.md` with every evidence SHA, test counts, CI run links, blockers and next actions. Never equate source committed with tested, tested with deployed, or deployed with verified recoverability.
