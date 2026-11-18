# FamilyPoints — Backend A / Backup & Portable Archive journal

## 2026-11-18 — B-A-03l closure / B-A-04 source contract kickoff
- Source-of-truth branch: `main`; independent Backend A handoff `docs/coordination/handoffs/BACKEND_A_LATEST.md`.
- Retention B-A-03k exact-sha `f10537ae` Actions `44275795521` overall SUCCESS **but backend Maven/test steps SKIPPED**. Actual PG18 fault-injection must remain QA UNVERIFIED, not be falsely marked PASS; step details for Java job `134703994678`. B-A-03k code includes physically unlinked ciphertext + forced SQL rollback and disk-failure negative tests. Do not enable production purge from this rehearsal.
- B-A-03l retention architecture disposition: file unlink and PostgreSQL commit are non-atomic. Final recommendation for Architecture review: approved file quarantine with durable intent, two-copy verification, lease/ownership locking and separate post-commit purge, audited reconciliation on crash. No production retention algorithm or endpoint changed. Archive deletion still operator/Release gated.
- B-A-04 initial **TEST ONLY**: `DisposablePortableMemberSourceCatalog` and `DisposablePortableMemberSourceCatalogPostgresTest`. Read actual `family_roles`/ `family_members` in one active REPEATABLE_READ SQL transaction, reject foreign-role parentage, capture original member ID/type/status/tombstone with source family and role provenance, omit private names/accounts/passwords/OAuth. PG18 two-family fixture plus weak transaction and cross-family wrong-role negatives.
- Field-level source/format decision request `docs/coordination/requests/B-A-04_MEMBER_SOURCE_FIELDS_AND_WIRE_GATE_20261118.md`. User approved DEC-01…08, Backend B conditionally ACKed logical member provenance only. **Architecture wire field/schema version/hidden-category/IMP ACK remains mandatory** before production exporter or inspector edits.
- Security: original/target ID matching cannot be guessed; no credentials or owner auth exported; Restore Executor remains B, owner binding and permits C, immutable finance D. `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
- QA: run `DisposablePortableMemberSourceCatalogPostgresTest`, `WorkspaceBackupRetentionRollbackGapPostgresTest` and both source/reverse audit race tests with actual Testcontainers on pinned SHA; complete CI `[full-ci]` requested and reported separately with real job-step evidence.
- Next large B-A-04b: after Architecture signed vNext contract implement additive privacy-reviewed source archive and matching inspector with backwards compatibility; until then source FK closure matrix and synthetic validator only.


## 2026-11-18 — B-A-04b test-only member FK closure
- SHA `db9e4eb`: `DisposablePortableMemberReferenceClosure` implements a strict original-member identity parent resolver for curated source categories, with explicit null-vs-missing, source-family and role provenance, 2000 row limit, unknown category denial.
- SHA `426fef3`: source closure negative unit suite; `b795d5a`: real PG18 migration column/nullability drift verification.
- SHA `4cdb874`: source roster fail-closed role/member row bounds, duplicate role codes and nonintegral IDs.
- QA packet `docs/coordination/requests/B-A-04B_MEMBER_REFERENCE_CLOSURE_QA_20261118.md` @`3392121`.
- Scope limitations: `TASK_EXECUTIONS.original_assigned_member_id` is optional but MUST be explicitly present in vNext; executor remains independent. The audited matrix is incomplete: account membership auth metadata excluded; member exception periods, other category polymorphic links, media and finance source types require D/B and Architecture review.
- Existing full-run `44277446121` for earlier `3cbe37b` remains in progress; all new B-A-04b tests need separately pinned run. PROD importer/inspector/archive format unchanged. HTTP423/BETA HOLD.


## 2026-11-18 — B-A-04c CI red repair and full original-member FK inventory
- Explicit full CI SHA `32cff0f` Actions `44278301617` FAILED Java testCompile at `DisposablePortableMemberSourceCatalog.java:98` unknown variable `n`; actual Java Maven ran and red, while Web/Android/Compose passed. Corrected source with `b6c4669`, exact updated SHA CI still pending; do not assume PASS.
- Repaired PG18 cross-family role negative fixture to respect production V28 trigger, then intentionally synthesize historical corruption only with disposable DB trigger temporarily disabled; fixture restores trigger in finally (`45c7ff9`).
- Added source-only `MEMBER_EXCEPTION_PERIODS.member_id` to reviewed provenance matrix `e58300b`, PG schema nullability guard `3d14025`.
- New `DisposablePortableMemberFkInventoryPostgresTest` `e29f276` and conservative evidence revision `be84906` query all PostgreSQL FK to `family_members`, assert required task/ledger/reward/exception source edges, print unreviewed edges; do not auto-authorize portable vNext fields.
- QA packet `docs/coordination/requests/B-A-04C_CI_FIX_AND_MEMBER_FK_INVENTORY_QA_20261118.md` `b6e9ce7`. Next B-A-04d: CI results, more FK sources, Architecture version/privacy/IMP ACK; production importer and exporter unchanged, HTTP423/BETA HOLD.


## 2026-11-18 — B-A-04e strict source-ID semantics & family exception-period closure
- Exact old baseline `64f4cdea` Actions `44282865822` FULL CI SUCCESS, non-skipped Maven/Testcontainers; backend-Surefire artifact confirmed. This does not validate B-A-04e.
- Test-only `DisposablePortableMemberReferenceClosure` strengthened to require Integer/Long source IDs (reject floating/BigDecimal aliases), role-code uniqueness and catalog row/type limits (`36e3a16`), with adversarial negative suite `4034754`.
- Test-only PG18 `DisposablePortableMemberExceptionClosurePostgresTest` `33679c7`: real family A/B and member exception periods, one-source-family snapshot accepted, neighbor member FK injection rejected.
- Source category `MEMBER_EXCEPTION_PERIODS` is not a current v1 export category. Architecture versioned source decision and B/D consumer review mandatory; not shipped as vNext.
- QA: `docs/coordination/requests/B-A-04E_STRICT_SOURCE_ID_AND_EXCEPTION_CLOSURE_QA_20261118.md`, source unchanged outside tests/docs; PROD Restore, file deletion, Reset remain gated.
