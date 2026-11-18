# Backend C → QA — C-C-05 PostgreSQL 18 Reset Proof concurrency

**Source commit:** `2119befd267e688d7d74d9f3e71eb1986cf2353a`
**Test:** `WorkspaceResetProofConcurrencyPostgresTest`
**State:** test source committed; NOT_RUN / exact-SHA CI not yet verified.

## Run

```powershell
git pull --ff-only origin main
git rev-parse HEAD
./mvnw -Dtest=WorkspaceResetProofConcurrencyPostgresTest,WorkspaceResetProofConsumptionSecurityTest,ResetChallengeAttemptLimiterTest test
```

If Maven wrapper unavailable, use installed `mvn`. Requires Docker with PostgreSQL 18 Testcontainers. Report exact SHA and Surefire class counts/failures/errors/skips. If a test fails, provide full failure stack and relevant Surefire XML; no production calls.

## Database-backed proof

Unlike prior Mockito tests, C-C-05 migrates a **disposable PostgreSQL 18** database via current Flyway, seeds two workspaces and a test-only BCrypt account/member, prepares a confirmed nonexecutable Reset proposal and issues a real `WorkspaceResetProofService` one-use proof inside a Spring transaction.

1. Two threads concurrently attempt to consume the **same** proof with independent database transactions. Exactly one must commit; second returns conflict. No epoch changes or tenant writes.
2. Rollback after successful in-transaction consumption restores unused proof; a retry may commit once and further retries fail.
3. A cross-workspace attempt must fail with no proof consumption or neighbor epoch changes.
4. Persisted family epoch increment invalidates prior proof (stale request) without consuming it.

## Scope and release
- Test does NOT call or enable the locked HTTP Reset Execute endpoint; never deletes family data.
- It is a PostgreSQL locking and proof lifecycle test, **not** a signed-off owner-binding, production Reset or Restore executor, actual device/client E2E, or full schema upgrade matrix.
- Owner-binding migration and controller policy still require Architecture/Integrator ACK.
- BETA HOLD and HTTP 423 remain mandatory; real family reset is not authorized.
