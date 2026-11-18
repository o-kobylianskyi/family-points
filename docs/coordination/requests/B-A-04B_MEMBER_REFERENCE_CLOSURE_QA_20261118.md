# Backend A — B-A-04b member-reference closure QA and Architecture gate
**2026-11-18 · Owner:** Backend A · **Status:** TEST-ONLY source preflight submitted; production wire gate OPEN.

## Scope and precise source references
- Previous `DisposablePortableMemberSourceCatalog` reads same-workspace `family_roles` and `family_members` from an active REPEATABLE_READ transaction, captures original positive BIGINT, type, role, active/deleted timestamps and excludes source member names/account credentials. This pass additionally bounds both role/member rows to 2000, denies duplicate role codes and non-integral source IDs.
- New `DisposablePortableMemberReferenceClosure` is test-only, with explicit source-member edges for:
  - `TASK_EXECUTIONS.member_id`, nullable `original_assigned_member_id`
  - `TASK_DEFINITIONS.assigned_member_id/created_by_member_id/preferred_member_id/responsible_member_id` (nullable)
  - `POINT_LEDGER.member_id`, `REPUTATION_LEDGER.member_id`, `REWARD_PURCHASES.member_id`
  - `POINT_EXCHANGE_REQUESTS.member_id`, nullable `processed_by_member_id`
  - `GROUP_MEMBERSHIPS.member_id`
  - `TASK_DELEGATIONS.delegated_by_member_id/from_member_id/to_member_id`.
  Every required source reference must be positive and resolve to exactly one original family member captured in the catalog, including tombstones. Missing nullable fields are **not** silently treated as null: explicit null is required. Duplicate member IDs, foreign source family/role and unreviewed categories deny. Max rows 2000/category.
- `DisposablePortableMemberReferenceClosureTest` covers separate executor/assignee, null assignee, missing field, missing/foreign member, delegated reviewer, tombstones, wrong family/role, duplicate IDs and oversized records.
- `DisposablePortableMemberEdgeSchemaPostgresTest` uses actual PG18 Flyway DB and `information_schema.columns` to assert source table/column presence and FK-field nullability, so schema drift fails tests rather than silently weakening ancestry.
- These classes in `src/test` are **not production exporter/inspector additions**. The matrix is intentionally a reviewed *subset*, NOT proof of complete source graph closure across all 23 legacy categories, polymorphic JSON, media or finance.
- A4 wire gate request remains `docs/coordination/requests/B-A-04_MEMBER_SOURCE_FIELDS_AND_WIRE_GATE_20261118.md`. Backend B conditional logical ACK; Architecture exact schema/IMP and Backend C/D field-level acceptance not observed as complete. Do not change current V50 source category allowlist or version without approval.

## Tests and negative acceptance
```bash
./mvnw -Dtest=DisposablePortableMemberSourceCatalogPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberEdgeSchemaPostgresTest,WorkspaceBackupRetentionRollbackGapPostgresTest test
```
- Strong isolation / real PostgreSQL 18 / two families and role crossing denied; no global account, login, email, OAuth metadata in catalog.
- Historical assignee stays independent from executor, nullable fields explicit, foreign/tombstoned relations preserved only as ancestry.
- Source column nullability is checked against actual current migrations.
- Test asserts no production archive version change, restore execution, automatic archive deletion or real family writes.
- Exact-SHA real Maven execution and test result are mandatory: workflow SUCCESS with Java test step skipped is NOT test PASS.

## CI and delivery
A prior `[full-ci]` request SHA `3cbe37bc` run `44277446121` was still in progress at B-A-04b pickup. An updated `[full-ci]` request should include all new source tests, pin SHA and record Java/Android/Web/Compose jobs. QA owns independent acceptance, Release owns deployment. No BETA unlock.

## Next B-A-04c
Close source category-by-category FK matrix and property-based negative tests; after Architecture exact A4-01…09 signoff implement additive authenticated vNext archive producer and inspector in coordinated A/B change. Finance unknown sources remain FAIL CLOSED. `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
