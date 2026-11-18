# Backend BKL-002 QA #49–52 — synthetic, single-use signed approval

2026-11-18 CET. **TEST ONLY / NO PRODUCTION RESTORE.** This is not family-owner authentication. Existing WorkspaceEpochTransactionGuard production locking was inspected; test fixture supplies synthetic targetEpoch rather than exercising the production guard.

## Changes and acceptance
- `a9786b11` — new test-only `DisposableRestoreApprovalGate`, HMAC-SHA256 domain-separated permit for source family, target family, original encrypted snapshot UUID, operation UUID, asserted actor ID, target epoch, issuedAt/expiry and canonical SHA256 of sorted explicit original→target member bindings. Within one verifier instance, a synchronized consumed-operation set rejects reuse.
- `3aee3324` — test-only expiry/future, wrong actor, epoch, workspace, snapshot, operation, changed or incomplete member bindings, signed same-role swap, wrong key, modified HMAC and repeated operation.
- `d1361679` — two independent disposable PostgreSQL18 multi-exchange integration uses real encrypted snapshotId with source→target reviewed member map, rejects changed epoch/swapped map and re-use.
- `e59691cc`, `201c486f` — max 5-minute approval interval and malformed permit rejection, explicit negatives.

## Run independent exact-SHA QA
```powershell
mvn -Dtest=DisposableRestoreApprovalGateTest,DisposableMemberBindingReceiptVerifierTest,DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then previous #24 twelve-class test suite and full four-component CI / pinned release artifact identity. Report actual SHA, run URL, Maven tests/fails/errors/skips and Docker PG18 startup. Latest code is **NOT independently run** by Backend GitHub connector; old 479 Maven green @8c397d0 predates packages #25–52.

## Strict boundary
- The HMAC key and 'actorId' are synthetic. No real owner reauthentication, authorization, production key management, or reviewed-pair trust chain is implemented.
- The consumed-operation set is **in-memory only**, not durable/cross-process, and cannot authorize a production write or prevent replays across restarts.
- Production V50 lacks signed portable original FAMILY_MEMBERS; original member identity after source loss is unresolved.
- No production `restorable`/`replaySupported`, HTTP423 reset Execute remains, BETA HOLD. Never reset real family data.

Next: consume exact-SHA QA failures, then design durable transactional nonce/operation locking and authenticated owner provenance.
