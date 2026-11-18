# Backend BKL-002 QA #53–56 — PG18 transactional approval reservation + actual CI failure triage

**Date:** 2026-11-18 CET. **TEST ONLY**. Production restore/reset flags unchanged; no Flyway migration or live REST controller added.

## Independent observed CI (not invented)
Release independently inspected four-component GitHub run [44196224270](https://github.com/o-kobylianskyi/family-points/actions/runs/44196224270), immutable SHA `304819c52ed5668da0c93b4dedc93eb1740868f4`. Backend job 134438387409: **493 tests, 2 failures, 0 errors, 0 skipped**; Web/Android/Compose successful. Backend failures: `DisposableLedgerSourceGraphVerifierTest.authenticatedArchiveNeverTreatsMalformedCurrencyOrParentAsEmpty` and `archiveRejectsDuplicateCurrencyCodesAndInvalidCurrencyId` expected `SecurityException` but import inspector emits `IllegalArgumentException` on malformed categories and invalid numeric row IDs. Both are fail-closed; class expectation mismatch, not evidence production restore was enabled.

## New commits and coverage
- `2353013d` + `8aed240a` align **malformed archive and nonpositive row ID** expectations to `IllegalArgumentException` from `ProtectiveBackupImportInspector`, while preserving `SecurityException` for otherwise valid payloads with duplicate denomination codes found by dedicated financial verifier. Re-run to independently verify two CI failures fixed; commits alone cannot prove it.
- `becbfa13` TEST-only `DisposableDurableRestoreApprovalStore`: within one PostgreSQL transaction, `SELECT families.data_epoch ... FOR UPDATE`, validate synthetic HMAC permit/actor/snapshot/family/mapping and insert one row under `operation_id uuid PRIMARY KEY` / `ON CONFLICT DO NOTHING`. SQL provides durable unique consumption **between separate Java helper instances** and rollbacks if an injected failure occurs in same transaction.
- `f446e93e` + `014a13cb`: new JUnit/Testcontainers PG18 class; real migrations before creating ephemeral `qa_restore_approval_consumption` table. Tests rollback then successful retry then duplicate denial after instantiating a new helper; stale epoch denies without insert; swapped mapping denies; two concurrent operations on same UUID yield exactly one commit, one rejected. Does not mutate actual production schema.
- `87fd52ca` + `eb5c8037` QA-038 source gap: every authenticated archived TASK_EXECUTIONS historical reward/penalty point-type must exist in the original same-family `POINT_TYPES`, even if no ledger row references the task. AES V50 negatives for unused foreign reward and penalty currency.

## Exact SHA QA target
At pinned latest SHA with JDK21, Docker PostgreSQL 18:
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableRestoreApprovalGateTest,DisposableDurableRestoreApprovalStorePostgresTest,DisposableMemberBindingReceiptVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then previous #24 twelve-class regression and full four-component CI with run URL, SHA and actual Maven TESTS/FAILURES/ERRORS/SKIPS. Prior CI run #44196224270 specifically **FAILED** Backend at old SHA, cannot be represented as PASS for this patch or extrapolated to moving main.

## Non-claims and blockers
- PostgreSQL table `qa_restore_approval_consumption` exists only inside synthetic PG18 test DB. No production Flyway migration, persisted real owner permission, owner reauthentication, multi-server trust/actor validation or main application integration.
- DB reservation and restore data mutation are not yet one atomic production transaction; demonstrates a candidate primitive only.
- A synthetic permit can be minted by anyone with the test key; the actorId is asserted input, not verified identity.
- Signed portable original `FAMILY_MEMBERS` source catalog is missing in V50; cannot establish owner/member provenance after original DB loss.
- No production `restorable`, `replaySupported`, Reset Execute; HTTP423 and BETA HOLD retained.

**Next:** Independent CI exact-SHA execution of known 2 failure repairs + new PG18 class; fix actual failures first. Then architect durable owner-reauth and atomic restore authorization protocol.
