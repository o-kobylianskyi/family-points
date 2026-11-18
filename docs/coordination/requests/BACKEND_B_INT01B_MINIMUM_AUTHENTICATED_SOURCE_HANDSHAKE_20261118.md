# Backend B — B-INT-01b minimum authenticated source handshake (Architecture IMP decision)

**2026-11-18 · Primary implementer: Backend B. Decision request, not production sign-off.**
**Baseline:** [Full CI #44281571865](https://github.com/o-kobylianskyi/family-points/actions/runs/44281571865), immutable SHA `3d1402570ec31cdcc1d8f82c7f54181dfd3afc9e`, Maven **726/726 (0 failures, errors, skips)**, Web/Android/Compose and exact-SHA attestation **all SUCCESS**. Independently inspected Maven job `134723393239` and full evidence job `134726484177`. An earlier second full green #44281496203 @`45c7ff9a` also gives 726/726. Newer commits are not certified by these exact SHA runs.

## 1. Exact existing runtime contract, inspected in source
- `ProtectiveBackupPortableFile.decode` reads size-bounded `FPB1` version-1 binary envelope with manifest: `snapshotId`, `workspaceId`, `operationId`, `dataEpoch`, `sourceDigest`, `schemaVersion`, `createdAt`, `contentSha256`, `contentBytes`, ciphertext. **Parsing alone authenticates nothing.**
- `ProtectiveBackupArtifactCodec.open` binds these exact manifest fields as AES-256-GCM associated data, authenticates encrypted plaintext and checks content byte length/SHA256. It DOES NOT independently prove original owner, target member map approval or a restored target epoch.
- `WorkspacePortableBackupExporter` currently writes `format=family-points-export-v1` with up to 23 scoped category tables. **It excludes `FAMILY_MEMBERS`, `FAMILY_ROLES` and complete authenticated historical source-role/TaskInstance identity provenance.** Some selected FK tables are scoped via member joins; source member tombstones are not independently retained in portable archive.
- `ProtectiveBackupImportInspector` rejects unsupported schema/categories and returns read-only `Inspection(..., restorable=false, ...)`; it is NOT a Restore executor or a verified-owner authority.
- `WorkspaceRestorePermitScopeV2.SourceArchive` requires **separate** sourceWorkspaceId, exact `schemaVersion`, snapshot UUID, sourceSnapshotEpoch, `snapshotDigest` and `manifestRootDigest`. There is **no existing V50 `manifestRootDigest` field**; do not silently alias `contentSha256` or `sourceDigest` as this independent value without a reviewed cryptographic definition.
- A `DisposablePortableMemberSourceCatalog` probes member+role ancestry under pinned REPEATABLE_READ, **TEST ONLY**. It neither exports sealed signed vNext nor changes `FPB1` envelope. A source B-A-02 contract remains **PROPOSED**.

## 2. Minimal ONE producer→inspector→consumer integration contract for approval
| Boundary | Input/evidence | Strict validation | Writer and checkpoint |
|---|---|---|---|
| Source MVCC selection | true source workspace, pinned snapshot UUID, original data_epoch, role and member catalogs incl tombstones, historical task/ledger FKs | one REPEATABLE_READ view; no filtered-out orphan, no credential/PII export, bounded rows/bytes | B primary, A optional isolated reader |
| Versioned sealed archive | explicit new content `schemaVersion` token approved by Architecture; `FAMILY_ROLES` and hidden `FAMILY_MEMBERS` original sources; all selected parent rows | deterministic source role/member IDs and tenant ancestry; explicit null semantics for historical fields; no source auth identities | B after single wire IMP |
| Authenticated manifest | category codes, row counts, immutable category content SHA256, dependency-closure SHA256, snapshot-level root SHA256 with defined bytes/encoding/domain | root must authenticate the **same** category bytes and source epoch as AES-GCM plaintext; fail closed on missing and duplicate category; source DB can be fully unavailable | B exporter and offline inspector together, separate verified checkpoint |
| RestoreScopeV2 | `SourceArchive` authenticated from archive, not client JSON; reviewed target family+owner, C binary mapping/selection canonical SHA256, target epoch | exact A manifest-root vs C source manifestRootDigest, C source digest and epoch; no B pipe-delimited hash acceptance | B one existing C V2 contract (no competing DTO) |
| Real owner permit | authoritative original-owner creator/verified legacy enrollment, genuinely fresh password/provider verification, server-issued one-use RESTORE-only grant | original archive member IDs are **never** login/owner credentials; permit binds signed source + reviewed mapping and selection | separate B security checkpoint after authenticated source verified |
| Native restore | selected full FK closure and D finance event/denomination/provenance parity | family lock → real permit lock → native target writes → epoch bump → immutable result receipt **one SQL transaction**, rollback/replay protection | B after prior gates; QA disposable source-loss proof |

## 3. Acceptance criteria for first executable source checkpoint
1. On two-family disposable PG18 and stable source epoch, produce `.fpbackup` with authenticated role and member rows (including soft-deleted), historical task/ledger/source references; source PostgreSQL can then be deleted.
2. Offline, with only ciphertext, independent external key and user-reviewed scope, reconstruct authenticated source workspace/epoch/snapshot/category content hashes/root; tampered bytes or swapped sealed fields fail GCM/root checks **before** any target SQL.
3. Capture same source family in concurrent changing transaction: consistent snapshot or explicit retry/conflict; never mixed old/new row lineage.
4. Legacy V50 still inspectable when supported but **not** restorable; no reinterpretation of missing member roster or root digest.
5. No password, email, provider subject, auth challenge/token, raw account profile, global membership or real authorization goes into archive. Target account binding is separate owner-approved operation.
6. SHA-256 canonical serialization has a single agreed definition; C canonical binary RestoreScopeV2 remains unmodified and keyed from authenticated producer evidence.
7. No production endpoint/feature flag, no data reset, no destructive retention, no partial verified status; `restorable=false`, `replaySupported=false`, HTTP423, BETA HOLD.

## 4. Small ordered commits/checkpoints, but one useful large delivery package
- **INT-01b.1 (producer source evidence):** add actual minimum role/member source catalog to exporter under approved version gate, keeping legacy behavior unchanged; verify exact PG18 two-family/tombstone and source-loss fixture.
- **INT-01b.2 (manifest and offline inspector):** compute authenticated immutable per-category digests/root, parse and verify through existing `ProtectiveBackupArtifactCodec.open` and `ProtectiveBackupPortableFile` with same envelope if reviewed; never bypass codec or trust caller-declared digest.
- **INT-01b.3 (B→C canonical binder):** reuse `WorkspaceRestorePermitScopeV2` + `WorkspaceRestoreSelectionClosureV2` to construct source-binding from *verified bytes*, still requiring separate C issuer/owner permit; check cross-source/cross-target, downgrade/replay and selected parent closure.
- **Independent lanes:** A isolated retention/read-only audit, QA full CI/fix single failing fixture and V49 Chromium isolation. No overlapping shared Flyway/API edits until versioned IMP accepted. Each checkpoint has targeted test then one pinned all-four QA, with isolated revert possible.

## 5. Explicit Architecture decisions required (single IMP, not duplicate parallel design)
- Exact wire schema marker (the string `VNext-2` in C tests is illustrative **not** an approved live archive version).
- Canonical bytes/domain for the separate `manifestRootDigest`, including whether source snapshot digest and old content digest are inputs or independent provenance, and how schema/category payload hashes are bound by AES AAD.
- Privacy and necessity of any opaque source-account linkage (default **none**), original role catalog representation and hidden original member category, tombstone/explicit-null semantics.
- FULL/SELECTIVE minimal parent-category closure and source financial/reputation/group, exchange/refund proof; which categories remain blocked versus restorable.
- Files/writer reviewer, migration need, legacy V50 read-only compatibility and QA immutable-SHA acceptance before any production code can assume restorable true.

**Status:** this contract is an implementation handoff with verified repo evidence, NOT a producer, not an issued permission, not a signed new archive. No new incompatible class/Flyway/API is created merely to force CI green.
