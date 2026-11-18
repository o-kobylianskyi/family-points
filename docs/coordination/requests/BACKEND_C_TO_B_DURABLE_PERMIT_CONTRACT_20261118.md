# Backend C → Backend B / Architecture — Durable RESET vs RESTORE permit contract (C-C-07)

**Date:** 2026-11-18. **Product direction:** ABCD-DEC-01..08 ACCEPTED; Architecture implementation principles accepted in `ADR-BKL002-ABCD-INTEGRATION-003`. **Consumer state:** AWAITING B ACK / REVISE on actual names, lock order and receipt semantics. **No destructive execution permitted.**

## Source delivered

- `src/main/java/com/olehkobylianskyi/familypoints/security/WorkspaceDestructivePermitContract.java` @ `306850b8` — a strictly validated immutable **scope descriptor**, not a durable grant.
- `src/test/java/com/olehkobylianskyi/familypoints/security/WorkspaceDestructivePermitContractTest.java` @ `fac58ba8` — five synthetic JUnit tests; exact-SHA execution **NOT_RUN** at handoff.

Scope fields: `action RESET|RESTORE`, `operationId UUID`, `workspaceId`, `ownerAccountId`, `ownerMemberId`, `snapshotId UUID`, `snapshotDigest SHA256`, `selectionDigest SHA256`, `memberMappingDigest SHA256` **mandatory for RESTORE and forbidden for RESET**, `dataEpoch`, `issuedAt`, `expiresAt` (strict positive duration ≤5 minutes). Unknown/null identity/digest, stale epoch and action cross-use fail closed. Redacted `toString`. Scope validation must never be confused with authenticated owner proof.

## Proposed DB/issuer/consumer lifecycle (review before implementation)

1. **C** resolves *authoritative verified original/current authorized owner* from durable registration/legacy audited provenance under target-family lock; owner account enabled and exact membership active.
2. **C** performs fresh server password or genuine provider reauth; no JWT-age or role heuristics. Rate limiting remains persistent; issuer stores SHA-256 hash of high-entropy one-use secret or suitably keyed verifier, not plaintext. Record has unique `workspaceId + action + operationId`, owner account/member binding, all scope hashes, epoch, creation/expiry, consumedAt, state/audit.
3. **C** exposes a sharply scoped RESTORE permit service to **B**: validated action and immutable digest values. Token issuance and wire transport follow explicit security design; do not reuse existing RESET challenge token or its `workspace_reset_challenges` table as RESTORE permission.
4. **B** begins the same target Postgres transaction as all restore writes: lock target family row/epoch **first**, then lock exact operation/permit record, check owner and action/scope/mapping snapshot, fresh expiry, unused, target current epoch and mapping provenance. Atomically consume permit + restore replay + epoch bump + success receipt. Any injected exception rolls back *everything*, including consumption. Races resolve to one commit and an idempotent receipt read, not a second replay.
5. C RESET executor, when separately released, follows analogous atomic boundary but only RESET action. Neither proof type authorizes the other.
6. Audit actor, action, original operation, digests, epochs, outcome and counts; never log proof secret or credential. A portable archive after full source-server loss must be handled by **B** without querying original DB; target owner authentication still proven independently by C.

## Blocking questions for B ACK/REVISE

- Does B require additional source-archive `schemaVersion`, `sourceWorkspaceId`, explicit target-workspace destination, or category count/Merkle-root fingerprint in exact signed scope? (C recommends include authenticated archive manifest version/source identity, not infer from target workspace.)
- Agree field names/canonical hashing for reviewed source→target member mapping; do not hash a UI display string with unstable iteration order.
- Confirm lock order `target family/epoch -> durable operation/permit -> target category rows -> receipt` and whether separate existing restore operation row precedes permit lock.
- Define same-op response after a lost POST: GET immutable receipt; never automatically consume a second permit.
- Define legacy owner evidence enrollment and recovery after loss of original DB: C cannot automatically invent owner from archived source member IDs.
- Agree who owns actual Flyway migrations and permit API wiring; request explicit IMP record before shared code.

## Tests / release
Run `mvn -Dtest=WorkspaceDestructivePermitContractTest,ResetOriginalOwnerPolicyTest,WorkspaceResetProofConcurrencyPostgresTest test` on pinned SHA, then non-skipped four-component CI. Also require real two-PG18 worker race and fail-injected rollback once durable store implemented.

**Known independent current CI failure:** run 44268695217 Java 572 tests, 1 failure + 1 error, both `DisposableFinancialTargetRehearsalPostgresTest` duplicate key (Backend B fixture). It is NOT evidence this new permit test executed. Backend C may not patch B-owned test without B routing.

**Guardrails:** Reset Execute HTTP423; production RESTORE still disabled; BETA HOLD; source-level scope does not confer permission.
