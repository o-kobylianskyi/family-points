# Backend B / IMP-01a — REAL sealed PostgreSQL source ancestry → offline archive inspector

**2026-11-18. Owner:** Backend B. **Product status:** PARTIAL PRODUCER/INSPECTOR CHECKPOINT, NOT FULL RESTORE. Architect IMP-01 approved implementation direction in Issue #15 comment `6975893637`. No production RESTORE/RESET enable, live family data, public HTTP endpoint or DB migration.

## Accepted implementation items — one large package, independent atomic commits

| ID | Module/path | Result | Checkpoint/acceptance | Blast radius/revert |
|---|---|---|---|---|
| INT-01a-01 | `service/PortableManifestRootV2.java` @a783ba39 | Actual canonical binary manifest root encoder: domain `family-points:portable:manifest-root:v2`, big-endian source family/schema token/snapshot UUID/source epoch/sorted code/count/raw 32-byte category and parent-closure SHA256 | `PortableManifestRootV2Test` @f3c92331: independently calculated Python/struct SHA `d0dc0fd28e2d1defc0a9d13c1092416c0f9c3d166b48438ba17df6b3d0298029`, mutated bytes, epoch, source and category counts fail parity | Separate new file; rollback class and test only |
| INT-01a-02 | `service/PortableMemberAncestryV2.java` @0868e350, @8233dcaf/@edf24aef | Minimal source category bytes: JSON UTF-8 sorted object keys, role/member rows ascending original ID, explicit nullable tombstone. Category dependency for MEMBER maps original member ID→original role ID; SHA256 over category-scoped dependency domain. Strict same family, valid role/type, uniqueness, row bounds, no arbitrary extra fields. Root recalculated independently | `PortableMemberAncestryV2Test` @99dd0061/@c55bca00: independent category+dependency+root golden vectors; foreign role, missing tombstone rejected | Separate new file; no shared migrations |
| INT-01a-03 | `service/WorkspacePortableAncestryV2Exporter.java` @f3429291 | **Real JDBC PG18 MVCC read**, pin `REPEATABLE_READ`, original `family_roles`/left-joined `family_members` under one source epoch, include active and soft-deleted, never include name, email, login/provider secrets. Emit new `family-points-export-v2` ancestry-only `categories` plus authenticated manifestRootDigest inside plaintext and AES-GCM FPB1 envelope. Independent `sourceDigest` domain-scoped, no alias of old v1 `sourceDigest` to manifest root | `WorkspacePortableAncestryV2SourceLossPostgresTest` @e69ba876/@e21af51d builds two-family disposable PG18, writes active+deleted, exports with pinned tx, then **destroys** source container; encrypted archive inspect only, wrong tenant/key/ciphertext, stale root after re-sealing and private-string inspection | New opt-in service, NOT replacing legacy exporter, no HTTP changes; revert file independently |
| INT-01a-04 | Existing `ProtectiveBackupImportInspector.java` @c86841ea | Adds **new** `inspectAncestryV2` method; decode FPB1, authenticate AES256-GCM by `codec.open`, validate exact v2 schema/source family/epoch, recalculate category/parent/root SHA and cross-check authenticated manifest source digest; return root and counts, **restorable=false**. Existing v1 `inspect()` semantics unchanged | PG18 source-loss test + malformed/wrong scope and root negatives | One isolated method in inspector; removal does not affect legacy parsing |
| INT-01a-05 | GitHub full verification | Requested below; **Maven/JUnit and actual exact-SHA PASS not yet observed for these new commits** | New immutable all-four full CI plus targeted JDK21+Docker PG18 suite | No runtime feature gate changes |

### Full SHA-256 portable source-vector details
- Protocol schema marker is exact `family-points-export-v2`; earlier synthetic C token `VNext-2` is not used.
- `PortableMemberAncestryV2` category content is canonical UTF-8 JSON with recursively key-sorted object attributes and rows in ascending original numeric ID order. Dependency digest input is UTF-8 literal `family-points:portable:member-deps:v2|<CATEGORY>|<sorted member:role pairs>`. Roles category has no parent pairs in first bounded slice. In independently computed fixture (family 17/epoch 7/snapshot `11111111-2222-4333-8444-555555555555`):
  - FAMILY_ROLES content SHA: `0eb20ca0b2e56292e3a879d0c04513322f342be08f1ed14d13bc2b4dfafe81bb`.
  - FAMILY_MEMBERS dependency SHA: `321a6081a6f7dbd7645bd1d7f25bcb13bbb6a8a8e8dd8357c2a9625b0dd3f041`.
  - manifestRootDigest: `dcda588e22db2d804f2b68e4de43bf5e3051a45a7bd68170f673c29dce7e87c1`.
- Source FPB1 AES-GCM AAD from existing `ProtectiveBackupArtifactCodec` binds `snapshotId`, source family, operationId, dataEpoch, independent sourceDigest, exact schemaVersion, creation timestamp, plaintext contentSHA256 and bytes. `manifestRootDigest` lives **within authenticated plaintext**, recomputed by inspector from authenticated category bytes. No trust in client-declared root.
- **Limited scope:** exporter includes ONLY source roles and members, not historical TaskInstance, event graph, media or full finance. Consequently `inspectAncestryV2.restorable=false`; no automatic mapping, role creation or account-link grant. External `WorkspaceRestorePermitScopeV2` binder and real C RESTORE permit are separate future sequential checkpoints.
- Signed-source description means **AES-GCM-authenticated encrypted archive under independent external key**, not an asymmetric signature or verified owner. Original role records are historical provenance only.

## QA execution commands
Run pinned current SHA with JDK21+Docker PostgreSQL18:
```powershell
mvn -Dtest=PortableManifestRootV2Test,PortableMemberAncestryV2Test,WorkspacePortableAncestryV2SourceLossPostgresTest,ProtectiveBackupImportInspectorTest,WorkspacePortableBackupConcurrentSnapshotPostgresTest test
```
If some prior test-class name differs, only omit non-existent name after checking actual sources; do not skip new three tests. Then run **real all-four [full-ci]** Java PG18, Web, Android, Compose, exact-SHA evidence and Release gates on **same immutable SHA**. Report tests/failures/errors/skips and first stack. Old full green [#44282671397](https://github.com/o-kobylianskyi/family-points/actions/runs/44282671397) @92e624e5 = **728/728** all-four PASS **before this packet**, NOT evidence of new versioned code.

## IMP-01 dependent next checkpoint
Extend same real exporter+inspector schema with complete reviewed TaskInstance/ledger/finance source identity and all selected FK/category graph only after A's 17 unreviewed member FKs are dispositioned (include/dependency-only/exclude-with-reason). Reuse exact C canonical binary `WorkspaceRestorePermitScopeV2`, receiving **verified** root/epoch from inspector; no B legacy pipe hashes, no new permit/migration, no enabled Restore. Architect reviews planned additive wire shape and source-loss true full-graph proof before upgrading `restorable=false`.

**Safety:** no production Restore or Reset Execute; HTTP423; BETA HOLD. No automated destructive backup retention (physical unlink before DB commit remains P0).
