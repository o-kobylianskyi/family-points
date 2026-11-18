# FamilyPoints — Web Frontend ACTIVE QUEUE (on every `++`)

**Read** [Architecture dispatcher](../ARCHITECT_DISPATCH_LATEST.md). The owner only types `++`; this work order is fully stored in GitHub. Work asynchronously, independently of Backend B and Backend Retention. Re-check `main` before each write because other streams commit concurrently.

## File ownership
Own `frontend/` Web UI and Node tests and `qa/e2e/tests/onboarding-v49-*.spec.js` **only for Web-V49 fixture/selector maintenance**; avoid modifying shared backend, DB migrations, RESTORE security, QA-owned source-loss test files, and Release CI. QA owns final independent browser acceptance and manual test execution. If a Web change needs a REST API change, report contract/expected payload to Architecture, DO NOT silently edit backend.

## WEB-01 — V49 two-tab finalize safety, one coherent package
Existing evidence in `WEB_LATEST.md`: V49 two-tab synthetic Playwright added, but truly authenticated Chromium two-tab QA remains unverified; a previous Web package addressed backup metadata account-switch identity isolation. **Do not duplicate existing source improvements**. Inspect current `frontend` source, `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js`, GitHub source/CI; fix concrete issues in one meaningful package: single finalize POST with stable original UUID despite two tabs, lost response/receipt recovery, storage state/account switch, stale epoch/revision, competing tab marker ownership, i18n and unintended writes. Favor existing code/test assets and bounded unit/synthetic run; do not invent new UI features or rewrite API.
**Do not block for B-INT-02**: V49 work is independently runnable against documented existing current API. If no actual defect is found, do not introduce speculative code; produce clear `WEB-01_READY_FOR_QA` with SHA, exact test command, confirmed/fixed cases and remaining browser/operator acceptance need. QA runs formal Chromium when Architecture activates QA after focused handoff or B readiness.

## WEB-02 — pre-authorized follow-on after WEB-01 (no new Architect instruction)
Audit the existing **account-scoped backup UI** and onboarding/backup isolation (per latest prior Web package): account/workspace remount, metadata state on auth switch, loading/retry, read-only inspection when `restorable=false`, safe display of unavailable Restore/Reset. Fix any confirmed Web-only defect as ONE large independent package with direct regression. Do not integrate new RESTORE endpoint while B-INT-02 contract is unfinished or change production feature flags. Post `WEB-02_READY_FOR_QA`.

## WEB-03 — optional after WEB-02
Review the static catalog/template editor/search/filter state for existing supported functionality and locale UA/RU/DE, only fix independently verified release-blocking Web defects within existing Web files. NO feature expansion, no overlapping Android task. If no actual defect is evidenced, mark `NO_WORK` and WAIT, do not create docs-only churn.

## Automatic next / acceptance
On each `++`: continue first unfinished item in queue; when coherent package is finished, post one issue handoff and **continue next independent pre-authorized task on the next `++`** without needing Architecture. If current task has no more runnable work or requires QA/Backend, record blocker and move to next independent pre-authorized item. Never mark QA PASS yourself without independent proof. Keep changes atomic; no full expensive all-component CI per microcommit.

**Manual operator test requests** must be separate `OPERATOR ACTION REQUIRED` to Architecture and QA, disposable environment only. Do not ask owner to follow instructions in ordinary `++` update. No production rollout; BETA HOLD.
