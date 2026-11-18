# FamilyPoints — Backend C: Security & Reset (2026-11-18)

**Direction:** Backend C, assigned by project owner on 2026-11-18.
**Baseline inspected:** main `129d3dca1542a28905934726828f7fae95c1814a`; this is an ownership/contract handoff, not executable implementation or CI evidence.
**Priority:** P0 BKL-002 safety. **BETA HOLD; Reset Execute HTTP 423 remains mandatory** until verified release gates.

## Boundary and invariants
- **C owns:** workspace original-owner binding; server-verified fresh reauthentication; durable single-use authorization grants; reset capabilities and read-only preview; FULL/SELECTIVE family-data Reset execution; reset audit, idempotency, monotonic dataEpoch and stale offline mutation rejection.
- **A owns:** portable backup capture/artifact/source archive and source member catalog. C consumes backup readiness only; no archive/codec/schema changes without A.
- **B owns:** archived-data Restore (source→target mapping, replay, restore receipt). C does not implement Restore or consume a Reset approval as a Restore approval.
- **D owns:** financial ledger, historical adjustments, currency/balance correctness and reconciliation. C must demand a signed-off financial deletion/adjustment plan before executing any mask affecting D data.
- **Integrator/Architecture owns:** shared API DTO contracts, common security services, file ownership, migration integration, CI/QA gates.
- Preserve global `UserAccount`, workspace ID and at least one validated original owner/admin membership, other workspaces, global template catalog and immutable security audit. Neither a role-name nor a shared account's presence in two families proves ownership of the target workspace.

## Security contract, pending architecture approval
1. Resolve authenticated global principal server-side and verify its **current** original-owner binding to the **target** workspace; delegated `MANAGE_WORKSPACE` alone is insufficient. Recheck in the execute transaction under workspace lock.
2. Verify fresh current-account credential with existing production auth service. Password path uses server-side password verification; provider-only account must use an independently verified recent provider challenge or fail closed. Never log/store plaintext password or treat typed family name as identity verification.
3. Create a cryptographically random, opaque **one-time durable approval** (store only hashed token), bound to actor account, target workspace, authorized action **RESET (not RESTORE)**, preview/scope fingerprint, current dataEpoch, nonce, creation/expiration, and failed-attempt state; short TTL (target <=5 min).
4. Consume approval atomically with the one permitted execute attempt; deny expiry, actor/workspace/scope/epoch mismatch, replay, concurrent double-submit, and conflicting idempotency-key payload. Failed or refused execution cannot return a false success.
5. Preview must be **read-only**, return actual per-category counts, dependency closure/blockers, preserved entities and exact version/fingerprint. No auto-expansion of selected categories without explicit acceptance. Recalculate/lock at execute: changed scope/counts => 409 re-preview.
6. FULL or SELECTIVE execution must respect approved dependency DAG and tenant-scoped FK ordering, rollback on any failure and update monotonic dataEpoch in the same successful commit. If a category is not implemented and proven safe, it is **not selectable** and execute stays HTTP 423 for that mask.
7. Audit actor/workspace/scope hash, operation UUID, before/after epoch, timestamp, outcome and counts without credential/token content. Same idempotency key+same request replays durable result, differing request => conflict. Any stale offline mutation from prior epoch must be rejected, including JWTs issued before reset.
8. Never run destructive calls against DEV/BETA/PROD family data without a separate explicitly authorized disposable fixture and Release approval. Only synthetic disposable PG integration tests are in C's initial scope.

## First delivery sequence (C-C-01 onward)
- **C-C-01:** inventory production auth/owner binding, current reset-related controller/service/migrations and existing HTTP 423 gates; map file ownership and explicit shared-contract requests. No disabling of any safety gate.
- **C-C-02:** implement independent owner/reauth policy with negative tests: child, delegated manager, cross-workspace shared account, stale membership, Google-only account and wrong password. Integrator reviews shared auth code changes.
- **C-C-03:** design/test DB-backed single-use approval bound to RESET action+scope+epoch; concurrency, TTL, hashed token, collision and replay tests. Avoid overlapping Backend B's disposable Restore approval test utilities.
- **C-C-04:** read-only preview/capabilities and versioned fingerprint with counts and closure; no mutation on denied/stale preview.
- **C-C-05:** implement only approved Reset category masks with Backend D reconciliation and scoped transaction/rollback, increment dataEpoch and enforce offline epoch barriers.
- **C-C-06:** synthetic PostgreSQL 2-workspace acceptance, repeat/retry/rollback, CI exact SHA and QA handoff; release remains HOLD until integrator and QA sign off.

## Contract requests / prerequisites
- **Integrator/Architecture:** confirm endpoint DTOs, shared owner-binding and epoch schema/API, migration ownership, `RESET` vs `RESTORE` approval separation and operation ID namespaces; assign production auth service files exclusively.
- **Backend D:** signed-off category closure, ledger/balance/history semantics and deletion/adjustment order.
- **Backend A:** backup readiness/capture indication; no assumed automatic undo.
- **Backend B:** approval-store isolation and coordination on workspace lock/epoch changes, but **no Restore unblocking** from C tests.
- **QA/Release:** synthetic fixture first, only explicit human-authorized disposable workspace for later manual DEV tests, no production cleanup.

## Evidence status
- Scope/legacy ADR and Backend B handoff reviewed; this file records the owner assignment and proposed secure sequencing.
- **No C code, schema migration or test execution is claimed by this handoff.**
- Existing scope decision on 2026-10-26 `REVISED_BETA_SCOPE_20261026.md` supersedes historical 2026-10-24 `AFTER_INTERIM_BETA` freeze for BKL-002 only.
- Track future changes and exact-SHA CI separately in `docs/coordination/logs/backend-c.md`.
