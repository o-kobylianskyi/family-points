# BKL-002 Backend QA #61–64 — atomic cross-category synthetic financial restore

Date: 2026-11-18. TEST ONLY. No production mutation, no new migration, no real Restore authorization.

## Changes
- `b607e240`: `DisposableDurableRestoreApprovalStore.consumeWithTwoSyntheticCategories` uses ONE PostgreSQL 18 TransactionTemplate, locks target family epoch `FOR UPDATE`, verifies synthetic snapshot/member-map HMAC permit, reserves unique operation UUID and writes two separate QA-only categories: points `qa_restore_financial_rehearsal` and reputation `qa_restore_reputation_rehearsal`. Fail injection at (1) after approval, (2) after point write, (3) after reputation write.
- `aef18c45`: Testcontainers creates QA-only reputation table after Flyway migration. For EACH failure stage assert approval, points and reputation rows all absent; retry using new store instance commits all three exactly once, duplicate rejected. Incorrect original-member map and stale epoch leave both category rows and approval empty.
- `ef222689`: concurrent duplicate operation UUID with BOTH category writes: exactly one successful commit, one rejected, one approval + one points row + one reputation row; original source second member mapped to target 42.
- All fixtures use synthetic key, asserted actor, source→target mapping; no real family member catalog, protected original category digest or production target ledger touched.

## QA exact SHA and candidate evidence
```powershell
mvn -Dtest=DisposableDurableRestoreApprovalStorePostgresTest,DisposableRestoreApprovalGateTest,DisposableMemberBindingReceiptVerifierTest,DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then #24 historical 12-class and full four-component no-skip candidate, exact SHA + run URL + Maven tests/failures/errors/skips, Testcontainers PG18 state. Prior known independent CI #44262590272, #44262614624 and #44262835157, #44262921026 each had exactly one Backend test failure involving opposite exception expectations in `DisposableLedgerSourceGraphVerifierTest.archiveRejectsDuplicateCurrencyCodesAndInvalidCurrencyId`; do not treat them as PASS. New current-SHA PASS not independently verified here.

## Explicit scope and blocker
The fixture only demonstrates SQL atomicity for one synthetic permit and two synthetic categories (points/reputation). It does NOT prove production full restore, durable owner reauthentication, portable original FAMILY_MEMBERS identity, protected original history mapping, full category dependency closure, or financial source-target checksum. Original HTTP423 Reset Execute, `restorable=false`, `replaySupported=false`, BETA HOLD unchanged.

## Remaining rough estimate, subject to architecture/QA evidence
Approximately **8–12 further large backend implementation batches** for BKL-002 production-grade feature readiness, not a commitment or percentage:
1. ~2 batches: authenticated original family-member identity and privacy/historical memberships
2. ~2: real owner reauthentication, durable grant/nonce schema and audit
3. ~2–3: full scoped restore graph with media, currencies, refund semantics and same-transaction rollback
4. ~1–2: cryptographic source/target verification, immutable receipts and post-crash recovery
5. ~1–3: closure of independent QA failures, tenant isolation hardening and release gates
Unknown architecture findings could increase range; full BETA readiness also depends on unrelated V49/Web/Android QA. Do not claim one package or synthetic tests equal production feature DONE.
