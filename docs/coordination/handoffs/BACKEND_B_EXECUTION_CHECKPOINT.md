# Backend B — DURABLE EXECUTION CHECKPOINT (current verified state)

**Last Architecture review:** 2026-11-18; read [architect dispatcher](../ARCHITECT_DISPATCH_LATEST.md) and [B active task](BACKEND_B_ACTIVE_ASSIGNMENT.md) on each `++`. This checkpoint replaces the original seed; source and current GitHub PR/CI override this file if newer.

## DONE — frozen B-INT-02 implementation (not QA accepted)
- Draft [PR #30](https://github.com/o-kobylianskyi/family-points/pull/30), branch `backend-b/b-int-02-integrated`, exact source **`b711371d6c78d77f1550002261ff0b6ac4bd941b`**. PR remains **UNMERGED** and GitHub currently reports `mergeable=false` against moving main; investigate integration/rebase conflict only on a new non-destructive QA integration candidate, never rewrite frozen PR SHA without coordination.
- Implemented bounded signed original-owner/reauth/RESTORE permit and native atomic target restore; supported scope and unsupported BLOCK categories: [canonical B handoff](../requests/B_INT_02_READY_FOR_QA_20261118.md).
- Executed on frozen PR SHA: [Actions #44392551759](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759), Maven PG18 **770 tests / 0F / 2E / 0S** (two Retention-owned fixture SQL CHECK errors), B native source-loss E2E **1/1 PASS**, original-owner PG18 **2/2 PASS**, Web/Android/Compose PASS. **Full all-component CI RED**. Not production ready.
- Retention owner already corrected its fixture SQL error on main at commit `a0210961687`; it is **not part of PR #30 SHA**. No combined exact-SHA full green verified.

## IN_PROGRESS / NEXT
- **Primary QA gate:** Architecture has activated QA for independent PR #30 B-INT-02 acceptance plus combined Retention-fixed integration candidate. Await one consolidated defects report; protect frozen PR code and no accidental source changes in its QA lane.
- **Backend B independent NEXT:** B-NEXT-01 read-only backend compatibility in separate nonoverlapping code/PR. Latest main commit `f82b59380155` documents this work; inspect actual branch/source before claiming it complete. On QA confirmed B-owned P0/P1, prioritize integrated B-INT-02-FIX and then resume NEXT.
- **Retention** separately owns Retention tests/service; no Backend B changes there. 
- **Blocked release:** full non-skipped combined CI pass; independent B package acceptance; unsupported category paths remain fail closed; BETA HOLD. No production Restore/Reset/destructive purge.

## Timeout-safe resume
1. Fetch PR #30 state, newest issue #15 comments and relevant branch/main commits. Determine DONE from actual code SHA, not text alone.
2. Do not repeat implemented B-INT-02. Resume B-NEXT-01 from last real source commit, or correct a new B-owned QA defect as one batch.
3. Before retrying a timed-out GitHub operation or CI run, look it up to avoid duplicate commits/runs.
4. Record next meaningful code checkpoint here with last SHA, exact affected files, tests PASS/FAIL/NOT_RUN and the NEXT action. Do not create a docs-only loop per microstep.

**Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, physical purge OFF, BETA HOLD.
