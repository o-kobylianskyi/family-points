# FamilyPoints Web — Issue #18 onboarding acceptance

Scope: non-destructive first-family onboarding at /family-setup; **no reset, no release, no automatic activation**. Existing /templates remains authoritative editing entry.

## Contract verified in Web source
- GET /template-catalog — global templates/packs; no write.
- GET /workspaces/{workspaceId}/templates — workspace private library.
- POST /workspaces/{workspaceId}/templates/imports/preview — explicit read-like preview (POST); does not imply a commit.
- POST /workspaces/{workspaceId}/templates/imports — explicit user confirmed import, without creating active tasks or rewards.
- GET/POST /workspaces/{workspaceId}/member-groups — separate MemberGroup model; never substitute a WorkNode GROUP.

## QA matrix — run on new disposable DEV family only
1. Fresh parent visit: no POST until user creates a group or confirms import.
2. Parent selection: TASK/REWARD, all packs, category and search; select overlapping packs; unique codes only. Preview includes intended toImport and alreadyImported.
3. Empty/unknown catalog, network interruption, 401/403 and cross-family mismatch: fail closed, no import POST.
4. Start import with network interruption; reopen and reconcile GET private library. A retry requires new preview and explicit confirmation; no blind POST replay.
5. Switch family during catalog load, preview, import and group creation: previous workspace data must not populate new workspace state.
6. Reload midway: selection is session-only, scoped to authenticated account and family; preview is discarded; no automatic POST.
7. Import TASK and REWARD: private template records created, no task definitions, reward definitions, ledger writes or purchases. Verify nullable reward price is not converted into zero.
8. Group create: MANAGE_GROUPS/ADMIN_OVERRIDE only; name duplicate check; ambiguous POST triggers read/reconciliation before another manual attempt.
9. Child account: manager-only imports and group creation return 403. UI cannot be considered a security boundary.
10. Desktop/mobile, light/dark, UA/RU/DE/EN; labels and layout; keyboard focus on all buttons/inputs.
11. Open /templates afterward and edit/imported template; activation still explicit in dedicated task/reward flows.
12. BKL-002 reset->onboarding NOT authorized; never clear Test/Test2 or production families.

## Expected evidence
Record exact main SHA, Vite build output, test family ID (not credentials), requests with paths/status, template counts, group IDs, no unintended ledger mutation, and screenshots across four locales. This document defines test cases; it does not claim executions or passes.

## Blockers
Backend durable onboarding status/idempotent step completion and WorkNode group setup contract are not yet established in this Web package; do not simulate them with client-only completion flags. Backend must define authority, tenant permissions, recovery and replay contract. Beta release remains on hold until coordinated acceptance.
