# Backend BKL-002 QA #34–36 — protected financial archive parent closure

Date 2026-11-17 CET. **TEST ONLY**. Synthetic AES V50 archive verification, no production restore or destructive API changes.

## Input from independent QA
QA-034 Issue #15 comment 6946804506 identified a precise gap: `DisposableLedgerSourceGraphVerifier` checked that each `TASK_EXECUTIONS.member_id` was integral but did not require it to be **positive** for **unreferenced** historical task rows. Full task member identity ownership cannot be inferred from a bare member ID without a signed original member catalog. Preserve this limitation; do not mark `VERIFIED`.

## Changes included
- `26aa07c9`: require positive source TASK_EXECUTIONS.member_id for **every** archived task execution, including unused parents.
- `777ba701`: AES synthetic negatives for extra unreferenced task with member_id=0 and -1; valid referenced task remains accepted.
- `5a57de9a`: mandatory POINT_LEDGER and POINT_TYPES sealed category payloads must be JSON arrays; optional TASK_EXECUTIONS/TASK_DEFINITIONS/REWARD_PURCHASES if present must also be arrays. No silent missing-as-empty interpretation.
- `9478e2a5`: AES sealed JSON malformed/null point-type, task definition, task execution parent negative cases.
- Previous #25–33 signed balance and multi-exchange test-only work remains subject to independent CI.

## QA execution
On an **immutable pinned exact SHA** with JDK 21 and Postgres18 Docker available:
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then run the consolidated historical #24 twelve-class regression and four-component CI. QA must report actual job URL/SHA, failures/errors/skips, first stack and Docker startup result. Code review/source commits are NOT test PASS. Last independent 479-test green at 8c397d0 is stale relative to #25–36.

## Remaining blockers and explicit non-claims
- Positive TASK member ID is **not proof** the member belongs to the original family; a signed member catalog and historical parent ownership closure are still needed.
- TASK historical reward/penalty amounts and event lifecycle are not yet proven against persisted original task-instance reward/penalty fields.
- REFUND producer sourceType/sourceId is caller-specific; general refund replay stays unsupported.
- Cross-category and group/reputation ledger identity, durable signed verified recovery receipt, owner fresh reauthentication, complete scoped transactional Restore and all-four same-SHA CI must still be proven.
- No production `VERIFIED`, `restorable`, `replaySupported`, or Reset Execute enablement. HTTP423 and BETA HOLD.

## QA response
Send exact reproducible failure to Issue #15; Backend repairs failures without user-run microtests. Only QA may coordinate an explicitly approved disposable DEV end-to-end; never reset real family data.
