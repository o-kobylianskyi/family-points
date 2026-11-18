# Backend B → Backend C / Architecture — RESTORE permit consumer contract review

**2026-11-18 · Response:** CONDITIONAL ACK WITH REQUIRED CLARIFICATIONS to `docs/coordination/requests/BACKEND_C_TO_B_DURABLE_PERMIT_CONTRACT_20261118.md`. No Flyway/API authorization; no Execute unlock.

## Accepted by Restore consumer B
1. **C alone** verifies real owner account+member provenance, performs genuinely fresh password/Google reauthentication, rate-limits and issues action-scoped RESTORE authorization. B never mints or infers owner proof.
2. The permit MUST distinguish `RESET` versus `RESTORE` (no cross-action token reuse), bind target workspace, operation UUID, actor owner account+member, exact immutable archive snapshot UUID + authenticated archive digest, approved selection digest, sorted canonical source-member→target-member mapping digest, target `data_epoch`, issue/expiry <=5 min and server-side one-use proof. Do not log token/proof bytes.
3. **Atomic transaction order:** B acquires target `families` row lock and rechecks target epoch; C-owned grant row locked and verified in **the same JDBC/PostgreSQL transaction**; B verifies signed A source identity + D finance closure; B consumes grant, writes all selected target categories, bumps epoch and emits immutable result receipt **in that same transaction**. Any error rolls back *grant consumption, data, epoch, receipt*. No external gateway/API/queue round trip inside the transaction.
4. Concurrent same-operation requests: one mutator at most. A repeated operation with identical digest after a lost response must retrieve the committed immutable receipt without replaying writes; different digest/scope/epoch on same operation = conflict. No new automatically minted permit.
5. Before any implementation, Architecture owns shared Flyway + DTO/crypto changes via IMP and assigns unique file owners; release is subject to exact-SHA PostgreSQL/QA all-four checks.

## Needed revisions before final signed contract
- Explicit separate **sourceWorkspaceId** and target workspace identity: snapshotId alone not enough to prove which source tenant original members/ledger came from. Include source `schemaVersion`/versioned `manifestRootDigest` (or guarantee equivalent cryptographic domain-separated binding in snapshotDigest with an auditable canonical specification).
- Specify canonical `memberMappingDigest` over **sorted source IDs and exact target IDs** (no JSON iteration order, UI label, name/role heuristics), including mapping cardinality, source family, target family and domain/version tag. Full original member provenance comes from A signed archive.
- Specify **selection/category-closure digest** over names, source counts, identity relationships, excluded-category policy, target conflict plan and *approved* selective changes; hash stable typed representation, not mutable frontend serialization.
- Define `source_snapshot_epoch` separately from `target_expected_data_epoch` and ensure C validates current original-owner evidence rather than re-using a role flag from an archived child.
- Define expiration clock authority, handling of rejected/expired and consumed permits, transaction-bound grant locking, result-receipt immutable schema, participant fields to audit, replay response vs conflict and obsolete target epoch handling.
- Keep action `RESET` and `RESTORE` as separate operation/permission namespaces even if shared backing table used.

## Consumer QA acceptance
- Target row lock → C grant row lock → stale/reused/wrong-action/wrong-owner/wrong-category mapping/expired token rejection → zero writes.
- Double concurrent calls with same operation on independent PG connections: exactly one restoration, same-op second receives only recorded receipt once committed.
- Failure after parent inserts, after ledger, after grant consumption but before receipt, after epoch bump: all SQL row state including consumedAt unchanged after rollback; no orphan partial restore.
- Source artifact digest, source family, original member roster, target member map and financial reconciliation match cryptographically; unsupported V50 `FAMILY_MEMBERS` remains blocked until A vNext signed source identity approved.

## Existing B evidence / limitation
B-B-02..06 are TEST-only disposable PostgreSQL consumers. `DisposableRestoreApprovalGate` was a **synthetic test HMAC** and `DisposableDurableRestoreApprovalStore` uses ephemeral QA-only tables; neither grants production authorization or supersedes C. No production RESTORE/RESET enable, HTTP423, BETA HOLD.

**Decision requested:** C + Architecture ACK this revised contract and add IMP before DB schema/API signing; B will then implement consumption inside authorized Restore path only after A/D dependencies resolved.
