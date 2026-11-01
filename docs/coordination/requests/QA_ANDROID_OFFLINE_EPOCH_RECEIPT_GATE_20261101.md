# QA P1 — Android offline/dataEpoch + V48/V49 receipt recovery (2026-11-01)

## Status and scope

**OPEN: synthetic source tests not executed on exact HEAD, runtime behavior unverified.**
Current Web Playwright gate: 32/32 PASS as reported by QA operator, 59.0 s. This is **not** Android acceptance.
Android owner journal identifies HELD/CONFIRMED SQLite journal hardening, epoch verification after receipt GET, and activation requests requiring exact authenticated account permissions. These are source claims awaiting Gradle/HTTP evidence.

**Non-destructive scope:** use isolated disposable DEV identities; never wipe/execute reset on Test (14), Test2 (11), BETA or production. User postponed live-device tests: compile/unit and synthetic isolated-DEV checks are the priority; device acceptance remains open.

## Android acceptance matrix

| ID | Trigger / synthetic setup | Invariant / required observation | Evidence |
|---|---|---|---|
| AND-EPOCH-01 | Queue offline mutation at epoch N, then provide authoritative N+1 before reconnect | HELD/quarantined, **zero replay writes** and no success indicator | unit + HTTP capture |
| AND-EPOCH-02 | Change active family A → B with A pending | A's pending payload never posts under B token/id; B's independent data unaffected | coordinator unit + logs |
| AND-EPOCH-03 | Disconnect / timeout epoch GET | Fail closed for writes, no guessed epoch or background retry | mocked network |
| AND-EPOCH-04 | Resume at unchanged epoch N | Only explicitly authorized eligible pending operations can proceed; no duplicated rewards/points | idempotency test |
| AND-EPOCH-05 | Parent vs child role; revoked permissions | UI action disabled and authoritative Backend 403; no alternate path | unit + isolated DEV |
| AND-REC-01 | V48 TASK/REWARD confirmed POST loses response | Store original UUID, exact payload and family/epoch first; recover via receipt GET, **never issue second POST** | fault-injected coordinator test |
| AND-REC-02 | Receipt GET 404/timeout | Keep UNCERTAIN without silently generating new operation UUID | unit + fake transport |
| AND-REC-03 | Epoch changes after receipt GET but before journal CONFIRMED | Transition to HELD, not CONFIRMED; no replay | race regression |
| AND-REC-04 | SQLite journal row is HELD or CONFIRMED | Both terminal states reject stale PREPARED/UNCERTAIN update | repository unit test |
| AND-REC-05 | Nullable TIME price/duration and unknown JSON fields | Preserve JSON null (not 0) and unknown properties; reject invalid request server-side | DTO/editor unit |
| AND-RESET-01 | Open Settings, app restart, offline reconnect | Reset Preview may be read-only; **no prepare/confirm/challenge/execute** or implicit retry | request recorder |
| AND-RESET-02 | Client dataEpoch changed by family reset | Cached tasks/rewards/ledger invalidated; offline outbox quarantined until explicit recovery | state/store unit |

## Reproducible execution and gate

Use actual checked-out main SHA and JDK 21. Windows PowerShell from repository root:

```powershell
git rev-parse HEAD
cd android
.\gradlew.bat --no-daemon --stacktrace -PfamilyPointsCiAndroidSdk=36 :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

If the repository's Android root differs, adjust the working directory from its actual Gradle wrapper; do not claim tests ran because source is committed. Preserve the exact Git SHA, Gradle task result, counts, test XML reports, and failing stack traces. On CI independently confirm Android workflow success for **the exact SHA**; empty status lists are not success.

## BKL-002 architectural guardrails

Backend's previous exact green CI SHA b4f62fc9 (329 tests; 54 transactional point mutation checks) is not a destructive executor signoff. Source inventory covers 136 HTTP mutations, disposable PostgreSQL metadata covers 70 app tables / 156 FKs; neither approves a deletion closure. Pending decisions: immutable owner / legacy enrollment, shared global-account proof, retention of financial ledger and unfulfilled entitlements, Google fresh reauth, approved category combinations, atomic execute+audit+epoch+rollback, backup/restore retention and deployment migration parity. Keep execute blocked and test only on a disposable database after Architecture/Backend/Release approval.

## Next handoff

**Android owner:** run exact-head Gradle suite; report AND-EPOCH/AND-REC case-to-test mapping and failing tests, confirm feature flag and permission behavior. **Backend owner:** publish BKL-002 write-path/deletion closure and safety gate differences since prior CI proof. **QA:** audit outputs against matrix, keep missing device/runtime evidence OPEN, then rerun Web E2E gate after any shared client changes.
