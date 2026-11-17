# FamilyPoints Web — rolling latest handoff

**New role chat:** FamilyPoints — Web Frontend (WEB-02), pickup 2026-11-16 Europe/Berlin after old chat reached hard cap. **Repo:** `o-kobylianskyi/family-points`; integration branch `main`. **HEAD inspected at pickup:** `e51c0972aa3dd7a2dd5960cb5e450b40bae4750c` (2026-11-16 20:25Z). Other tracks push concurrently: re-fetch HEAD before every edit. Old emergency handoff: [20261116_WEB_CONTINUE_AFTER_LIMIT.md](20261116_WEB_CONTINUE_AFTER_LIMIT.md).

## Source and decisions checked
- Read `AGENTS.md`, `docs/coordination/README.md`, chat-capacity pilot, Web journal, Web QA packet and issue #18/#20. Release status must not be inferred from commits alone.
- Source checked: `frontend/src/utils/setupFinalizeSafety.js` and `frontend/tests/setupFinalizeSafety.test.mjs` (current file content fetched at pickup); `frontend/src/utils/setupCatalogSelection.js`, its node test file, and `frontend/package.json`.
- Latest Web/Designer commits after prior screenshot: `2717222` and `442a458` guard step PATCH epoch/revision/step; `cb5c44f` centralizes repeated-step guard; `5c969dc` covers filtered partial deselect. **Do not duplicate these.**
- CI optimization issue #20 adds `npm run test:regression` and lint to main CI. Full same-SHA jobs should be confirmed through Release with exact run ID and executed steps.

## WEB-02 package 1 (2026-11-16)
- **Source commit:** `e5b66b4df4dda057c40316fc0abfbcf4cb5eff25` removes dead `allMatchingSelected` derivation from `InitialFamilySetupReview.jsx`, now that the actual Deselect matching button correctly uses `hasSelectedSetupMatch`. No API, UX or security semantics intentionally changed. This cleans an obsolete computed state in the regression area.
- **Validation:** source-only inspection, CI/Node/Vite/lint NOT_RUN in this chat; same-SHA CI awaits observed jobs. Review commit on current HEAD before additional edits.
- **Continuity:** rolling handoff originally created `bc84f6ef`; capacity checkpoint `4650a470`. This revision reflects new source SHA.

## WEB-02 package 2 — V49 PATCH invariant regression (2026-11-16)
- Read actual `frontend/src/api/setupApi.js`, `frontend/src/utils/setupFinalizeSafety.js`, node safety tests, and synthetic browser specs `onboarding-v49-epoch-race-synthetic.spec.js`, `onboarding-v49-loading-retry-synthetic.spec.js`, `onboarding-v49-finalize-recovery-synthetic.spec.js`. These synthetic specs already cover epoch-stale, 503 retry, and four finalize receipt outcomes. **Do not repeat them.**
- Found unguarded legitimate-HTTP-but-invalid step response: same target step/epoch/revision could unexpectedly change other decisions or prematurely return setup COMPLETED. Source fixes `c36dacd`, `87d34f4` require successful PATCH to be IN_PROGRESS and leave existing other decisions untouched; lost-PATCH GET reconciliation returns UNCONFIRMED on same violations. New explicit PENDING entries remain allowed. Tests added `2b706f4`, `6d8f829`.
- **Latest new source/test SHA:** `6d8f8293a755e10be0833a65c848603b71d48cfd`. All four commits on `main`. Validation **NOT_RUN** (GitHub connector writes only, no repo runner attached to this chat); CI same-SHA **NOT_VERIFIED**, no PASS claims. QA can run automated synthetic Playwright alongside broader suite; QA owns real disposable workspace E2E.
- Next: inspect feedback from actual same-SHA CI/source tests, resolve any detected failures, then check cross-tab localStorage race and existing source readiness. No real-family changes, BETA HOLD.

