# B-INT-01a — Architecture decision gate for first real authenticated vNext archive

**Owner:** Backend B (sole primary integrator). **2026-11-18.** This is an implementation blocker/decision record, **not** approval, a runtime producer, or a ready Restore. No production file/Flyway/DTO modified.

## Verified actual code
- `WorkspacePortableBackupExporter` currently emits `family-points-export-v1` for 23 allowed categories. It does NOT select original `FAMILY_MEMBERS` or `FAMILY_ROLES` and cannot prove original identity/role history after source database is lost.
- `ProtectiveBackupPortableFile` FPB1 version 1 contains sealed manifest fields: `snapshotId`, `workspaceId`, `operationId`, `dataEpoch`, `sourceDigest`, `schemaVersion`, `createdAt`, `contentSha256`, `contentBytes` + ciphertext. Its decoder alone is unauthenticated.
- `ProtectiveBackupArtifactCodec.open` authenticates these manifest fields using AES-256-GCM AAD and validates plaintext SHA-256. `ProtectiveBackupImportInspector` checks the decrypted legacy payload and deliberately keeps `restorable=false`.
- C-owned canonical binary V2 `WorkspaceRestorePermitScopeV2.SourceArchive` separately contains `sourceWorkspaceId`, `schemaVersion`, `snapshotId`, `sourceSnapshotEpoch`, `snapshotDigest`, `manifestRootDigest`. **No approved mapping of separate `snapshotDigest` and `manifestRootDigest` to existing FPB1 fields exists.** B's old pipe-delimited SHA tests cannot substitute C canonical hashes.
- A's `DisposablePortableMemberSourceCatalog` and `DisposablePortableMemberReferenceClosure` implement **test-only** source role+member MVCC ancestry and 9 reviewed category edge groups; no signed producer/inspector. `DisposablePortableMemberFkInventoryPostgresTest` discovers additional unreviewed physical member FKs. A reader must not implicitly export auth/account/session rows.

## Mandatory single-IMP decisions (P0)
1. **One versioned sealed wire payload**: finalized schemaVersion value, explicit source FAMILY_ROLES and hidden FAMILY_MEMBERS, role/member/tombstone representation and explicit-null original assignee, snapshot integrity; old v1 inspectable read-only and non-restorable.
2. **Exactly one hash contract**: domain-separated deterministic binary/canonical category digest, closure digest and manifestRootDigest; define source snapshotDigest vs manifestRootDigest, whether GCM-authenticated plaintext byte hash or a new root, envelope binding and source epoch. No invented alias or dual text/binary acceptance. Independently recompute all hashes after `codec.open` from authenticated bytes, never trust caller-supplied values.
3. **Physical member FK coverage disposition**: each unreviewed FK/table classified `PORTABLE_SOURCE_DEPENDENCY`, `EXCLUDED_AUTH_OR_ACCOUNT`, `EXCLUDED_NONPORTABLE_WITH_BLOCKER`, or `REQUIRES_ARCHITECTURE_REVIEW`. Cannot mark `FAMILY_MEMBERS` closure complete until every original-member reference in selected categories is proven; optional FK NULL is not an omitted reference.
4. **Privacy/security boundary**: exclude password hashes, raw emails, provider subject IDs, user sessions/JWT, OAuth tokens and account login identities from source package. Original member/role are historical graph nodes, NEVER authorize target login/owner.
5. **FULL/SELECTIVE closure**: approve category dependency policy including group memberships, task delegation/exception periods, reward requests+obligations, media actor references, point/reputation/group ledgers and exchange/refund; deterministic BLOCK for unsupported source types.
6. **Atomic source capture**: exact `REPEATABLE_READ` single MVCC source view and stable epoch/limits across roles+members+task/reward/finance+encrypted plaintext; source DB is then disposable and can be destroyed before offline verification.
7. **Ownership/checkpoints**: B edits producer then inspector then C consumer *sequentially*; A may independently finish FK inventory only, without simultaneously changing shared exporter/codec. Architecture approves final shared hash/format and impact record before schema changes.

## Concrete acceptance and rollback
- INT-01a.1: approved wire source capture; two real PG18 families, active/deleted members, role family cross-tenant denial and no private auth fields; independent migration/legacy tests. Revert only producer change if red.
- INT-01a.2: offline AES-256-GCM envelope open from archived ciphertext without source PostgreSQL, independent root/category hashes and fail closed on byte/metadata/parent manipulation. Revert inspector stage only; preserve earlier producer checkpoint.
- INT-01a.3: reuse existing C **binary V2** `canonicalMappingDigest`/`canonicalSelectionClosureDigest`/scope, binding **authenticated** root+source epoch+family. This remains preflight, not an issued one-use owner permit; no Restore Execute or production target writes.
- After independent cheap checkpoints, QA must certify one immutable SHA: JDK21 full non-skipped Maven/PG18, Web, Android, Compose, exact SHA attestation and the authenticated source DB loss test. No broad new synthetic matrix.

## Confirmed historical CI for context only
[Full #44281571865](https://github.com/o-kobylianskyi/family-points/actions/runs/44281571865) @`3d1402570ec31cdcc1d8f82c7f54181dfd3afc9e` **726/726 Maven PASS 0F/0E/0S**, Web Android Compose and exact SHA PASS. Later main commits require fresh pin, and no CI outcome proves signed vNext source after source database loss.

**Decision asked:** Architecture issue a single explicit signed-off IMP with fields/hash bytes/FK dispositions and exclusive producer/inspector owner. Without it, code that claims to authenticate/restorable vNext is a security downgrade and must not land.

**Release gates:** production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
