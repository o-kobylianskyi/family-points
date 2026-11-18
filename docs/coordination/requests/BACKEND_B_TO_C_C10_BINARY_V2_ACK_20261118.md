# Backend B → Backend C / Architecture — B-B-08 response to C-C-10 V2 hash interoperability

**Date:** 2026-11-18. **Decision:** **CONDITIONAL FIELD-LEVEL ACK of binary V2 as sole shared RESTORE wire candidate; REJECT treating older B-B-07 string hashes as C V2 values.** Architecture IMP, A/D manifest/finance fields and authentic owner proof remain pending. No production authorization.

## Precise source-verified decision
Reviewed runtime `WorkspaceRestorePermitScopeV2`, `WorkspaceRestoreSelectionClosureV2`, C request `BACKEND_C_C10_V2_INTEROPERABILITY_GATE_20261118.md` and its independently computed golden vectors:
- Mapping canonical SHA256 typed binary / `family-points:restore:member-map:v2`: source/target family int64 BE, pair count int32 BE, source-ascending ID pairs int64 BE, injective.
- Selection closure canonical SHA256 typed binary / `family-points:restore:selection-closure:v2`: schema version, source+target, sorted category + RESTORE/PRESERVE/BLOCK decision, source row count, category content/parent-closure/target-conflict SHA256 bytes.
- Overall C scope canonical SHA256 typed binary / `family-points:restore:scope:v2`: target owner identity+operation, sourceWorkspaceId, exact source schemaVersion, snapshotId, sourceSnapshotEpoch, snapshotDigest, manifestRootDigest, mapping+selection digests, expected target data_epoch, issued/expires nanos.
- Golden mapping: `690dafc3d70b893b8b394827044692b26f55b8dcee773d52e510d892c1bca8fc`; selection: `b008109bcb80fde5728b36393827c5afafc3780c0d8eb3616d0a4289acf0202b`; scope: `17f6d74e1eb83f826b455d81b2cbcf8b896ab9af1d639305ff3ed5e4bf916979`.
- The older `DisposableRestorePlanFingerprint` uses string `FP-RESTORE-*-V2` domain hashes **which differ** from C's binary V2. It is retained for historical synthetic QA only, NOT a portable grant or a canonical wire format. Never compare those strings to C's RESTORE mapping/selection digest.

## Backend B test-only implementation
- `DisposableRestoreScopeV2Consumer` uses **actual C methods** `WorkspaceRestorePermitScopeV2.canonicalMappingDigest`, `WorkspaceRestoreSelectionClosureV2.canonicalDigest` and `canonicalScopeDigest`; checks source workspace, proposed `VNext-2` marker, snapshot UUID, source snapshot epoch, manifest root digest, target workspace+epoch and exact C-issued-vs-attempted immutable scope including TTL. No second hash implementation.
- `DisposableRestoreRowClosureVerifier` validates a bounded original-family FAMILY_MEMBERS + TASK/REWARD/PURCHASE/POINT ledger source graph including tombstoned original members, exact original roster→explicit target mapping and hard denies foreign/orphan categories and unsupported REFUND/EXCHANGE.
- `DisposableRestoreVerifiedGraphGate` combines caller-supplied C scope, category counts/decisions, exact original member map and bounded per-row finance ancestry before any target writes; refuses PRESERVE/BLOCK on needed categories in the bounded FULL graph. The supplied category source digests **are not independently checked against authenticated A ciphertext/manifest**, so this is NOT a signed-source proof.

## Required final IMP clarifications
1. **A:** `VNext-2` here is a TEST marker copied from C golden vectors, NOT an approved real on-wire archive version. Define exact schemaVersion token in authenticated A manifest and expected owner reviewed source row/category digest canonicalization.
2. **C:** separate owner review evidence (who saw and approved which original→target mapping) from injective mapping digest. No caller-constructed `RestoreScope` can grant access. Real issuer and token/nonce durable owner session required. Receipt has unique operation/action/target/scope hash, before/after epoch, status and immutable completed source/target counts; no replay after lost POST.
3. **D:** source finance root hashes, all ledger source/parent economic parity (including refund, exchange, group and reputation), and target reconciler callback; net balance alone insufficient.
4. **B:** after IMP, one target family row lock first, C permit lock second, preflight / native category writes / epoch bump / immutable receipt in ONE transaction. Failed work and grant consumption both roll back. Never substitute separate synthetic plan transaction for real finance transaction.
5. **Architecture:** assign shared byte contract, C grant lifecycle and Flyway/file ownership; prove archive full source-DB loss and verified original owner/target identity. Until then cross-package contracts are design acceptance only.

## QA exact same immutable SHA
```powershell
mvn -Dtest=DisposableRestoreScopeV2ConsumerTest,DisposableRestoreRowClosureVerifierTest,DisposableRestoreVerifiedGraphGateTest,WorkspaceRestorePermitCanonicalVectorsTest,WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test test
```
Plus B-B-06 PG18 regression, D finance PG18, C owner permit PG18 and full four-component CI/Release attestation on one SHA; report JDK21 tests/fails/errors/skips and original run URL.

**No real source archive signature validated, no production RESTORE, no true C grant consumed, no production FK writes, no rollout.** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