## WEB-02 package 3 — cross-tab finalize marker ownership (2026-11-16)
- HEAD checked at pickup: `15c0c62e801aac7e22af12d01445230c2821468b` (other tracks may have newer commits).
- Examined `frontend/src/components/InitialFamilySetupReview.jsx`, `setupFinalizeSafety.js`, safety unit tests, shared `api/client.js`. Found that staged finalize markers were not rechecked immediately before the one POST after competing tab changes.
- Source: `c60f6d3` defines `ownsSetupFinalizeMarkers(session, shared, operation)` requiring both markers to match all immutable fields; `2ab9791` validates immediately after staging and before finalize dispatch. On mismatch fail closed, preserve markers rather than deleting a competing tab's operation.
- Regression `c731a06b8ae518daf8f87e91d17c65ff60f1f931`: tests same/foreign UUID, account, workspace, epoch, revision, ordered codes, absent and blocked markers. QA synthetic browser adversarial scenarios appended to `WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md` commit `bed5098`.
- **Status:** source/test committed, no Node/Playwright/browser execution or same-SHA Actions evidence in this chat (**NOT_RUN / NOT_VERIFIED**). Frontend browser marker check is non-atomic across tabs; Backend revision/idempotency/dataEpoch remains final authority. No real-family activity, BKL-002/BETA HOLD.
- Next: obtain actual test/CI evidence, fix failures as found, add an automated synthetic two-tab browser test if QA coverage missing, review mark-clearing races conservatively.
- Rolling handoff updated in this batch; last inspected HEAD after source packet: `bed509893f65e0a13699594f0599b44e238beda2`. Re-read main on next pickup.

## WEB-02 package 4 — two-tab synthetic Playwright (2026-11-16)
- Startup HEAD `09a7a9e6b556545c6d5454f10f49563125f3b0ec`; source/test at `fe64edb351ea5c30fce080c8385129ca6c99452e`.
- Added `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js`: two real browser tabs in one Playwright context, mocked all setup/data-epoch/catalog requests after QA login. Case A: tab A loses finalize response; tab B sees shared pending, cannot dispatch another finalize, GET receipt 404 keeps same UUID. Case B: tab B changes shared UUID during tab A's receipt lookup; tab A must preserve competing marker and remain blocked. No actual setup writes.
- Initial commit `381f3f4`, test injection ordering corrected `fe64edb` so foreign marker is inserted **during in-flight receipt recovery**, before cleanup (not before clicking a now-disabled receipt button).
- **Execution:** NOT_RUN; browser runner unavailable in this GitHub connector-only session. CI actual same-SHA jobs NOT_VERIFIED; no PASS claim. Needs QA to run on a pinned SHA in the consolidated synthetic campaign. Avoid assuming authenticated second tab shares sessionStorage; this is one browser context with shared localStorage, so auth/selected workspace precondition is checked.
- Next: inspect QA test output for selectors/auth and race timing, fix failures, then inspect CI complete/scoped job evidence and remaining Web release defects. BKL-002/BETA HOLD remains.

