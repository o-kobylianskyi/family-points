# Backend A → Architecture / Backend B / D — B-A-04 signed member-source contract freeze request
**Date:** 2026-11-18 · **Priority:** P0 · **Status:** FIELD-LEVEL PROPOSAL / APPROVAL REQUIRED BEFORE PRODUCTION WIRE CHANGE.

This is the implementation-ready source snapshot proposal following owner-approved ABCD-DEC-02/03/04/07 and Backend B's **conditional logical**, NOT wire-schema ACK (`BACKEND_B_TO_A_MEMBER_SOURCE_CONSUMER_ACK_20261118.md`).

## Confirmed PostgreSQL fields and projection
Source role table `family_roles`: `id BIGINT` → `sourceRoleId`; `family_id BIGINT` → `sourceWorkspaceId`; `code VARCHAR(50)` → `roleCode`; `system_role BOOLEAN` → `systemRoleAtSnapshot`. Source `name` is not required for identity or authority; omit it from the minimum portable proof unless Architecture decides otherwise. Source role permission assignments are **not** target security grants.

Source member table `family_members`: `id BIGINT` → `sourceMemberId`, `family_id BIGINT` → `sourceWorkspaceId`, `family_role_id BIGINT` → `sourceRoleId`, `member_type VARCHAR(30)` → `memberType`, `active BOOLEAN` → `activeAtSnapshot`, `deleted_at TIMESTAMP WITHOUT TIME ZONE` → `deletedAtSnapshot` (explicit nullable timestamp; no invented UTC offset). Soft-delete `name` mutates to redacted localized label; **do not** infer original identity from it. Recommended minimum portable catalog does **not** include names. If a display name is needed for user-review mapping, it should be a separate explicitly privacy-reviewed optional field, never a login matcher.

Source IDs are unique positive BIGINT **only within original signed graph**. The global-account-to-workspace link, OAuth subjects, password hashes, email, tokens, session keys, invitations and global permissions must never be dumped as identity-provenance side effects. An opaque account binding may be added only with separate Architecture+C approval and demonstrated need.

## Proposed unambiguous version transition
- Preserve existing `family-points-export-v1` as immutable legacy; current inspector may inspect it offline, but remains `restorable=false` when source original-membership proof is absent.
- Proposed *name* `family-points-export-v2` plus explicit portable `schemaVersion` change, with a shared version matrix across `ProtectiveBackupPortableFile`, `ProtectiveBackupArtifactCodec`, `WorkspacePortableBackupExporter`, `ProtectiveBackupImportInspector`. **The exact value is pending Architecture acceptance.**
- Add authenticated `SOURCE_FAMILY_ROLES` and `FAMILY_MEMBERS` as hidden dependency graph sources; do not change current 23-category v1 allowlist or silently add hidden rows to v1.
- Every member-referencing archived category must have a graph closure proof: unique member ID, own family role, original family ID, full historical tombstone and all optional/mandatory FK edges; any unknown or missing source parent is a deterministic blocker.
- Source catalog participates in the same repeatable-read MVCC transaction, source epoch and archive digest, not a second asynchronous read. Missing source DB during import must not require a remote lookup.
- `TASK_EXECUTIONS` must preserve executor `member_id` separately from `original_assigned_member_id` (nullable) and historical snapshot fields enumerated in Backend B ACK. TASK ledger `source_id` is historical execution ID.
- Physical file size, category row limits and media bytes remain hard fail-closed gates; partial exports must never pass inspect.
- Source-account identity is not target owner proof. C decides fresh actual owner authentication and target mapping grant; B consumes those permits, D validates economic source histories.

## Exact requested decisions
| Gate | Question | Default recommendation | Consumer |
|---|---|---|---|
| A4-01 | Signed role and member categories | `SOURCE_FAMILY_ROLES` + mandatory hidden `FAMILY_MEMBERS` | B, D, Architecture |
| A4-02 | Member display name privacy | Omit from minimum; optional separate consent-reviewed mapping UI data | C, B, Architecture |
| A4-03 | Account linkage | No global auth identifiers or account metadata | C, B |
| A4-04 | Tombstone timestamp encoding | Explicit nullable local timestamp (as stored), no invented timezone | B, QA |
| A4-05 | Exact v2 format marker and manifest schemaVersion | Version pair approved and immutable; legacy remains non-restorable | B, Release, Architecture |
| A4-06 | Hidden category membership / FULL vs SELECTIVE closure | Mandatory source dependency for any selected member FK | B, D |
| A4-07 | Financial/Task historical ancestry & media closure | D and B signed source field requirements before exporter vNext | D, B |
| A4-08 | Missing source DB owner proof | C owner-auth, independent of archive source IDs; no guess-based identity | C, B |
| A4-09 | Shared IMP entry | Architecture assigns impact identifier + review path before wire change | Architecture |

## Implemented test-only proof (not archive format)
- `DisposablePortableMemberSourceCatalog`: scoped `family_roles` + `family_members`, same active REPEATABLE_READ Spring transaction, positive IDs and member/role family matching, redacted projection; no global account or credential columns.
- `DisposablePortableMemberSourceCatalogPostgresTest`: actual PG18 + Flyway, two families, parent + child tombstone timestamp fidelity, no neighbor data/private names, denied weak/missing transaction, cross-tenant role FK rejected instead of silently dropped.
- This is deliberately test-only until explicit Architecture field/wire approval, no production exporter/inspector/manifest change.
- Retention B-A-03k fault-injection has been submitted for QA; no production quarantine/purge or Restore unlock.

## QA / acceptance
```bash
./mvnw -Dtest=DisposablePortableMemberSourceCatalogPostgresTest,WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupReconciliationRacePostgresTest,WorkspaceBackupRegistryPresenceRacePostgresTest test
```
Need a **non-skipped** Java21 + Testcontainers job on exact head SHA (ideally full Web/Android/Compose) before claiming tests passed. A workflow marked SUCCESS while Maven steps are skipped is only scope/routing success, not backend test acceptance. QA independently audits this package.

**No production change**: `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423 and BETA HOLD.
