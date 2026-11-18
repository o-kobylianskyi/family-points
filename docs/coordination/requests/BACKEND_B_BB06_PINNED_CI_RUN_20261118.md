# Backend B B-B-06 — explicit FULL CI request (read-only source verification)

**2026-11-18**. The newest tests are **NOT certified** by an earlier green CI. Commit this request with the workflow's supported `[full-ci]` directive and independently inspect the same SHA.

## Targeted JDK21 + Docker PostgreSQL18 suite
```powershell
mvn -Dtest=DisposableTaskInstanceSnapshotValidatorTest,DisposableFinancialTargetRehearsalPostgresTest,DisposableRestoreFinancialDependencyPreflightTest,DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest test
```

## Non-skipped full workflow
Require actual executed Java Maven backend, Web/Node, Android/Gradle, Compose/Docker and exact-SHA Full CI attestation and per-commit outcome. Report SHA, run URL, complete tests/failures/errors/skips and first non-infrastructure failure. All jobs must share one immutable SHA. A green workflow with skipped heavy component jobs is not a PASS. Infrastructure Maven distribution fetch failures are separate from JUnit failure. Prior #44268695217 was red 572/1F/1E on B QA global PK, #44270256780 red 602/0F/6E from C V41 fixture, and #44271499224 testCompile blocked by FIN-09 Testcontainers generic issue. QA's subsequent source fixes do NOT by themselves confirm fresh CI success.

## Acceptance essentials
- B-B-06 strict snapshot metadata: original member/assignee, date+slot, complete title/status/claim/start/completion/excuse, point and reputation economies; missing fields fail closed, immutable source earnings/penalties match ledger.
- V41 and FIN-09 known errors are rechecked at current SHA alongside B global PK collision; any failure returned to owning stream and QA, no attempt to mask.
- PG18 isolated: 4 injected rollback points remove all five QA target tables including executions; clean fresh retry, duplicate target replay denied, concurrent same operation at most one success, wrong-family/global PK collision leaves target and neighbor unchanged; TASK source_id maps to allocated execution ID.
- No real user/family data, no production restore/reset, no unauthorized owner grant.

This request is **a test trigger, not a PASS result**; release remains BETA HOLD / HTTP423.
