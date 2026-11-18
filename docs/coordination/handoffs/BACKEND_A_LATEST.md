# FamilyPoints — Backend A ownership and first source-contract audit

**Date:** 2026-11-18. **Track:** Backend A — Backup & Portable Archive. **Requested by project owner.**
**Baseline inspected:** main at `74b0b37bc1ebbaa301430c09108e3bc91814c8b9` (check HEAD before each edit). **Status:** SOURCE REVIEW / CONTRACT GAP; not CI verified.

## Ownership boundary
- Backend A owns source-side capture/export; selected FULL/SELECTIVE category closure; protected family-scoped snapshot; stable source metadata and digest; encrypted portable file/manifest/versioning/retention; offline producer-side validation; archival privacy and historical FAMILY_MEMBERS contract after Architecture approval.
- Backend B owns consumer-side restore engine, original→target ID maps, durable authorization, atomic SQL replay/rollback, idempotency/receipts and disposable target tests. Its handoff is `docs/coordination/handoffs/BACKEND_B_LATEST.md`.
- Shared producer↔consumer contract requires explicit Backend B ACK and Architecture approval before any schema/category breaking change. No unilateral modification of Backend B disposable restore executors.
- Legacy `BACKEND_LATEST.md` and `logs/backend.md` remain historical; new work should use distinct `B-A-XX` identifiers.

## B-A-01 — evidence-based source audit (completed review only)
Source files reviewed:
1. `WorkspacePortableBackupExporter.java`: SCOPED_TABLES currently enumerates 23 categories, includes POINT_TYPES, POINT_EXCHANGE_RATES and POINT_EXCHANGE_REQUESTS but **no FAMILY_MEMBERS**. It exports `family-points-export-v1`, family/workspace ID and selected category arrays, validates dataEpoch and source snapshot digest, uses explicit fixed SQL scope, MAX_ROWS 2000 and MAX_BYTES 12 MiB. File header expressly states media blobs need separate handling, and there is no VERIFIED promotion.
2. `ProtectiveBackupImportInspector.java`: fixed matching 23-category allowlist. `checkMemberParent` explicitly marks every present positive member_id as `IDENTITY_REBIND_REQUIRED`, because member provenance is intentionally missing. Inspector is offline-only and cannot make archive restorable.
3. `ProtectiveBackupArtifactCodec.java`: AES-sealed file manifest includes snapshotId, workspaceId, operationId, dataEpoch, sourceDigest, schemaVersion, timestamp, plaintext hash and length. Existing crypto/manifest compatibility must be preserved.
4. Backend B handoff at `74b0b37`: original member catalog/historical membership source provenance is P0 blocker, and current archive does not prove portable identities after complete source DB loss.

**Conclusion:** Adding a literal `FAMILY_MEMBERS` entry to the exporter alone is NOT a safe solution: archive version/allowlist, source digest/snapshot closure, privacy, member account roles and consumer verification must be updated together. Historical membership and source ID ancestry must be signed by archive, not inferred from target ID/name/role.

## Producer↔consumer contract questions (Architecture approval required)
- Define minimal portable member representation: stable source member identifier and family ancestry; distinguish account-global identity from workspace membership; privacy-minimized identity fields; role/status at snapshot; historical/removed member references without revealing secret auth attributes.
- Define deleted/merged member semantics and all member-reference columns (mandatory/optional) across archived categories. Original member_id must not be treated as target id.
- Decide whether `FAMILY_MEMBERS` is a mandatory hidden dependency for every scope with member references, a selectable visible category, or both (scope semantics and import blockers must be explicit).
- Specify archive schema versioning: keep old v1 readable as non-restorable where necessary, introduce new version only after Backend B consumer agreement; AES manifest schemaVersion and JSON format are separate version fields and must not silently diverge.
- Define source atomicity under concurrent writes and dataEpoch changes: selected-scope digest plus all categories should refer to one consistent database snapshot. Current `@Transactional` alone does not prove repeatable read/snapshot isolation.
- Agree explicit size/row limits and media binary handling; never silently truncate or omit data; reject over-limit with stable errors.
- Define QA negative cases: malicious altered encrypted file, cross-tenant member, removed historical member, missing/duplicate parent, target ID collision, same account in multiple families, concurrent change during snapshot, partial category subset, old archive compatibility, missing media content.

