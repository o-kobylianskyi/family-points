# Backend C → Backend B / Architecture — C-C-09 revised restore permit v2

**2026-11-18 · Status:** C response to B's **CONDITIONAL ACK** (`BACKEND_B_TO_C_RESTORE_PERMIT_CONSUMER_ACK_20261118.md`). SOURCE COMMITTED. Awaiting final **B field-level ACK and Architecture IMP** before shared Flyway/HTTP/consumer integration. No production grant, Reset/Restore still disabled.

## Reviewed new independent source

1. `security/WorkspaceRestorePermitScopeV2.java` @ `8345305b`: typed `SourceArchive` sourceWorkspaceId, schemaVersion, immutable snapshotId, **sourceSnapshotEpoch**, snapshotDigest, manifestRootDigest; typed `RestoreScope` targetWorkspaceId, authorized owner global account/member IDs, operationId, authenticated source archive, selectionClosureDigest, memberMappingDigest, **targetExpectedEpoch**, issue/expiry <=5min. Domain-separated binary canonicalScopeDigest SHA-256 includes **all** fields, no names or JSON order. 7 JUnit methods @ `2ce5003b`.
2. `security/WorkspaceRestoreSelectionClosureV2.java` @ `598b26c3`: category selection contract `categoryCode`, decision **RESTORE/PRESERVE/BLOCK**, source row count, source category digest, dependency closure digest, target conflict-plan digest; sorted lexicographically, duplicates rejected, domain separated, schema+source/target family identity; 5 JUnit methods @ `06b73c4f`.
3. `WorkspaceRestorePermitScopeV2.canonicalMappingDigest` sorts unique positive original source-member IDs; rejects duplicate target member IDs/noninjective mapping, zero/null family/IDs; binds source+target family and cardinality, versioned domain. B still must validate source IDs against A's signed original member roster and target IDs against active intended target membership; digest alone cannot establish identity.

## Exact canonical encoding (proposal for consumer ACK)
- UTF-8 domain/version strings prefixed with **big-endian signed int32 byte length**. No normalization or implicit JSON serialization; schemaVersion restricted to `[A-Za-z0-9._-]{1,40}`.
- All numeric family/member IDs and epochs use **big-endian signed int64** with positive IDs, nonnegative epochs.
- UUID: 16 bytes, big-endian mostSignificantBits then leastSignificantBits.
- SHA-256 digests: exactly 32 bytes decoded from lowercase 64-character hex.
- Member mapping: `domain "family-points:restore:member-map:v2"`, source family int64, target family int64, number of pairs int32, pairs sorted ascending by source int64; each source and target int64; injectivity required.
- Selection closure: `domain "family-points:restore:selection-closure:v2"`, schemaVersion, source/target family int64, number of categories int32, sorted categoryCode strings (UTF-8 length prefixed), decision enum text, sourceRowCount int64, sourceCategoryDigest, dependencyClosureDigest, targetConflictPlanDigest as 32-byte hashes.
- Overall RESTORE permit scope: `domain "family-points:restore:scope:v2"`, targetWorkspaceId, authorizedOwnerAccountId, authorizedOwnerMemberId, operation UUID, sourceWorkspaceId, schemaVersion, snapshot UUID, sourceSnapshotEpoch, source snapshotDigest, manifestRootDigest, selectionClosureDigest, memberMappingDigest, targetExpectedEpoch, issue and expiry instants as epochSecond int64+nano int32. SHA-256 of byte stream. A/B must guarantee manifest canonical digest is authenticated, not a client-supplied arbitrary text.

## Security and integration boundaries

**Not solved by the pure classes:** true original owner enrollment, fresh password/provider reauth, server-side store/migrations, authenticated source archive, exact category closure verification, financial parity, durable issuance and receipt. All require separate production development.

Before producing real migration/DTO:
- **B** ACK exact field names, version marker, mapping/selection hash encoding, definition of source archive snapshot digest and manifestRootDigest and source epoch, category-completeness/exclusion semantics, receipt and operation-row lock order.
- **A** ACK authentic signed manifest contains sourceWorkspaceId/schemaVersion/snapshotId/source epoch/sealed root.
- **D** ACK source row/ledger graph financial closure digest inputs.
- **Architecture** assign IMP, Flyway/version, registration writer file ownership and legacy owner evidence enrollment; no guessed admin.
- **QA** run `mvn -Dtest=WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test,WorkspaceDestructivePermitContractTest,DisposableOwnerPermitStorePostgresTest,DisposableOriginalOwnerBindingPostgresTest,WorkspaceResetProofConcurrencyPostgresTest test` on current exact SHA, then non-skipped four-component full CI.

**Known QA correction:** `WorkspaceResetProofConcurrencyPostgresTest` fixture status `CONFIRMED` required `confirmed_at` under V41; QA fixed @ `cee9f3f3`. Other owners' Testcontainers generic syntax compile errors are being fixed. No current HEAD green may be inferred without exact run.

## Review questions
- Does B accept `sourceSnapshotEpoch` and `targetExpectedEpoch` split, stable hashing format and target-family scoped operation UUID?
- Will B provide the immutable receipt schema and source-member-to-target-member pairing **review evidence digest** (separate from injective map) for permit issuance? Current map digest proves values/ordering only, not that owner reviewed them.
- Should selection block incomplete historical ledger graphs before issuance rather than marking `BLOCK` inside an otherwise eligible selection? C recommends gate before reauth grant issuance.
- Is 100,000 member-pair cap and 10,000 category cap acceptable, or should declared manifest quotas be tighter?

**Guardrails:** Reset Execute HTTP 423; RESTORE production `restorable=false`, `replaySupported=false`; BETA HOLD; no real family modifications.
