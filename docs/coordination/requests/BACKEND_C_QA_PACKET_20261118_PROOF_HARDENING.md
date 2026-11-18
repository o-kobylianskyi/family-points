# Backend C → QA / Integrator — C-C-04 proof hardening

**Source commits:** `1534d864ec167bdc10acd81ccf57949ad9fadadf` (production integrity guard), `fbbae49788bd23771e574464fda5047faa930b32` (proof negative tests), `e133438cb8654e3fe864abad83674986af01b26e` (rate limiter tests).
**Status:** CODE COMMITTED, TESTS NOT RUN. **Production data-reset Execute remains HTTP423.**

## Targeted command
```powershell
git pull --ff-only origin main
git rev-parse HEAD
./mvnw -Dtest=WorkspaceResetProofConsumptionSecurityTest,ResetChallengeAttemptLimiterTest,WorkspaceResetChallengeControllerTest test
```
If no Maven wrapper, use `mvn -Dtest=WorkspaceResetProofConsumptionSecurityTest,ResetChallengeAttemptLimiterTest,WorkspaceResetChallengeControllerTest test`.

Report exact SHA, per-class tests/failures/errors/skips, Java BUILD result and any Surefire errors. Do not call source-only test code PASS.

## Changes & expected results
1. Persisted challenge `verifier_hash` missing, invalid length or invalid hex => controlled `409 RESET_FRESH_CHALLENGE_REQUIRED`, no mutation; no unexpected server null pointer.
2. Missing persisted actor account, epoch, member ID, digest or credential fingerprint => controlled 409 without consuming proof.
3. Valid proof still consumes only once; expiry, credential changes, wrong tenant/actor, stale epoch and replay remain rejected by existing guards.
4. Reauth limit mock tests: 1..5 allowed; sixth denied by controller; null SQL count => fail closed as 6; DB error propagates instead of allowing; tenant/account composite keys stay separate.
5. **Not proved by Mockito:** PostgreSQL row-lock concurrency, transaction isolation, 15-minute rolling window under real DB time, rollback of attempts, frontend HTTP flows and original-owner identity.

## Unresolved P0
- Canonical original-owner provenance contract `BACKEND_C_OWNER_PROVENANCE_CONTRACT_20261118.md` still needs Architecture ACK; isolated `ResetOriginalOwnerPolicy` is not yet wired into controllers.
- Confirm whether rate-limit policy should count successes; present implementation counts every challenge attempt and may throttle legitimate repeats. Do NOT loosen brute-force protection without agreed policy.
- Need live PostgreSQL integration concurrency and exact-head CI before any execution unlock. No real workspace reset or restore permitted.
