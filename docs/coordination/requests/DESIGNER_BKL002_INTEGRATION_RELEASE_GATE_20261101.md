# Designer BKL-002 — integrated UX/API/QA release gate (2026-11-01)

**Current source checkpoint:** GitHub main `297185651` observed before this documentation commit; shared main is moving. **Status: HOLD**. Do not present a queued/cancelled CI run or previously reported local Playwright as exact-current green.

## Verified facts
1. `WorkspaceProtectiveBackupController` is still metadata-only (POST registry request, GET list, GET detail). No user-facing HTTP `.fpbackup` download/upload, FULL/SELECTIVE restore apply/receipt or approved reset execution exposed by that controller. Internal exporter/portable archive checks and PostgreSQL rollback rehearsals **do not** prove an executable restore.
2. Web `FamilyDataSafetyPlan` now has three tabs Backup / Reset / Restore and nests existing `FamilyResetReadiness` read-only server dependency diagnostics inside Reset. Settings shows exactly one parent-only family-data entry point. Reset/Restore remain non-executable.
3. Web FULL reset presentation distinguishes DELETE, RESET_DEFAULT and BLOCKED for currencies/settings and member/economy; global owner/account/other families stay protected. Restore FULL count is intentionally zero pending validated archive manifest; no implied restore for an arbitrary selected local category.
4. `qa/e2e/tests/family-data-navigation.spec.js` is staged with manager navigation, blocked FULL rows, no unsafe network writes, local TTL copy explanation and optional child account exclusion. Its existence is **not** a Playwright PASS.
5. QA published `QA_BKL002_END_TO_END_ACCEPTANCE_GATE_20261101.md` covering E01-E30 disposable PostgreSQL and W01-W12 UI/API acceptance. A prior operator `BUILD SUCCESS` for an aggregate exporter/preview/relational-rehearsal command is **not** evidence for a complete restoration engine.

## Cross-team complete integration packet (no additional placeholders)
### Architecture / Backend prerequisite
- Sign per-category FULL/SELECTIVE replace/merge vs retained economic evidence, ID/FK remapping, owner/shared global account, Google fresh proof, key/passphrase portability, and local 30-day TTL semantics.
- Prove **real** family-scoped encrypted export→decrypt→verified staging→FULL/SELECTIVE apply into domain tables→commit→compare in disposable PostgreSQL, with rollback, second-family protection, media bytes, ledger balancing, and correct dataEpoch.
- Publish versioned HTTP transport (archive download `Content-Type`/`Content-Disposition`/`no-store`, upload bounds and explicit integrity/schema/family rejection), preview of archive contents/category counts/conflict action, idempotent apply/receipt with original UUID, fail-closed 409/422/423 and fresh reauth. Exact routes must come from code, not speculative Designer URLs.
- A VERIFIED metadata row alone is not proof the archive can be recovered and applied.

### Web integrated acceptance
- Present Backup/Restore/Reset as separate intents. Keep **all file and destructive controls disabled** until above contract, signoff and QA evidence.
- Reset FULL must never check every category as DELETE. Restore FULL must never claim all category contents are available without archive manifest. SELECTIVE must not implicitly include dependent categories; show BLOCKED and explicit user decision.
- Upon approved HTTP: wire owner/manager permissions, file preview, original payload UUID stable across timeout/retry/refresh, no automatic retry with new UUID, same-workspace and account fencing, typed confirmation+reauth, snapshot backup before apply, receipt outcome and visible post-restore epoch refresh.
- Avoid showing duplicate planners with competing authority. Existing nested `FamilyResetReadiness` is **server diagnostic**, not a second action pathway; explicitly label it and do not sync unsafe checkbox state into a mutation.
- Four locales + light/dark/mobile + screen reader labels. Never promise “30-day undo”: local files expire, downloaded files persist with the user.

### Android integrated acceptance
- No queued reset/restore/upload offline operations. After confirmed authorized apply, quarantine old-epoch queued actions and clear account+workspace caches; verify receipt handling. Read-only metadata permitted once authorized.

### QA pinned acceptance
- E01–E30 / W01–W12 in the existing QA gate, including owner/global identities, cross-family, corrupt/foreign archive, stale epoch, original-UUID receipt recovery, local TTL vs downloaded copy, financial and media FK correctness. Include current source SHA, actual test counts, skips and job results. Only disposable fixtures for destructive actions.
- If exact-HEAD CI cancels repeatedly under shared-main changes, choose a pinned candidate SHA and inspect completed jobs for that SHA; **do not** substitute unrelated historical green as release proof.
- QA/Release must independently verify deployed DEV build SHA before declaring a UI asset or Web fix visible.

## Decision
**Designer source contract and read-only Web UX are accepted as staged; LIVE FILE BACKUP/RESTORE and RESET remain BLOCKED.** The next meaningful integration unit is the Backend-verified portable HTTP exporter + restorer + receipts, not further cosmetic changes. No real workspace mutation, deploy or server storage provision authorized by this note.
