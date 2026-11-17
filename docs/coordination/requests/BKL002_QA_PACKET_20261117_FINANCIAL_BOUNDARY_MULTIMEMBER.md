# BKL-002 Backend QA #26 — financial boundary, shifted source legs, dual-member reconciliation

**Scope:** source-only/test-only, two isolated Testcontainers PostgreSQL 18 databases. **Never run on DEV/BETA/PROD family data.**

## Exact implementation
- `bcac5bdf` #25: rate-currency FK and immutable amount drift negatives.
- `2696cf3f` #26: 21 original exchange requests must reject bound 1–20; source ledger leg reassigned between two valid exchange requests must reject original 2-leg per-request provenance.
- `98b2ad06` #26: two source members mapped to two distinct target members; requests and both ledger legs rebound consistently; signed immutable exchange history read-only verifier checks per-member/point-type balances and full SHA-256 semantics; restore synthetic fixture for existing baseline and original late-credit rollback checks.
- Test: `src/test/java/com/olehkobylianskyi/familypoints/DisposableMultiExchangeNativeRestorePostgresTest.java`.
- Helper: `src/test/java/com/olehkobylianskyi/familypoints/DisposableExchangeHistoryIntegrityVerifier.java`.

## Acceptance
Run after checking out a pinned exact SHA with Docker and PostgreSQL 18:
```powershell
mvn -Dtest=DisposableMultiExchangeNativeRestorePostgresTest test
```
Then execute the full previous #24 twelve-class bundle and the existing full four-component CI. Record commit SHA, run URL, tests/failures/errors/skips, PostgreSQL containers, target family/neighbor row counts, exception types and stack traces. **Tests authored in GitHub are not independently verified until that run.**

### Required results
1. Oversized 21-request authenticated source: `SecurityException`, no target financial writes; 2-request source baseline restored.
2. Shift credit leg from request #2 to request #1 (3/1 source ledger graph): deny before target writes, source restored.
3. Valid 2 members: signed source→target request/ledger remap, balances member A −10/+2, member B −20/+4; semantic SHA equal; no neighbor mutation.
4. Revert disposable multi-member fixture; original 2-request 4-ledger baseline and all previous #24 negatives remain valid.
5. Invalid target currency code, swapped rate currency endpoints, amended request history, zero-sum extra ledger pair, duplicate replay, null/foreign mappings all rejected.
6. PostgreSQL error on second request's final +4 credit: 100% atomic rollback, not an in-memory mocked failure.

## Release boundary
No new production bean/controller. `restorable=false`, `replaySupported=false`, reset Execute HTTP 423, BETA HOLD. Historical signing and test-only SHA do **not** constitute a protected restore receipt or owner reauthentication.

## QA feedback
Report exact-SHA reproducible errors in Issue #15. Backend fixes source in bounded batches without awaiting user-operated microtests. Real approved E2E remains QA-owned.
