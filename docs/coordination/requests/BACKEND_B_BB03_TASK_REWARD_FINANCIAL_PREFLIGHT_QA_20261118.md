# Backend B B-B-03 — TASK/REWARD/POINT_LEDGER target mapping QA

2026-11-18. TEST ONLY. Consumer read-only preflight; not production Restore.

## Code
- `DisposableRestoreFinancialDependencyPreflight.java` @cbc520e9 + @4b733d78: require six source arrays, unique scoped TASK/REWARD/POINT_TYPE and parent IDs, injective explicit original→target member, currency, task, reward and purchase mappings. Resolve TASK_EXECUTIONS→TASK_DEFINITIONS, PURCHASE→REWARD_DEFINITION and POINT_TYPE; reject missing/foreign parent.
- Validate source POINT_LEDGER: TASK EARN/PENALTY signed and owned by matching TASK_EXECUTION, each TASK+event at most once; REWARD_PURCHASE SPEND exact original buyer/currency/historic price and purchase ID charged only once; MANUAL must have no source_id. Unsupported REFUND/POINT_EXCHANGE/unknown source types fail closed. Checked target member×currency signed aggregate.
- `DisposableRestoreFinancialDependencyPreflightTest.java` @e87240d0 + @93af5b44: TASK +5, purchase −4, MANUAL +2 => mapped target balance +3. Negatives for missing task/reward parents, wrong family/member/currency, incomplete or noninjective mapping, wrong purchase value/type/parent, repeated ID and TASK award, refund/exchange disguised as supported type.
- No production DB write, no archive schema or importer changes, no owner credentials granted.

## Independent QA
At pinned newest SHA, JDK21 + Docker PG18:
```powershell
mvn -Dtest=DisposableRestoreFinancialDependencyPreflightTest,DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest,DisposableLedgerSourceGraphVerifierTest test
```
Then full non-skipped [full-ci] 4 jobs same SHA. Send job URL, SHA, Maven TESTS/FAILURES/ERRORS/SKIPS and Docker outcome. The previously verified #44265994374 Maven 507 PASS **predates** this B-B-03 change; new test status NOT_RUN independently.

## Contract blockers
The helper assumes source JSON is authenticated by Backend A and member ancestry is separately proven; no signed original FAMILY_MEMBERS in V50. D owns real REFUND/POINT_EXCHANGE/reputation/group financial semantics; this preflight deliberately refuses unsupported types. C owns actual owner authentication/grant, B only consumes approved C grant with target transaction. No TASK/REWARD/POINT_LEDGER production target insertion yet and no full 23-category engine.
Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.

## Next B-B-04
Native synthetic target FK writing under one approved operation with exact identity/finance parity; wait for A/C/D shared contracts before any production integration.
