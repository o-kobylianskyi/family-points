# Backend C → QA — C-C-03 proof-consumption security regression

**Source commit:** `39a1ece44ac38dae9117f9e0c876b9c1bbfca8a1`
**Test class:** `com.olehkobylianskyi.familypoints.service.WorkspaceResetProofConsumptionSecurityTest`
**State:** committed source, **NOT YET RUN**; no exact-head CI confirmation.

## Run
From repository root in IntelliJ terminal, after pulling latest `main`:

```powershell
git pull --ff-only origin main
./mvnw -Dtest=WorkspaceResetProofConsumptionSecurityTest test
```

If no wrapper, use `mvn -Dtest=WorkspaceResetProofConsumptionSecurityTest test`. Report exact `git rev-parse HEAD`, tests/failures/errors/skips and Maven BUILD result. If class fails, provide Surefire XML/log and original exception text; do not mark PASS from source inspection.

## Coverage
- Valid one-use proof only updates challenge consumption; **no Reset deletion**.
- Consumed token replay rejects.
- Expired token rejects.
- Current epoch mismatch and changed-password fingerprint reject.
- Cross-account, cross-member, malformed digest and wrong secret reject.
- Different workspace or challenge rejects.
- Concurrent consumed-at conditional update returning zero rejects (simulated race).

## Limits of evidence
Unit test uses Mockito; it does **not** establish PostgreSQL lock order, physical uniqueness constraints, independent two-transaction concurrency, controller authorization, original-owner provenance, rollback, migration safety or real Android offline behavior. Those require separate integration and operator QA. Backend C did not run Maven in the connector-only execution environment.

**Safety gate:** `POST /workspaces/{workspaceId}/data-reset/execute` remains HTTP 423. No DEV/BETA/PROD reset permitted.
