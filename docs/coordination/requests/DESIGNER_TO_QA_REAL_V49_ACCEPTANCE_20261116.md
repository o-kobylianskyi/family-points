# FamilyPoints — Designer → QA: operator-assisted real V49 acceptance
Date: 2026-11-16. Owner: QA, with user/operator present. Designer does not execute these tests.

## Scope and hard safety rules
- Perform all **real authenticated** writes only in an explicitly approved **disposable DEV/QA family**, with confirmed isolated workspace ID and owner credentials. User's existing families, public BETA/PROD, and other tenants are out of scope.
- Record environment, base URL (without secrets), git SHA, app/API version, workspace ID redacted in public logs, epoch, timestamp, browser/device, full pass/fail/skip and request ID evidence. Confirm operator's explicit approval of the disposable workspace before performing mutations.
- **Never** invoke BKL-002 `data-reset/execute`, backup replacement, restore, destructive SQL, or live deployment based on this packet. Any reset/restore testing has a **separate gate**: Backend safe owner-bound API, verified protective backup, Architecture/Release approval and explicit user approval.
- No hidden POST replay: simulate lost response only in isolated QA fixtures, preserve original UUID; do not retry create-family POST to recover uncertainty.

## Phase A — non-destructive, authenticated preflight (QA may run now)
1. Pin a clean exact source SHA including Designer's recent Web/Android changes; compile Web/Android and capture CI same-SHA job links. Previous full green `3c0cf014` and Android 98/98 `09da7231` are **historical evidence only**, not proof for newer commits.
2. Verify feature flag, workspace permissions (owner, child, foreign account) and actual V49 API availability. GET `/workspaces/{id}/setup`: assert workspaceId, dataEpoch, revision, seven allowed step identifiers and legitimate empty/partial initial map.
3. Read-only catalog: exactly **142** starter TASK/REWARD templates and **6** starter packs; validate locale labels UA/RU/DE/EN and pack/category/search filters, including 0 matches, filtered multi-select and switching TASK/REWARD.
4. Open V49 multiple times, reload page, switch family/account, open secondary tab. No implicit workspace creation, step PATCH, template POST, activation, purchase or ledger mutation from navigation.
5. Verify new-family vs existing-family Android navigation and GET-only status view. Initial empty/partial steps must not be called invalid. Four languages, light/dark, narrow viewport. Read-only retry sends only GET.
6. Confirm role access: unauthorized child/foreign tenant receives 401/403 and no destructive/setup controls or cross-tenant data; exact HTTP response/result should be recorded. Reauth/logout and epoch changes must clear stale workspace view.

## Phase B — actual V49 setup writes (only in approved disposable family)
1. With operator, create one **new** disposable family through normal app action. Record unique identity and initial setup revision/epoch. Never repeat create POST automatically after lost response.
2. Explicitly complete/skip all **seven** setup steps: MEMBERS, MEMBER_GROUPS, WORK_NODE_GROUPS, CATALOG_SELECTION, TASK_REVIEW, REWARD_REVIEW, FINAL_REVIEW. For each PATCH verify revision +1 and GET reflects same intended change; do not auto-replay a lost PATCH.
3. Select a few TASK and REWARD entries across two packs; verify selection persists across search, pack/category switching, and reload behavior per current UX specification. Check duplicates/empty selection and `alreadyImported` versus `toImport`.
4. Call **review** through UI: side-effect-free POST; assert exact selected ordered codes, same workspace/epoch/revision, `requiresExplicitConfirmation=true`, `createsActiveDefinitions=false`; active task/reward counts and ledger remain unchanged.
5. Explicitly tick confirmation and finalize import **once**. Capture original Idempotency-Key UUID, response receipt, revision +1, status COMPLETED. Validate only **private template imports**; no active task/reward definitions, balances, purchases, obligations or ledger writes.
6. Reload, log out/in, revisit Settings and a second browser tab. Server remains COMPLETED, private imports persisted, no second finalize, original receipt tied to same account/family/epoch/revision/ordered codes. A stale pending storage marker must block a new POST.
7. Deliberately lost finalize response: **only via synthetic/disposable controlled fault injection**. The UI must GET the receipt and subsequent setup state, never issue new UUID/POST; if receipt/state can't be verified, remain blocked and present recovery action.
8. Separate explicit activation (where supported) of one private TASK and one private REWARD, after approving the action; no activation via preview/import/finalize alone. Verify amount UNSET differs from explicit 0 and time-reward mapping with backend DTO before claiming pass.

## Phase C — cross-tenant and persistence regressions
- Two independent families, owner vs child vs foreign user. Cross-family mutation/receipt/metadata denied. No leaking foreign template selection/balances. Recheck on reload.
- Negative revision, stale epoch, invalid step/code, malformed receipt, failed GET, expired credential, concurrent two-tab finalize, and duplicate original UUID. Fail closed without automatic repeat write.
- Existing-family data untouched and ledger before/after equal for setup/import-only actions. Inspect backend data through approved read-only diagnostics if available.

## Evidence and gating
For each scenario: `CASE_ID | SHA | workspace (redacted) | action | expected | actual | HTTP methods/count/UUID | PASS/FAIL/SKIP | proof`.
- QA must supply exact testcase counts plus Playwright/JUnit outputs, screenshots/traces with secrets redacted, server revision/epoch and backend row-count diff if approved.
- Existing local Web suite had operator-reported **47/47 PASS** on earlier checkout; it does not prove these new real authenticated and persisted V49 flows.
- **Release stays HOLD** until QA confirms real enabled V49 E2E and Backend BKL-002 owner-bound verified restore/execute is independently accepted.
- Record results in `docs/coordination/logs/qa.md` and cross-reference issue #18 (onboarding) and #15 (BKL-002); send defect-specific evidence to Web/Backend/Android owners. Designer will only fix verified UX defects.