## Proposed ordered work
- **B-A-02 (next):** prepare precise `FAMILY_MEMBERS` schema/column/foreign-key inventory from Flyway + entity + service; propose privacy-minimized vNext archive contract to Architecture and Backend B. No production member export before approval.
- **B-A-03:** implement approved source category + digest + inspector checks, schema compatibility and encrypted immutable member ancestry. Add unit and two-tenant PostgreSQL integration cases.
- **B-A-04:** complete category-closure and media/financial dependency export tests, inspect partial FULL/SELECTIVE semantics and limit handling.
- **B-A-05:** evaluate transactional export consistency, source digest/epoch races, retention and encryption boundary; regression suite and exact-SHA CI evidence.

## Safety and delivery state
- **Implemented in this package:** ownership handoff + source audit documentation only. **No Java or production mutation. No new tests executed.**
- **Not implemented/accepted:** original FAMILY_MEMBERS archive, vNext contract, producer↔consumer ACK, independent exact-SHA full CI.
- **Release gates unchanged:** Restore Execute production blocked (`restorable=false`, `replaySupported=false`), Reset Execute HTTP 423, BETA HOLD. Synthetic/disposable data only; no real family reset.
- **Coordination:** Backend B handoff, Architecture `logs/architecture.md` and `IMPACT_REGISTER.md`, QA issue #15. Send precise contract proposal before changing shared archive format.


## B-A-02 completed — 2026-11-18 (documentation and source-schema evidence)
- Verified V1 family_members fields and WorkspaceMember tombstone behavior; V21/V22 global account→workspace membership and DB scope trigger; V29 optional legacy account pointer/external identity separation; V31 shared email collision; GroupMembership is separate from account membership.
- Published producer contract proposal: `docs/coordination/requests/B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md` (commit `1ede3ee`). Includes minimum privacy-safe source node, original FK/role ancestry, soft-delete, account link risk, versioned wire format, legacy compatibility and 12 negative cases.
- NO runtime Java/schema changes or tests; awaiting Architecture and Backend B ACK before source format modifications.
- Next B-A-03: only once approved, implement archive-category/digest/inspector updates with PG18 two-tenant tests; meanwhile independently audit source snapshot isolation/limit behavior without changing restore execution.


## B-A-03a — 2026-11-18 export snapshot isolation safeguard
- Verified exporter uses first digest, per-table rows and final digest while PostgreSQL default READ_COMMITTED may observe multiple MVCC statement snapshots. Family-row lock only fences cooperative writers.
- Source change `a9c9b667`: exporter `@Transactional(isolation = Isolation.REPEATABLE_READ)` when initiating its own Spring-proxied transaction; no archive format/restore changes.
- Test source `b49191ed`: transaction declaration regression, **NOT_RUN** in this track. QA packet `docs/coordination/requests/B-A-03A_SOURCE_SNAPSHOT_ISOLATION_QA_20261118.md` commit `a52a277`.
- **Remaining P0 hazard:** if REQUIRED exporter joins caller's READ_COMMITTED transaction, the annotation does not raise isolation; current direct-instance tests also do not test proxy. Must review actual callsites and add true PG18 concurrent integration before asserting atomic capture.
- B-A-02 FAMILY_MEMBERS wire contract still awaits Architecture / Backend B ACK; do not merge a guessed category or turn on Restore Execute.
- Next: B-A-03b callsite/outer-transaction inventory and fail-closed production isolation boundary, plus controlled PG18 concurrency test.
