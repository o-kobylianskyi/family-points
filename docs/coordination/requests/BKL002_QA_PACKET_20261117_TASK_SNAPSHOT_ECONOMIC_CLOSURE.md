# BKL-002 Backend QA #37–40 — historical TASK snapshot economics, member identity blocker

2026-11-17 CET. **TEST-ONLY** encrypted V50 source-graph preflight; no actual family restored.

## Producer/source checked
- `TaskInstance` JPA entity `task_instances` stores `reward_amount`, `reward_point_type_id`, `penalty_amount`, `penalty_point_type_id`; these are snapshot fields inherited from task definition at instantiation.
- `TaskService` emits `TASK/EARN` positive reward and `TASK/PENALTY` negative mandatory-miss penalty using this instance and `source_id=instance.id`.
- A signed exported task row with explicit snapshot amount/type can therefore be compared to its matching ledger row without deriving mutable current task-definition policy. If snapshot missing in reduced/legacy fixture, no claim of economic closure.

## Added source-only changes
- `2ee38757f2dc865f9383e3ee11e357278932fb80`: `DisposableLedgerSourceGraphVerifier` when persisted task snapshot amount OR currency exists, demands BOTH signed original fields, strictly positive bounded amount, exact ledger denomination and magnitude; type/sign must already be correct. Supports EARN and PENALTY independently.
- `d7fe717df60c95c56b34a666de86e57914bcc052`: authenticated V50 synthetic TASK EARN amount=5/POINTS, penalty=7/POINTS accepted; reward amount mismatch, currency mismatch, incomplete snapshot and penalty amount mismatch rejected.

## QA precise execution
Pin current SHA, JDK21 with Maven; Docker PostgreSQL 18 for exchange integration:
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then previous #24 12-class regression and all-four CI at one immutable SHA. Report actual counts, failures/errors/skips, run URL and first stack traces. NO post-new-changes Maven PASS is known from Backend source connector. Previously green 479 @8c397d0 is stale for this packet.

## Owner/architect security decision still required
The actual `WorkspacePortableBackupExporter.SCOPED_TABLES` has `TASK_EXECUTIONS` filtered by `family_members` relation, but **does not export a FAMILY_MEMBERS category**. A positive member ID inside a signed payload cannot independently prove source family ownership after original database is unavailable, nor map identities for portable target restoration. This packet does NOT implement or enable generic production member restoration. Future implementation needs explicit tenant-scoped authenticated member inventory with a verified source→target identity contract, plus owner reauthentication and full durable receipts.

TASK historical snapshots may differ from computed policy when an approved negotiated override modifies rewards. Any such disagreement should be explicitly reviewed by QA rather than auto-repaired or silently reinterpreted.

## Fail-closed release boundary
`restorable=false`, `replaySupported=false`, HTTP423 Reset Execute, BETA HOLD. Never run reset against real family, never claim VERIFIED.
