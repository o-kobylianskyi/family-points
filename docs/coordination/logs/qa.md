# FamilyPoints — Testing / QA journal

**Track:** QA · **Created:** 2026-10-24 · **Scope:** existing interim-BETA stabilization only

## Operating method
- At each user request "аналіз", "що тестувати", "далі", check `origin/main`, latest CI, open PRs #4/#5/#8 and logs of Backend/Web/Android/Release, and confirm whether actual deployed BETA server and APK versions are known. Update [live board](../QA_TEST_BOARD.md) with precise freshness and dependencies.
- Show the user no more than 3–5 top actionable tests at a time, with location, roles/accounts, steps and expected output; no deployment or unverified production data writes. Use synthetic test workspace/member/accounts rather than real family spending where practical.
- Use test statuses: `READY_TO_TEST`, `BLOCKED`, `NOT_VERIFIED`, `PASSED`, `FAILED`, `NEEDS_RETEST`. A manual test cannot become PASSED from CI or code inspection alone.
- When bug found, record reproducible issue here and route it to existing owner (B/W/A/R), with severity, exact BETA SHA/APK version, error HTTP/logs (secrets removed), screenshots and expected-vs-actual. Once owner fixes, require a new run on deployed fixed version; then close.

## Initial snapshot — 2026-10-24
- GitHub source inspected for QA setup. Most recent source/CI/workflow state is changeable; current actual VPS/APK not checked.
- Key P0: approved child reward request → purchase exactly once, 20-point debit, no unexpected logout. Backend current main has Flyway V35 reward purchase constraint fix plus a repeat-purchase conflict check; runtime deployment unverified.
- Key P0: login and transient Backend error Android session retention, offline sync without doubled points, multi-workspace isolation.
- Key P0: signed release APK on physical handset, DB V35 reward and candidate V36 calendar/V37 media migration, backup/restore, real BETA smoke; Web forms and Android pull refresh as regression.
- See QA_TEST_BOARD and QA_TEST_CASES. No test execution or pass claimed.


## 2026-10-24 — INCOMING Web package WEB-SF-01: QA assignment
- Web formally transferred the complete Soft Family stabilization visual regression suite to QA: `docs/coordination/QA_TEST_CASES.md` (WEB-SF-01), tracked on `docs/coordination/QA_TEST_BOARD.md`. Source handoff commit `d206fde`; board registration `a3e752e`.
- **QA action required:** check actual deployed Web SHA against tested source; then execute full test matrix light/dark × desktop/mobile × four locales, all existing main flows, group/task/reward dialogs, shared numeric fields, focus/scroll, and approved Home no-change. Record PASS/FAIL by case with screenshot/viewport/browser/theme/locale/exact SHA; no automatic PASS from build results.
- Provide one consolidated defect report to Web owner. Web should fix all returned defects in one batch and re-submit to QA; repeat until verified green. Report blockers where runtime or accounts are unavailable; do not claim tests were run without evidence. Scope frozen before interim release, no new feature inclusion.
