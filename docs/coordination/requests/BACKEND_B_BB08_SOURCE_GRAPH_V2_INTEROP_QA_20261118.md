# Backend B / B-B-08 — Source row closure, C canonical V2 interoperability and QA

Date: 2026-11-18 CET. **TEST ONLY / SOURCE READY / exact-current-SHA Maven NOT_RUN independently by Backend B.** No production Restore/Reset API, owner auth, migration or family data writes.

## Reviewed current source
- Architecture Checkpoint 04, C-C-09 V2 Java `WorkspaceRestorePermitScopeV2` and `WorkspaceRestoreSelectionClosureV2`, C-C-10 P0 mismatch report and its independent golden vectors. C string/binary mismatch is real; previous B `DisposableRestorePlanFingerprint` pipe-delimited hashes MUST NOT be consumed as C V2 scope digests.
- A B-A-02 source member contract is still proposed, not a cryptographically authenticated vNext importer.
- QA-055 introduced Testcontainers2 Java syntax preflight, but no verified current full CI PASS from that commit. Previous #44273378444 failed before JUnit on FIN generic Testcontainers; independent exact-SHA B-B-07 PG18 PASS not yet documented.

## Test-only deliverables
- `DisposableRestoreRowClosureVerifier` @ed12e564: exact original source FAMILY_MEMBERS roster from source family (including inactive and tombstoned members), mandatory role/type/active fields and source identity. Strict mapping source IDs equal roster, rejects foreign/duplicate/unknown members, missing task/reward/currency parent, purchase without archived reward source, unsupported request purchase, orphan TASK/REWARD_PURCHASE ledger, unsupported REFUND/EXCHANGE and other source types. Composes existing B `DisposableRestoreFinancialDependencyPreflight` for ledger amount/source/buyer/denomination parity and mapped target balance. Source DB not needed; JSON is synthetic, NOT authenticated A.
- `DisposableRestoreRowClosureVerifierTest` @ce4883ca: positive deleted member historical ownership +3 signed target balance and negative wrong family, duplicate roster, orphan assignee/task/purchase, wrong currency, refund/request bypass and incomplete source roster mapping.
- `DisposableRestoreScopeV2Consumer` @caa9768d / schema marker @120f5a6e: exact typed C binary SHA256 using C-owned `canonicalMappingDigest`, `WorkspaceRestoreSelectionClosureV2.canonicalDigest` and `canonicalScopeDigest`, no B second implementation. Requires caller scope matches purported issued scope, sourceWorkspaceId / `VNext-2` test-only marker / snapshot UUID/source epoch/manifest root, destination/epoch, sorted injective mapping and full category decisions/row counts. Explicit BLOCK or covert selection drift denied.
- `DisposableRestoreScopeV2ConsumerTest` @879b98b4 / capture fix @d890355d / schema marker @3a082c12 / C-C-10 golden cross-check @590040d2: fixed C SHA vector parity, sorted category order independence, same-role member swap, wrong manifest/source epoch/scope, selected row count and decision tamper, expired or mismatched operation. The `VNext-2` string is copied from C golden test vector; final A source wire-format token requires Architecture IMP.
- `DisposableRestoreVerifiedGraphGate` @67429d44: require C V2 checked descriptor + FULL bounded source category roster/count and original member→target mapping + per-row task/reward/purchase/currency/ledger integrity **before target writes**, rejects PRESERVE/BLOCK of required source graph categories (bounded 7-category scenario only).
- `DisposableRestoreVerifiedGraphGateTest` @085192ef / marker fix @c922d9e6: happy +3 mapped balance, source family drift, missing original roster, orphan task/ledger, omitted/blocked parent, row count mutation, member map spoof, unsupported refund.

## Critical security limits
- **No verified signed A source manifest**, no authenticated category content/dependency digest comparison (categories in test are caller-supplied hash strings), no proven true owner of target, no real C durable permit consumed. A digest is not authorization.
- The legacy B-B-07 `FP-RESTORE-*-V2` pipe hashes are not interoperable with C's binary contract and cannot serve as permit digest. This package introduces an adapter that **calls C canonical algorithms**, not conversion or dual-acceptance.
- All seven categories here represent a bounded supported full TASK/REWARD/PURCHASE/POINT graph. Other 16+ categories/media, historical refund/exchange/reputation/group are unsupported in this test gate, remain D/A dependencies. No production target writes or release.
- Owner-reviewed mapping evidence digest, source category encrypted hash verification, audited receipt and actual C grant/epoch atomic consumption remain P0 Architecture/C/D/A approval tasks.

## Independent QA command
Pin immutable latest merged SHA, JDK21 and Docker PostgreSQL18 for existing B PG suite:
```powershell
mvn -Dtest=DisposableRestoreRowClosureVerifierTest,DisposableRestoreScopeV2ConsumerTest,DisposableRestoreVerifiedGraphGateTest,WorkspaceRestorePermitCanonicalVectorsTest,WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test,DisposableRestorePlanFingerprintTest,DisposableRestorePlanTransactionGuardPostgresTest,DisposableFinancialTargetRehearsalPostgresTest test
```
Then non-skipped full four-component [full-ci] with exact SHA, run/jobs, Maven total/fail/errors/skips and first failure stack; verify QA testcontainers2 preflight and backend C/D tests. Prior red runs including #44271716795 (643/12E) and #44273378444 testCompile cannot stand for current green.

## Formal response
`docs/coordination/requests/BACKEND_B_TO_C_C10_BINARY_V2_ACK_20261118.md` @041bb43c: conditional field-level ACK to C binary wire candidate and C golden vector test; rejects treating B text hashes as C, requests A canonical manifest source and D financial closure, C real issuer/owner evidence and Architecture shared IMP.

**Production restorable=false, replaySupported=false, Reset Execute HTTP423, BETA HOLD.**

## Next Backend B / B-B-09
After exact-SHA QA, independently attest original signed A category payload digests and full parent closure against C selection; once Architecture approves shared IMP, perform C permit consume + finance replay + epoch + receipt within a single target transaction. Never substitute synthetic gate for owner permission.
