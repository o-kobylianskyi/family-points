# Backend B — durable B-INT-02 execution checkpoint

**Purpose:** prevent restarting from zero after timeout/context interruption. This file is updated by Backend B on meaningful implementation checkpoints, not per microstep. Follow [BACKEND_B_ACTIVE_ASSIGNMENT.md](BACKEND_B_ACTIVE_ASSIGNMENT.md).

## Seed status (Architecture, 2026-11-18 21:28 CET; not an implementation claim)
- **Active scope:** B-INT-02 full end-to-end portable Restore as defined in active assignment.
- **Latest B-INT-02 actual implementation checkpoint:** NOT YET REPORTED. At Architect's most recent repository inspection, newer commits were Retention/Web/QA/coordination only; that is NOT proof that no uncommitted/off-branch work exists. Backend B must check current GitHub refs and actual source at next `++`.
- **Last known existing baseline:** signed FPB1 v2 8-category original roles/members + history source capture and offline inspector / C binary source digest contract, code predating B-INT-02. Do not rewrite it without actual defect.
- **Unverified as completed:** authoritative new-family original-owner registration, provider/password fresh reauth and durable RESTORE-only grant, true transactional target restore/financial parity/epoch/receipt and complete source-loss-to-target E2E. Treat as remaining until source evidence proves otherwise.
- **CI:** QA prior source-history 763/763 non-skipped at QA PR `425fbc8e`; does not certify B-INT-02/current HEAD.
- **Safety:** production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, retention physical purge OFF, BETA HOLD.
- **NEXT source action:** inspect existing owner/permit/target production classes and migrations, identify actual missing integration seams, then implement the largest safely integrated owner enrollment+reauth+one-use-RESTORE-permit vertical slice and continue native target/finance within B-INT-02, persisting coherent source commits before timeout. Do not create another planning-only loop.

## Backend B: overwrite only the values below upon real code progress

| Field | Value |
|---|---|
| Last updated (CET) | NOT_REPORTED |
| Current verified HEAD / implementation SHA | NOT_REPORTED |
| DONE (path → commit SHA, observed behavior) | NOT_REPORTED |
| IN_PROGRESS (smallest incomplete coherent slice) | B-INT-02, not yet verified |
| NEXT (exact source file/test to implement) | Production owner enrollment / reauth / durable RESTORE permit seam |
| BLOCKED (contract/source error, alternative) | NONE_VERIFIED |
| Targeted tests executed (SHA, class, pass/fail/skip) | NONE_VERIFIED |
| Last long-running/uncertain action | NONE_REPORTED |
| READY_FOR_QA exact SHA / supported scope | NOT_READY |

**Timeout recovery rule:** Check whether the previous GitHub commit or test call already succeeded before retrying. Source evidence outranks stale checkpoint fields; never repeat completed changes. Update once per meaningful committed batch; do not request new owner instruction between internal B-INT-02 steps.


## 2026-11-18 — actual Backend B checkpoint / B-NEXT-01 after frozen B-INT-02

**DONE — B-INT-02 (source ready for independent QA, NOT release):** frozen PR #30 SHA `b711371d6c78d77f1550002261ff0b6ac4bd941b`, `READY_FOR_QA` Issue #15 comment 6991088205; full [CI #4590](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759) 770 Maven tests 0F/2E/0S due independently owned Retention fixture. B-native two-PG18 encrypted source loss + transactional replay 1/1 PASS, creator provenance 2/2 PASS, Web/Android/Compose PASS. Release remains HOLD; no overlap edits permitted to PR #30 QA files.

**CURRENT / B-NEXT-01:** branch `backend-b/b-next-01-compat-readonly`, source SHA `bfa2f5dce8f6f02a5d43b1dff4a5d183f62b08aa`, [Draft PR #31](https://github.com/o-kobylianskyi/family-points/pull/31). Added *new independent* `WorkspaceBackupCompatibilityAssessmentService` + seven-case `WorkspaceBackupCompatibilityAssessmentServiceTest`. Read-only metadata registry contract (tenant/snapshot/expiry/verified but never executable), legacy V1 existing inspector (blocked for owner/identity/native replay), authenticated eight-category V2 existing inspector (no owner permit implied). No API/HTTP/schema/production feature change or frozen B-INT-02/Retention/Web edits.

**CI actual status at checkpoint:** [#4604](https://github.com/o-kobylianskyi/family-points/actions/runs/44395020403) exact `bfa2f5dc`: Web, Android, Compose successful; Java 21 Testcontainers **in progress**, compilation/testCompile steps completed; no final Maven test result yet. Do NOT claim B-NEXT-01_READY_FOR_QA until actual Java result. NEXT: inspect CI #4604 completed Java logs and B-NEXT-01 test (7 cases); fix any B-owned error on PR #31 only; then single B-NEXT-01_READY_FOR_QA if green. If QA sends B-INT-02 P0, preempt next tasks. Production restorable=false, replaySupported=false, Reset HTTP423, physical purge OFF, BETA HOLD.
