# Backend C C-C-10 → Architecture / Backend B — canonical V2 interoperability gate

**Date 2026-11-18. Status: FIELD-LEVEL REVISE/ACK REQUESTED. No shared migration/API/permit issuance authorization.** Reference: ADR-BKL002-ABCD-INTEGRATION-003 and Architecture Checkpoint 04.

## Critical source inspection: B vs C fingerprint mismatch

**C V2:** `WorkspaceRestorePermitScopeV2` @8345305b and `WorkspaceRestoreSelectionClosureV2` @598b26c3 encode domain-separated **binary typed** fields (length-prefixed UTF8, 64-bit IDs/epochs, UUID bytes, hash bytes) and SHA-256. They bind source schema/version, manifestRootDigest, source epoch, target expected epoch, owner and reviewed mapping/selection.

**B B-B-07:** `src/test/java/com/olehkobylianskyi/familypoints/DisposableRestorePlanFingerprint.java` currently uses **UTF-8 pipe-delimited text strings** for mapping, selection and plan digests: `FP-RESTORE-MEMBERS-V2`, `FP-RESTORE-SELECTION-V2`, `FP-RESTORE-PLAN-V2`. Its category list has code/count/boolean and conflict policy `FAIL_ON_CONFLICT`, but does **not** carry C's source-category content digest, dependency closure digest, target conflict-plan digest, source archive schemaVersion text, target owner account/member and sealed manifestRootDigest as distinct canonical binary scope fields.

Therefore C and B hashes **are NOT interoperable** today even when semantically describing the same plan. This is an explicit **P0 integration blocker**, not a problem to solve with weaker string comparison or accepting two hash formats silently. Both current algorithms remain test-only descriptors, neither a real grant.

## Exact independent C V2 golden vectors

`WorkspaceRestorePermitCanonicalVectorsTest` @ `ef322c01` defines cross-language expected output independently calculated from byte encoding, not circularly from B fixture:

- Mapping: domain `family-points:restore:member-map:v2`, source family 7, target family 8, two sorted pairs `11→41`, `12→42`:
  `690dafc3d70b893b8b394827044692b26f55b8dcee773d52e510d892c1bca8fc`
- Selection: domain `family-points:restore:selection-closure:v2`, `VNext-2`, source 7, target 8; `REWARD_PURCHASES=PRESERVE count 8` category/closure/conflict hashes b/c/d; `TASK_DEFINITIONS=RESTORE count 11` hashes a/b/c:
  `b008109bcb80fde5728b36393827c5afafc3780c0d8eb3616d0a4289acf0202b`
- Overall RESTORE scope: domain `family-points:restore:scope:v2`, target 8, owner account 23/member 17, UUID `f1000000-0000-4000-8000-000000000002`, source 7, `VNext-2`, snapshot UUID `f1000000-0000-4000-8000-000000000001`, source epoch 12, snapshot SHA a×64, manifest SHA b×64, selection SHA c×64, map SHA d×64, target epoch 4, `2026-11-18T12:00:00Z` to `12:05:00Z`:
  `17f6d74e1eb83f826b455d81b2cbcf8b896ab9af1d639305ff3ed5e4bf916979`

Binary layout precisely documented in `BACKEND_C_C09_CONTRACT_V2_B_ACK_20261118.md`. **Do not hardcode these vectors as live archive hash**; deterministic test fixtures only.

## Additional C fail-closed hardening

- `WorkspaceRestorePermitScopeV2` @208017fb and `WorkspaceDestructivePermitContract` @d7cadcf reject `targetEpoch == Long.MAX_VALUE` before a future `data_epoch + 1` SQL write can overflow bigint.
- JUnit negative `WorkspaceRestorePermitScopeV2Test.targetEpochOverflowIsRefusedBeforeAnyDatabaseWrite` @b61a0592.

## Required consumer decisions / technical IMP

1. **B ACK or REVISE binary wire format:** Should B switch its TEST-only canonical plan hashing to C V2 field definitions after explicit A signed manifest and D closure; or propose a new unified versioned byte contract? Both owners must use the **same golden vectors**, no silent conversion.
2. **A exact sealed archive:** `sourceWorkspaceId`, `schemaVersion`, `snapshotId`, original source epoch, manifestRootDigest and authenticated category payload digests. Do not trust input hash supplied by web client.
3. **D accepted closure:** category content/dependency/finance target conflict hash inputs; full source mapping and all parent ancestry; reject incomplete selection before reauth grant.
4. **B/C immutable owner-review evidence:** user-reviewed mapping digest must be tied to authenticated owner/session and exact displayed roster; digest of map alone doesn't establish human review.
5. **Architecture/Integrator:** one shared IMP with producer/consumer field-level ACK, stable canonical encoding test vectors, migration version ownership and exact same target family row -> permit row -> data row -> receipt ordering. Confirm old B synthetic v2 string hashes **not** accepted as C V2 permits.
6. **QA:** `mvn -Dtest=WorkspaceRestorePermitCanonicalVectorsTest,WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test,WorkspaceDestructivePermitContractTest,DisposableOwnerPermitStorePostgresTest,DisposableOriginalOwnerBindingPostgresTest,WorkspaceResetProofConcurrencyPostgresTest test` on exact latest commit, then full four non-skipped CI. Current source commit != verified PASS.

## Confirmed latest QA prior to this packet

QA-053 `#44271716795` Maven 643 tests/0F/12E/0S; **WorkspaceResetProofConcurrencyPostgresTest 6/6 PASS**. Owner fixture previously failed duplicate PARENT role, corrected in C commits 58c40601/4ffcf6ac. Owner permit QA fixture had JDBC Instant type inference failure, QA corrected in `ac8addc6` by binding java.sql.Timestamp. QA-054 says later #44273378444 blocked before tests by unrelated Testcontainers generic syntax, corrected @fff4fb40; final C new suites await pinned new evidence.

**Runtime gates remain:** Reset Execute HTTP423, RESTORE `restorable=false`, `replaySupported=false`, BETA HOLD. No production schema/owner enrollment/auth/permit issuance, no real family writes.
