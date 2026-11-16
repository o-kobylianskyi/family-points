# BKL002-QA-20261116-01 — Backend V51/V52 recovery + native cross-DB + financial closure

**Status:** READY_FOR_QA (not independently accepted). **Source baseline:** backend `257ba06895f694c00d83bcc0aadf1fa4d1dc0e86`; re-pin current source SHA on pickup; later commits may update CI/tooling. **Owner:** Backend producer; QA/Testing validates and owns queue status. **Issue:** #15. **Protocol:** BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md.

## Evidence already provided
- Operator-reported local recovery assessment suite **20 tests, 0 failures/errors/skips, BUILD SUCCESS**, 2026-11-16 19:17 CET (exact checkout SHA not supplied).
- Operator-reported cross-DB native restore suite **10 tests, 0 failures/errors/skips, BUILD SUCCESS**, 2026-11-16 19:34:49 CET; fixture corrected after initial `TEMPLATE_ID_REMAP_FAILED` via `5ed18c0` (exact checkout SHA not supplied).
- 2026-11-16 latest financial closure suite: operator says **BUILD SUCCESS**. Exact checkout SHA, counts and skip totals not yet supplied. Do not transmute into independently QA accepted.
- Developer-side tool cannot run Windows Docker/Testcontainers directly; above reports originate from operator. `READY_FOR_QA` identifies test cases and implementation, not current exact SHA developer preflight PASS.

## P0 independent QA validation (synthetic data only)
**Environment:** isolated Docker/Testcontainers PostgreSQL 18 and Java 21; no DEV/BETA/PROD family data; ensure no external network writes/deploy.

```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,DisposableCrossDatabasePortableRestorePostgresTest,DisposableComposedNativeRestorePostgresTest,WorkspaceBackupRehearsalAssessmentServiceTest,WorkspaceDisposableRestoreRehearsalPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupArtifactRegistryPostgresTest,WorkspaceBackupRegistryStagingCoordinatorTest,WorkspaceBackupArtifactStagingServiceTest,WorkspaceBackupLocalArtifactStoreTest" test
```

### Acceptance scenarios
1. **BKL002-QA-REC-01:** real encrypted `.fpbackup` export from source PostgreSQL A, native bounded target restore on *independent* PostgreSQL B; migrated schema V52; preserve media byte digest and catalog/private mapping.
2. **BKL002-QA-REC-02:** remapped fresh group/template/media IDs, member and binding FK; source, target, neighbor workspace isolation; no accidental shared PK semantics.
3. **BKL002-QA-REC-03:** duplicate replay rejected without extra target rows, wrong AES key rejected, tamper rejected before mutations; late conflict atomic rollback.
4. **BKL002-QA-REC-04:** registry/staged file SHA, size, tenant, snapshot, selected-category and count comparison; rollback-only rehearsal explicitly remains `verifiedForRestore=false`.
5. **BKL002-QA-RET-05:** expired file cleanup serialized against active lease; wrong-family lease release denied; physical cleanup and metadata expiry evidence. Re-run with concurrent stress if feasible.
6. **BKL002-QA-FIN-06:** `WorkspaceFinancialRestoreClosureService` always marks native financial replay **unsupported**; detects point/reputation/group ledger, purchases, requests, task executions/slots; missing category explicit. Group-point ledger still unexported; *never* silently clear history.
7. **BKL002-QA-FIN-07:** two-family PostgreSQL financial fixture demonstrates source ledger amounts unaffected and no cross-tenant counts.
8. **BKL002-QA-GATE-08:** reset/execute stays HTTP 423; no VERIFIED registry promotion, HTTP restore command or PROD release is enabled. Test skipped/blocked only with explicit reason.

## Known blockers and next Backend package
- Native group/media/template/work-node/definition replay lives under `src/test`; not a production full import implementation.
- Missing complete FK/financial ledger/obligation/settlement reconciliation, owner binding + recent Google OIDC proof, passphrase KDF, approved HTTP file export/import, 30-day scheduled physical retention reconciliation, full/selective target-replace receipts, independent durable VERIFIED evidence. Full requested reset/restore must fail closed.
- Backend will continue independent implementation during QA execution. QA should consolidate severity, failure logs, exact checked-out SHA, per-suite PASS/FAIL/SKIP, CI link and remediation request in issue #15; do not ask operator to run every small Maven batch.
- QA status transitions: READY_FOR_QA -> IN_QA -> PASS / FAIL / BLOCKED_ENV / STALE_SHA, with evidence. No automatic chat-to-chat notification: QA must explicitly pick up this document.

**Release gate:** BETA HOLD; reset execute HTTP 423; no destructive real family tests/deploy authorization.
