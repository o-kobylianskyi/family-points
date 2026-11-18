# Backend B — B-B-06 large batch: task history, slot conflict, global PK and C permit contract

**Date:** 2026-11-18 CET. **SOURCE READY; ACTUAL CI PENDING. TEST-ONLY.** Not a production restore. No migrations, real `task_instances`, owner credentials, live ledgers or Reset Execute touched.

## Independently observed red QA evidence, not mislabeled PASS
QA issue #15 reported actual run [#44268695217](https://github.com/o-kobylianskyi/family-points/actions/runs/44268695217) @93c789f: **572 Maven tests, 1 FAILURE + 1 ERROR, 0 skipped** in `DisposableFinancialTargetRehearsalPostgresTest`. Cause: synthetic global `qa_restore_tasks.id` PK collision gave Spring DuplicateKeyException after previous fixture used fixed IDs; prior exception assertion should not be weakened. Other QA testCompile failure in `DisposableFinancialLedgerPostgresParityTest` from Testcontainers 2.x generic type was separately repaired by QA @2250f32; this does not prove B-B-06.
Current B mapping uses per-target family IDs; `093f4ca6` also checks entire global QA task/reward/purchase target id set **before any write** and fails closed `QA_FINANCE_GLOBAL_ID_COLLISION` for occupied foreign PK. Test `2a07b90a` inserts a neighbor-owned colliding PK, rejects B target operation before writing and preserves neighbor row. This checks predictable conflict handling but not arbitrary cross-tenant shared-ID concurrent allocation; production must generate IDs with DB sequences and track source→target maps.

## History implementation
- `fd5dc71c`: new TEST-only `DisposableTaskInstanceSnapshotValidator`, checks source task, executor and original assignee ID map, schedule date, 40-char `slot_key`, title/description, mandatory, due time, valid TaskInstanceStatus, start/completion time, point reward+currency and penalty+currency pair semantics, reward/penalty processed flags and TASK ledger historical amount, currency, member and duplicate event lineage. Missing historical required fields refuse import rather than pulling from mutable task definition.
- `c35c8a5b`: requires explicit nullable original assignee, description, due time, claim/start/complete time, excuse reason/comment, point reward and penalty, reputation reward and penalty. Matches production `TaskInstance` and `TaskExcuseReason` enums. Supports reputation-only processed task without fabricating a POINT_LEDGER reward. EXCUSED must identify valid reason. Does NOT reconcile reputation ledger itself; D owns reputation finance closure.
- `08fcfd83`, `b93222a1`: `DisposableFinancialTargetRehearsal` fails closed on incomplete source historical data **before** target transaction, detects duplicate `(sourceTask,member,scheduledDate,slotKey)` before insert, and persists historical source snapshot into QA-only `qa_restore_executions`: mapped task/member/original assignee, date, slot, title, description, mandatory, due/status, claimed/start/complete, excuse, point/reputation amounts, original currency rebinding and processed flags. Keeps allocated original execution→target execution map; TASK ledger `source_id` uses new allocated target execution ID.
- `7cbcff8c`, `5a36e161`: PG18 QA table has **UNIQUE(task_id,member_id,scheduled_date,slot_key)** and physical foreign keys into target family/member/task/currencies; fixture includes all explicitly null historical fields. Tests stored status, date, slot, original assigned member, claim time, historical reward/denomination and full transaction rollback.
- `2f2bdb52`, `d5d894aa`, `8f9068e6`: pure JUnit snapshot positives/negatives: missing historical fields, invalid dates/slots/status/excuse/assignee/currency, incomplete reward currency pairs, mismatched TASK financial amount/currency/type, duplicated reward leg, rewardProcessed without ledger, valid reputation-only award, no guessing absent claim/comment fields.
- `2a07b90a`: PG18 negative scenarios for missing claim field, altered task snapshot reward vs ledger, invalid original assignee, two source executions with identical target task/member/day/slot, cross-family global PK conflict, and reputation-only task with no synthetic POINTS earning (purchase −4 plus manual +2 produces −2).
- All tests in `src/test` only; no production restore/DB/API change.

## Owner permit coordination with C
- C requested Backend B ACK/revise for `WorkspaceDestructivePermitContract` @306850b8 (descriptor is not a real grant). B replied conditional ACK + versioned source identity/typed domain-separated hashes and lock/receipt details: `docs/coordination/requests/BACKEND_B_TO_C_RESTORE_PERMIT_CONSUMER_ACK_20261118.md` @25d2b6cc.
- B agrees target family epoch row FIRST, C-issued action=RESTORE permit row SECOND, then transactional restore writes, epoch bump and immutable receipt. Failures roll back grant consumption + all writes. Same-op lost response retrieves receipt, not second replay. Scope MUST bind authenticated source workspace/manifest version/snapshot/digest, target workspace, source→target member digest, selection/closure, owner's account+member, TTL <=5m and target epoch.
- No new production grant mint/store migration by B; C + Architecture IMP ACK required.

## Exact SHA QA command, in order
On one immutable SHA, JDK21 + Docker PostgreSQL 18:
```powershell
mvn -Dtest=DisposableTaskInstanceSnapshotValidatorTest,DisposableFinancialTargetRehearsalPostgresTest,DisposableRestoreFinancialDependencyPreflightTest,DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then previous B-B-02..05 source-related tests, Finance D integration and **real all-four [full-ci]** (Maven/Web/Android/Compose build/test not skipped; 98+ Release gates). Report SHA, Actions URL, test counts, failed first stack, PG18 container state; exclude unrelated doc-only green. **Backend B source connector has not executed Maven for new B-B-06 commits.**

## Remaining P0 blockers
- Test-only QA tables and mock original JSON; real production `task_instances` target insertion and conflict reconciliation not implemented. Producer V50 does NOT contain signed portable original `FAMILY_MEMBERS`. Backend A must supply authenticated original member ancestry and complete historic TaskInstance fields for vNext; no invented fallback for old V50.
- C actual owner-auth durable RESTORE grant and action-scoped issuer/storage not implemented by B; owner reauth and lost server proof unresolved.
- D must approve and reconcile real reputation, REFUND, exchange, group financial semantics before full replay. No source-target all-23 category closure, media binary integrity or production full restore.
- Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, **BETA HOLD**.

## Next B-B-07
After QA exact SHA fixes, implement test-only stable typed restore plan/digest and attempt-source identity conflict matrix; integrate C permit **consumer** only when signed A and C permits + Architecture shared IMP accepted. No production unlock without signed owners/security and D finance closure.