## WEB-02 package 5 — GitHub Actions evidence audit (2026-11-16)
- Current `main` at audit entry: `b415039a84517d602dbbf089f13ba89de42b70ca`. Four-component run IDs obtained from Release journal and verified directly using actual GitHub Actions **job step** results:
  - [43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960): Web regression/lint/i18n/build executed SUCCESS, Android executed SUCCESS, Compose executed SUCCESS; Backend FAILED, full attestation FAILED.
  - [43953661904](https://github.com/o-kobylianskyi/family-points/actions/runs/43953661904): Web regression/lint/i18n/build executed SUCCESS, Android SUCCESS; Backend FAILED; Compose PowerShell parser FAILED, subsequent Compose tests SKIPPED; attestation FAILED.
  - [43953986968](https://github.com/o-kobylianskyi/family-points/actions/runs/43953986968): same Web/Android executed SUCCESS; Backend FAILED, Compose parser FAILED, attestation FAILED.
- These runs **predate the later two-tab browser spec `fe64edb` and are not a current HEAD proof**. Playwright test is not covered by the Web Node regression job: V49 two-tab synthetic remains **NOT_RUN**. No new Web runtime defect has been confirmed from these runs, therefore no speculative source rewrite.
- Release owns Compose parser and CI gate; Backend owns PG test failures. QA owns actual browser synthetic run with pinned SHA and real disposable V49 acceptance. BKL-002/BETA HOLD remains.
- Next Web priority: reconcile actual two-tab Playwright trace/results when available; preserve single-POST and pending markers, fix only reproduced defects; verify a newer exact-SHA CI run with real executed steps.

## WEB-02 package 6 — V49 readiness evidence audit (2026-11-16)
- Source intake HEAD `03a9de2a1788b18c2434312783efc68e7d6332d6`. Read latest QA Web historical 47/47 output and QA handoff QA-025. The 47/47 operator result is not immutable same-SHA with later V49 and two-tab code; no real current-head acceptance evidence.
- Published [WEB_V49_READINESS_AUDIT_20261116.md](../requests/WEB_V49_READINESS_AUDIT_20261116.md) `75ed5265`, evidence matrix and exact follow-up automated QA procedure; Web source/tests remain COMMITTED but two-tab Playwright NOT_RUN. Historical Actions Web/i18n/lint/build 3 runs PASS; no newer same-SHA current evidence.
- **No verified Web defect was reported in QA.** Therefore no speculative behavior refactor in this batch. QA owns pinned Node+Playwright and operator-only real disposable family acceptance; Web fixes reproducible defects when received. Backend/Release BKL002 and exact full-gate separate; BETA HOLD.
- Next: consume QA exact-SHA outcomes/Playwright traces, fix confirmed Web regressions and rerun targeted tests; keep handoff current.

## WEB-02 package 7 — verified newer complete CI, QA V49 status (2026-11-17)
- Independently fetched actual GitHub Actions jobs for [run 44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962), exact source SHA `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a` as independently recorded by QA. Web locale check/source-backed regression/lint/Vite build: all executed **SUCCESS**. Backend Maven, Android Gradle, Compose parser/release safety, Full CI exact-SHA evidence and CI outcome reporting: all jobs executed **SUCCESS**. QA log records Maven 479 tests with zero failure/error/skip and 55 Release synthetic cases, from actual logs.
- Newest sampled moving main at audit pickup `6724c186d929bc45fa9e069f45cfffc9162c70c1`, later than green source SHA. Do not call this an exact-main PASS; subsequent Backend #25–#27 source tests have no observed matching full CI.
- QA independently source-reviewed `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` (QA-031), but **did not execute Chromium Playwright**. Same applies to operator-authorized real feature-enabled seven-step V49 acceptance. New Web-specific verified defect: none in observed CI. QA owns test execution, Web fixes any reproduced trace. BKL-002 protected reset/restore and Beta release remain HOLD.
- Next Web work: consume QA exact-SHA two-tab fixture results, audit test fixture reliability and correct confirmed failures only; coordinate with Release for new same-SHA non-skipped CI following Backend source commits.

## WEB-02 package 8 — V49 two-tab Playwright source parser fix (2026-11-17)
- Source reviewed QA-031; no executed V49 two-tab browser result yet. Independently re-read `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` and identified **28 doubled backslash pairs** in regex literals, e.g. `/\\\\/login/` and `/7\\\\/7/`, generated by previous escaped transport. These prevent intended JS regular expression parsing/matching. Corrected in `a0d87ee52ebed8b1e62903a910fc2bb010999189` by reducing each doubled pair to a single regex escape, without changing request mocks, UI or Back-end behavior.
- Browser test execution **NOT_RUN** here: QA must run `node --check qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` and the actual two-tab Playwright suite on pinned SHA, then capture PASS/FAIL/SKIP, trace and network POST count. Prior read-only source inspection is not an E2E PASS; the new change fixes a source-level fixture defect.
- Latest independently observed full CI run [44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962) PASS on older `8c397d0`, not a current HEAD + browser suite PASS. BKL-002/BETA HOLD remains.
- Next: inspect QA test run; fix environment/auth or browser race only with real failure evidence, then target completed V49 acceptance.

## WEB-02 package 9 — two-tab fixture syntax validation (2026-11-17)
- Latest main at pickup `875720e961a154ec1b7a4d3be36b1dd2d5ee2ca1`. QA-032 [handoff](../handoffs/QA_LATEST.md) confirms review of Web regex fix `a0d87ee`, no executed Chromium result.
- Independently fetched corrected `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js` blob `761d10edd052cb31a0a27f50b7ba974661ad3332`. Validated with JS V8 `new Function` parser after replacing top-level imports and `import.meta` **for syntax parsing only**: **PASS**. Not an actual Node module check, Chromium browser run, or V49 acceptance.
- No confirmed UI/runtime failure. QA remains execution owner; avoid extra manual/operator test requests. No source modifications in this checkpoint. Last all-four full CI PASS is historic run `44044271962` at `8c397d0`; newer Backend commits require new exact-SHA job evidence.
- Next: consume QA two-tab test PASS/FAIL/SKIP + failure trace or establish isolated browser test harness under QA; investigate real failures only. No BETA/PROD or family data, BETA HOLD.

## Implemented vs verified
- Implemented in source: seven-step V49 wizard, 142 catalog/6 packs, parent-gated review, durable original finalize UUID, GET-only recovery after lost replies, cross-tab mismatch blocking, no implicit writes; step epoch/revision protection.
- Historic user-side Playwright evidence: 3/3 read-only synthetic and 2/2 finalize-recovery synthetic PASS, **not pinned to current HEAD**. Old screenshot reports Web node source 31/31 PASS on `758dfe4`, Vite PASS, lint 4 warnings/0 errors; **historic chat report, not rerun here**.
- This pickup performed **source inspection only**. Node/Vite/Playwright and current SHA Actions **NOT_RUN / NOT_VERIFIED**. Do not call CI green until inspecting actual jobs and steps.
- Real feature-enabled V49 E2E: **QA-owned, pending** approved disposable workspace. QA packet: [WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md](../requests/WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md) and Designer's consolidated campaign.
- Security gate **BKL-002 HTTP 423/BETA HOLD**: no PROD/BETA deploy, no destructive reset/restore/execute or writes to actual families.

## Next safe Web package
1. Inspect current V49 UI, `setupApi`, and Playwright synthetic specs (epoch race, GET 503 retry, wrong receipt revision/epoch) for scenario holes. No duplicate Web/Designer fixes.
2. Add a cohesive bounded Web source/test regression package for an actual uncovered defect; run `cd frontend && npm run test:regression && npm run lint && npm run i18n:check && npm run build` on exact SHA if a runner is available. Otherwise mark NOT_RUN; route automated Playwright run to QA with exact SHA.
3. Hand real multi-tab, role ACL, lost-response, selective-import and mobile/theming acceptance to QA only; operator tests stay aggregated there.
4. Inspect GitHub Actions full vs scoped steps on exact candidate SHA; separate current HEAD and test SHA. Check issue #18 and #20.
5. Every significant batch refresh this rolling handoff plus `chat-capacity/WEB.md` and track journal; keep files self-contained. Do not promise precise ChatGPT context percentage.

## New chat opener
```text
Продовжуємо FamilyPoints Web після перенесення чату. Прочитай AGENTS.md,
docs/coordination/handoffs/WEB_LATEST.md, docs/coordination/logs/web.md
та перевір новий main SHA GitHub. Продовжуй V49 source+synthetic regression,
передавай всі реальні E2E QA. BKL-002/BETA HOLD. Після кожного пакета
оновлюй rolling handoff і chat-capacity WEB log, не вигадуй % ліміту.
```
