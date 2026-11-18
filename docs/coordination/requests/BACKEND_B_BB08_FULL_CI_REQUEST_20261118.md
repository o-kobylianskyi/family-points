# Backend B B-B-08 — exact-SHA required full regression [full-ci]

Date 2026-11-18. This file and commit are **a request for execution, NOT a claim of green CI**.

## Compile and target execution
JDK21 + Docker PG18, pin the immutable SHA of this change:
```powershell
mvn -Dtest=DisposableRestoreRowClosureVerifierTest,DisposableRestoreScopeV2ConsumerTest,DisposableRestoreVerifiedGraphGateTest,WorkspaceRestorePermitCanonicalVectorsTest,WorkspaceRestorePermitScopeV2Test,WorkspaceRestoreSelectionClosureV2Test,DisposableRestorePlanFingerprintTest,DisposableRestorePlanTransactionGuardPostgresTest,DisposableFinancialTargetRehearsalPostgresTest test
```
Then trigger true all-four [full-ci] at same SHA, confirm Maven/JUnit/Postgres18, Web, Android and Compose *non-skipped* and Release/attestation exact SHA. Capture tests/failures/errors/skipped and first stack.

Specifically verify QA-055 preflight prevents illegal Testcontainers2 generic syntax; previous actual red #44273378444 compile and #44271716795 643 tests/12E are not current PASS. On failure route B tests/contract bugs to Backend B, C owner permit to C, A manifest proof to A, D economics to D.

## Immutable security invariant
Signed original A vNext member+category manifest and actual C reauthenticated owner grant remain absent, so B-B-08 code only creates/reviews synthetic source graph and C **descriptor** matching; it does not authorize production restore. Do not enable reset or restore flags. HTTP423, BETA HOLD.
