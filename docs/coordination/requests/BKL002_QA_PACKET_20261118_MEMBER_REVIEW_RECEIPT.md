# BKL-002 Backend QA #45–48 — snapshot-scoped member review receipt and unused TASK parent economic closure

**Date:** 2026-11-18. **TEST ONLY.** All receipt signing in current tests is synthetic; this is **not** owner authentication, an active portable member catalog, or authorization for production restore.

## Scope
- V50 importer intentionally forbids `FAMILY_MEMBERS` category, while production exporter omits it; do NOT silently change V50 archive contract.
- A two-PostgreSQL roster preflight (#41–44) checks roster membership/role but cannot distinguish a swap of two same-role persons.
- `bcb05290`: new test-only `DisposableMemberBindingReceiptVerifier`. Its HMAC-SHA256 review receipt binds an **explicit reviewed sourceMember→targetMember pair** to source family ID, target family ID, exact protected snapshot UUID, and a unique per-binding nonce. Exact, injective mapping coverage; duplicate approvals/nonces, wrong target, signature tamper, wrong key, wrong source/target family or wrong snapshot fail closed. Caller-provided approvals are authenticated only to the supplied review key; proof of owner identity/authorization is still absent.
- `89f47646`: pure test fixture of two same-role source members and two same-role target members; swapped IDs fail against original approval receipts; replay under different snapshot, source/target family or HMAC key and altered identity fields fail.
- `793e9b40`, `304819c5`: existing independent two-PostgreSQL 18 multi-exchange test demonstrates roster structurally accepts a same-role swap while an approval receipt bound to **actual encrypted archive snapshotId** rejects it.
- QA-036 observed unused archived TASK parent could carry partial original economics. `5de56133`: require matching non-null snapshot reward_amount/reward_point_type_id or penalty_amount/penalty_point_type_id for every archived task parent, including unreferenced parents. `0af75e10`: AES negatives for unused task with only reward amount or only penalty currency; valid paired but unreferenced task accepted.

## Independent QA commands
Pin exact latest SHA and ensure JDK21:
```powershell
mvn -Dtest=DisposableMemberBindingReceiptVerifierTest,DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then repeat previous #24 twelve-class suite and full all-four CI on same exact SHA (Docker/PostgreSQL 18 required). Report exact commit, run URL, TESTS/FAILURES/ERRORS/SKIPS, first exception, and confirm neighbor family unchanged. Prior 479 Maven PASS @8c397d0 predates #25–48 and cannot qualify new code.

## Explicit non-claims/remaining blockers
- The synthetic HMAC key in unit/PG test is not backed by authenticated owner session or durable protected approval. It authenticates a particular reviewed pairing **only if the review key and review process are trustworthy**; no live API may use it to bypass stronger gates.
- Signed receipts do not authenticate a person's identity on their own. Original FAMILY_MEMBERS remains missing from portable V50 graph; post-loss source tenant ownership not fully proven.
- The original historical TASK snapshot pairing validator accepts reduced legacy rows where both fields are absent; those rows cannot count as proven complete economics. If production exports zero-priced tasks with partial null pairs, QA must report exact semantics before choosing schema policy.
- No live restore implementation, snapshot signing trust chain, full media closure, owner reauthentication, cross-category refunds, durable receipt lifecycle, or production release permission.
- Production `restorable=false`, `replaySupported=false`, reset Execute HTTP423 and BETA HOLD unchanged.
