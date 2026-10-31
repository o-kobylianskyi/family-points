# BKL-002 — QA acceptance gaps after Backend write-path/FK audit (2026-10-31)

**Status: PARTIAL SOURCE+CI ACCEPTANCE ONLY. NO RESET EXECUTION APPROVAL.**

## Reviewed evidence
- `BKL002_WRITE_PATH_COVERAGE_20261031.md`: source review of 136 controller mutation mappings; declared/direct/delegated fences and documented exclusions; **not** dynamic HTTP/transaction coverage of every route.
- `BKL002_RETENTION_FK_INVENTORY_20261031.md`: 70 application tables, 156 FK constraints in disposable V45 PostgreSQL; proposals are **not** an approved deletion graph or financial policy.
- Exact green CI SHA `b4f62fc9bd13b9c9aec223619f016bac2f7ebcc7`, run 41249950772: 329 Backend tests and 54 `PointMutationTransactionPostgresTest` cases; includes real family fence contention, stale ledger and domain writer rollback, proof one-use/concurrency, and V44→V45 preservation testing. CI does not prove deployment parity.
- Local QA: Test (14) ↔ Test2 (11) Preview/Plan denied in both directions; epoch GET denied across family boundary; browser stale-epoch POST prevented before dispatch. These are endpoint-specific and browser findings.

## QA disposition by gate

| Gate | Disposition | Remaining acceptance evidence |
|---|---|---|
| Guard uses family row lock and same transaction | **SOURCE+INTEGRATION COVERED FOR REPRESENTATIVE PATHS** | Confirm invoked for each approved executor write path, preserving original client epoch |
| All client writes on 136 mapped routes | **INVENTORIED, NOT UNIVERSALLY PROVEN** | Exact per-route exercised HTTP/transaction matrix; list and justify any unfenced exceptions |
| Existing point/task/reward/group/schedule/template/media races | **REPRESENTATIVE POSTGRES TESTS PRESENT** | Target untested branches when identified by mapping; failure before side effects |
| Cross-family Web and epoch reads | **QA PASS for Test/Test2** | Other endpoints and permissions independently scoped |
| Reset snapshot, password proof, replay | **BACKEND CI PASS FOR INTERNAL PROOF** | Public transport, owner authorization, fresh Google reauth and security review |
| Delete closure and balance/entitlement policy | **BLOCKED** | Explicit architect-approved table dispositions, semantic links, owner and financial rules |
| Atomic execute + epoch + audit + onboarding | **BLOCKED** | Implementation + rollback/commit-fault and idempotency proof in disposable PostgreSQL |
| Android stale queue and device state | **BLOCKED** | Isolated device sync/replay QA; never replay old offline writes |
| Current DEV/BETA/PROD schema and deployment | **NOT ESTABLISHED BY CI** | Flyway version, deployed SHA, tests on isolated target, backups/recovery gate |

## Next owner-specific actions

**Backend:** demonstrate one outer-transaction fence for each *proposed executor* write; publish a per-path delta for routes not dynamically covered. Add injectable exception between approved deletion groups (when deletion code exists) and assert unchanged all-or-nothing rows, epoch, audit, active entitlements and cross-family data. Test two simultaneous executor operations, executor vs active write, one-family contention vs independent family, idempotent same request and changed-payload conflict. Do not build tests on real Test/Test2 user data.

**Architecture:** decide immutable owner and audited legacy enrollment, shared global-account permissions, financial ledger and unfulfilled reward entitlements, approved category combinations, retention/FK closure and onboarding lifecycle. Until then, execution stays disabled.

**Web/Android:** no destructive CTA or offline retry; validate error codes, epoch quarantine, startup/reload and manual recovery. Keep all four locales.

**Release/QA:** pin SHA and V45+ schema in isolated disposable target; require evidence, verified backups and post-rollback tenant audit. Current 18/18 browser baseline and 3/3 epoch UI suite are not executor signoff.

**Safety:** do not call /execute or apply test mutations to persistent workspace 14 (Test) or 11 (Test2).
