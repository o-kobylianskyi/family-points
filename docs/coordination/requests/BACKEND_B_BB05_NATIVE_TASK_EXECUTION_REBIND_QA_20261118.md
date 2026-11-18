# Backend B B-B-05 — historical TASK execution ID rebinding

**Date:** 2026-11-18. **Status:** SOURCE READY, QA exact-SHA pending. **Scope: TEST ONLY.** No production Restore controller, schema, migration, real ledger mutation or owner authentication.

## Production schema checked
`TaskInstance` declares `task_instances.id`, mandatory `task_definition_id` and `member_id`, and unique `(task_definition_id, member_id, scheduled_date, slot_key)`. Historic `POINT_LEDGER.source_type=TASK` references the **task instance**, not task definition. Preserving old numeric source_id after relocation would create a false ancestry. Do not infer target by matching IDs.

## Changes
- `DisposableFinancialTargetRehearsal` @`0cbe423a`: in same PG18 transaction after task/reward definitions, allocate `qa_restore_executions.id` for every original `TASK_EXECUTIONS`, linking mapped target member and target task definition. Return immutable `Map<originalTaskInstanceId,targetTaskInstanceId>` in `Result.executionIds`. For each TASK ledger row set `source_id` to this new execution ID; for REWARD_PURCHASE preserve separately remapped purchase ID. Missing execution parent -> hard deny, no original ID fallthrough.
- After financial SQL writes, `101b5a94`: resolve each task/purchase ledger source to a parent row in the **same target family and with the same target member**, verify actual parent ownership, task-definition FK and source category lineage. No generic unresolved `source_type` permitted.
- `DisposableFinancialTargetRehearsalPostgresTest` @`5b59ce64`: new QA-only `qa_restore_executions` table has physical FK references to `families`, `family_members` and `qa_restore_tasks`, unique source-execution key within target. Verify TASK ledger source_id equals returned allocated target execution ID and its target task/member FK. Add failure injection after executions and expand all-stages rollback to 4 stages. Reject source task execution referencing absent task definition or ledger source_id referencing absent execution, leaving all QA restore tables empty. @`4e3f1af3`: do **not** require new numerical DB ID != old ID — equal numeric value can happen coincidentally; ancestry is proven by mapping and FK, not inequality.
- No Backend A producer/codec/archive version changes; no Backend C owner security changes or Backend D accounting-policy changes.

## Independent QA instructions
Pin immutable newest commit SHA with JDK21 and Docker PostgreSQL18:
```powershell
mvn -Dtest=DisposableFinancialTargetRehearsalPostgresTest,DisposableRestoreFinancialDependencyPreflightTest,DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then run full [full-ci] on a fixed same SHA and verify actual non-skipped Maven/Web/Android/Compose/attestation, compile, Testcontainers, test count, FAIL/ERROR/SKIP, run URL and first stack traces. Previously verified 514 Maven PASS @982ad942 predates B-B-05 and is not evidence of these changes.

## Remaining blockers
- `qa_restore_executions` and other four target tables are synthetic QA tables. Not real `task_instances` or `point_transactions`. Historic execution snapshot status/date/slot/reward/penalty fields, original-assignee remap and unique target-key collisions are not yet transported; architectural source/target contract must cover them.
- Original member provenance after original DB loss requires Backend A signed original roster. Backend C owns actual owner reauthentication, one-use grant minting and Reset executor. Backend D owns REFUND/POINT_EXCHANGE/REPUTATION/group economics. Backend B only consumes approved permits within target transaction.
- Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. No real-family tests.

## Next B-B-06
Extend test-only TaskInstance snapshot metadata and parent ownership conflict checks, including assigned member and full historical fields; integrate with signed A contract and D finance proof after Architecture ACK.
