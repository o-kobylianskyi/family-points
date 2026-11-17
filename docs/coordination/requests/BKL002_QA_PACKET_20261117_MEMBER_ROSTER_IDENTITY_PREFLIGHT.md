# BKL-002 Backend #41–44 — two-database roster/identity preflight

Date 2026-11-17 CET. **TEST ONLY, disposable two PostgreSQL 18 databases**; no backup-format promotion and no production restore endpoint.

## Verified boundary from current source
- `WorkspacePortableBackupExporter.SCOPED_TABLES` deliberately excludes `FAMILY_MEMBERS`.
- `ProtectiveBackupImportInspector.CATEGORIES` rejects unknown categories including `FAMILY_MEMBERS`; `checkMemberParent` marks `IDENTITY_REBIND_REQUIRED`.
- `WorkspaceMember` identity columns: `family_id`, `member_type`, `family_role_id`, `name`, `active`, `deleted_at`. Member IDs/names/role codes alone are NOT reliable portable identity proof.
- Do **not** silently add a new archive category or mark restores VERIFIED in V50.

## Source code changes
- `4b7ccd57fc0a781b710f5438338dafa4bca4254d`: test-only `DisposableMemberMappingPreflight` queries original and target family rosters against two independent PostgreSQL DBs. Caller-supplied map MUST have exact source roster key set, injective target values, target member exists in explicit target family, and original/target member_type and role code match. Rejects zero/invalid/foreign member ids, duplicate target ids and missing original members. Does not infer mapping by name, order, ID coincidence or role.
- `c4e5d23dce076b73ca3311c3725e0b9610d10de1`: integrated into existing 2-PG18 multi-exchange test, validates original single member then two member roster before synthetic replay; rejects neighbor and missing/duplicate mappings.
- `997fb762893a44cb2fa44c264d1a55ff5fd24937`: explicit type drift (target CHILD vs original PARENT) rejected; restore disposable target type then valid roster proof.

## QA
Pin immutable latest commit SHA, JDK21, Docker + PostgreSQL 18:
```powershell
mvn -Dtest=DisposableMultiExchangeNativeRestorePostgresTest,DisposableLedgerSourceGraphVerifierTest test
```
Run prior #24 twelve-class and full four-component CI after fixing targeted failures. Record exact commit SHA, run URL, containers started, actual test/failure/error/skip counts, first stack on failure and no neighbor mutation. Previous confirmed 479-Maven green @`8c397d0` predates #25–44; no new PASS may be inferred from commits or source review.

## Remaining structural blockers — explicit non-claims
- New verifier works only when original/source PostgreSQL **still exists**. It does NOT create a sealed portable source member catalog, authorize deleting original data, or establish identity after original loss.
- Same-family map of two identically typed/role members could be swapped and still pass this roster structural preflight. A separate owner-approved stable identity / rebind contract is required. Signed roster alone must not turn name equality or role into authentication.
- Member family role may be configurable/renamed; requiring code match is test-only conservative policy.
- Missing original-archive member catalog, full source→target identity rebind, owner fresh authentication, protected durable verification receipt, all-category graph closure, complete full restore transaction and same-SHA all-four CI remain P0 blockers.
- Production `restorable=false`, `replaySupported=false`, reset Execute HTTP423, BETA HOLD. Never test reset on a real family.
