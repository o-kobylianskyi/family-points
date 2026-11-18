# FamilyPoints — DESIGNER rolling continuation (LATEST)
Updated 2026-11-16, Europe/Berlin. Coordination owner: Designer track. **Read this before continuing in a replacement Designer chat.**

## Source and evidence
- Last verified source baseline in coordination: current main was `1ce69bbe9e5f2573eb952aa415c7bb83ac59b65f` at this handoff's creation; moving branch must be refreshed. Designer's latest functional code package ends at `5c969dc9194502261c9f0feaf047ebaa7d9c0c30`, subsequent comparison through `19adfe1b7` showed **no frontend source changes**.
- **Web CI PASS** at `19adfe1b7`, GitHub Actions [run #43947907960](https://github.com/o-kobylianskyi/family-points/actions/runs/43947907960), Web job `133582551196`. Independently inspected actual job steps: install, i18n check, source-backed Web regression, lint, frontend build all SUCCESS. Design V49 changes precede this SHA. This is scoped Web acceptance only, not whole release green.
- In same run, Backend Maven/Testcontainers job FAILED (460 tests, 1 assertion failure, 1 error), full attestation FAILED. See issue #15; backend owner to fix. Do not report Full CI PASS. Earlier full all-four green run #43934619348 at `3c0cf014` is immutable historical evidence and precedes recent Designer source.
- Exact-source tests run by Designer locally: **none**; success is GitHub Actions verified on the tested SHA, not self-executed local tests. Real V49 persistent E2E still OPEN.

## Owned implementation / current completed scope
- Designer's Web V49 read-only catalog UX and safety improvements: correct error and pack labels UA/RU/DE/EN, disabled repeat step decision PATCH, selected-count a11y, keyboard/mobile wrapping, epoch-safe PATCH success, GET-only load retry, partial bulk deselect, zero-results copy, new pure regression tests. Last functional Web commit `5c969dc919`.
- Android new-family route to V49 read-only status (not native editor), four locale copy, cancellation and schema checks, accepts initial empty/partial step map while requiring seven terminal steps for COMPLETED; test `WorkspaceSetupStatusStateTest`. Android P0 independent 98/98 JVM PASS run #43893026715 historical; newer Designer Android changes must not inherit that specific run result.
- Full authenticated V49 seven-step editing/import completion is QA/Web/Backend-owned, not confirmed by Designer. No auto activation or ledger writes from read-only screen.
- No BKL-002 execute/reset/restore, no existing-family or PROD/BETA mutation authorized. Keep BETA HOLD pending Backend protective snapshot/restore and QA approval.

## Human-in-the-loop real QA
User explicitly requires all **real** acceptance testing be conducted together with QA, not Designer. Canonical [Designer→QA acceptance guide](../requests/DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md); registered in `docs/coordination/logs/qa.md` and issue #18. Only isolated disposable workspace with explicit scoped consent; never touch existing families. QA collects SHA, HTTP counts/original UUID, pass/fail/skip, browser/device and redacted evidence.

## Coordination protocol
- GitHub Push CI cannot be discovered reliably through connector `fetch_commit_workflow_runs` (PR-only) or `get_commit_combined_status` (empty even when Actions PASS). Look up documented run IDs from issues/logs; query run jobs and named steps. Empty results ≠ no CI. Use `docs/coordination/CI_EVIDENCE_LOOKUP_PROTOCOL_20261116.md`.
- Chat-capacity policy [pilot](../CHAT_CAPACITY_OBSERVABILITY_20261116.md): **no trusted current tokens/remaining percentage counter**. User requested 50/75/85% warnings, but they cannot be truthfully guaranteed. Record unknown as unknown. Update `docs/coordination/chat-capacity/DESIGNER.md` with observation provenance after each substantial Designer batch; refresh this self-contained handoff at same time.
- Coordination Issue #18: Designer/QA/Web V49 UX + safety; issue #15 Backend BKL-002; #19 Android; #20 CI observability. Read latest notes before acting.
- Don't append marginal cosmetic commits while release gating. Freeze new Designer features until QA finds concrete UX defects. Do not ask user to perform automated tests manually when independent CI evidence already exists.

## Next priority
1. **DESIGNER-QA-SUPPORT:** leave Designer in maintenance mode; wait for QA's operator-assisted V49 acceptance defects, fix concrete bugs only.
2. Refresh current HEAD CI evidence when a new **release candidate** is pinned; latest known scoped Web PASS `19adfe1b7` but full gate FAIL Backend. Do not misstate latest HEAD green.
3. Continue user-requested honest chat-length evidence checkpoints and handoff freshness. No invented percentages.

## New-chat bootstrap
"Continue FamilyPoints as Designer. First read `docs/coordination/handoffs/DESIGNER_LATEST.md`, `docs/coordination/chat-capacity/DESIGNER.md`, issues #18/#15/#20; refresh main and CI evidence. User runs all real E2E tests with QA. Designer source is Web CI green at `19adfe1b7` but overall run #43947907960 is red Backend. Remain in UX defect-fix mode, no real data operations, maintain rolling handoff."

## Latest verification checkpoint — 2026-11-16, ~21:47 CET
- Observed moving main SHA `a6171d1283c5934274681f4d3545f06c0356dca2`. Compare against tested `19adfe1b7`: 34 commits ahead, one frontend file changed (`InitialFamilySetupReview.jsx`): removal of the unused `allMatchingSelected` declaration only; no known behavior changes. This later SHA is NOT independently certified by the older run.
- QA Issue #18: no newly reported confirmed Designer-owned UX failure after handoff. Continue support/bug-fix only; user performs live E2E with QA.
- Backend Issue #15: two red cases from run #43947907960 have source repairs in `839e9f8`, `e1e4e2b`, `9ccfe2e`; QA retest remains requested, newer exact-SHA full CI not yet confirmed. Do not approve BETA or BKL-002 execute.
- Source review only in this checkpoint, no Designer code mutation or direct test execution. Latest verified Web CI remains #43947907960 @ `19adfe1b7` (all Web steps green; Backend/full gate red).

## 2026-11-16 ~23:13 CET — Designer QA/CI audit
- Latest observed `main` at audit: `ff044623ab518ee75bb74935b4e706c7f073b3fc` (Backend capacity checkpoint; moving main may advance).
- Verified GitHub Actions run [#43953986968](https://github.com/o-kobylianskyi/family-points/actions/runs/43953986968) completed: Web SUCCESS, Android SUCCESS, Backend FAIL, Compose FAIL, full exact-SHA gate FAIL. Earlier [#43953661904](https://github.com/o-kobylianskyi/family-points/actions/runs/43953661904) same component statuses. Those are real job-level checks, not assumed from missing commit statuses. Run #43953986968 was on older SHA `4088b5e5` per coordination issue #15; not the latest moving main.
- QA issue #18 has no newer confirmed Designer-owned UX failure. QA real V49 approval remains pending isolated user-assisted campaign; no Designer-owned real test performed.
- Backend issue #15 has new #16 authenticated point-type/currency catalog and manual ledger test-only replay code and negative tests; owner reports new exact-SHA Testcontainers execution NOT_RUN, so neither financial VERIFIED nor BETA readiness can be inferred.
- Release/Android issue #19 reported a source-level PowerShell parser error in Android APK publication script; Release owns it and new SHA validation. Designer must not modify release scripts.
- **Designer status:** no new code tasks; remain QA support and avoid speculative feature churn. Next concrete task: react to a new QA V49 UX finding or verify newest pinned full CI once Backend+Release fixes have independent run evidence.

## 2026-11-16 — latest Designer pickup after QA-019
- Main observed at `246efab2d3f7b7606bcdd2bc1e767ecf0316ebe0` (QA-019 checkpoint). QA #18 still has no new confirmed UX defect assigned to Designer; real V49 E2E remains user+QA-owned.
- Since the Designer's prior monitoring baseline `a6171d12`, Web-owned files `frontend/src/components/InitialFamilySetupReview.jsx`, `frontend/src/utils/setupFinalizeSafety.js`, `frontend/tests/setupFinalizeSafety.test.mjs` changed. Source inspection confirms new `ownsSetupFinalizeMarkers(session,shared,operation)` guard: checks both exact browser markers before finalize POST as a best-effort cross-tab race mitigation, with corresponding regression tests. It is NOT an atomic lock and Backend revision/idempotency remains authoritative. Designer does not overwrite it.
- No newer exact-SHA Web/Full CI evidence discovered in issues #18/#15/#19/#20. Historical Web PASS at `19adfe1b7` cannot validate these later Web changes. Known full run #43953986968 remains Backend+Compose FAIL while Web+Android PASS on its older SHA.
- Backend BKL-002 portable archive #16 POINT_TYPES and point-ledger work still needs independent newest pinned PG18 tests; BETA HOLD, `reset/execute` HTTP 423.
- **Task ownership:** Web owner confirms this new cross-tab code in CI, QA adds two-tabs competing finalize original-UUID/no-repeat POST check in disposable synthetic/real isolated campaign. Designer continues support only, no speculative new features, no real family writes.

## 2026-11-17 — confirmed full CI and QA synthetic status
- Verified GitHub Actions [run #44044271962](https://github.com/o-kobylianskyi/family-points/actions/runs/44044271962), immutable SHA `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a`: all backend Maven, Android Gradle, Web Node/Vite and BETA Compose jobs SUCCESS, plus Full CI / exact-SHA evidence SUCCESS. Read Web job individual steps: install, i18n, source-backed regression, lint and compile all SUCCESS. Historical pinned green only, not approval of live BETA.
- Observed main HEAD `c5786f1e3135998d449a61914cad5a5958f2bdf3` at checkpoint, 93 commits ahead of this green SHA. Differences include `frontend/src/utils/setupFinalizeSafety.js` and `frontend/tests/setupFinalizeSafety.test.mjs` (seven changed lines each) and `qa/e2e/tests/onboarding-v49-two-tab-synthetic.spec.js`. Newest source changes therefore are not covered by the older full green run.
- QA issue #18 [QA-031/#032]: two authored V49 two-tab synthetic Chromium tests for original finalize UUID and competing shared storage marker; QA found wrongly double-escaped JS regexes and Web/QA fixed source `a0d87ee`. Latest documented state is SOURCE_FIXED / PLAYWRIGHT_NOT_RUN. No result for two new Chromium tests can be claimed, even though the Web Node CI was green.
- Backend issue #15 has new #25–33 financial archive/ledger test-only source after prior full-green SHA; current pinned Maven proof is pending, production restorable/replaySupported=false, reset HTTP423, BETA HOLD. No manual real family test or destructive operation authorized.
- Designer owns no new confirmed UX defect. Work is maintenance-only; user+QA run final real E2E in approved disposable workspace, not Designer. Next task: inspect later exact SHA CI when provided, then address only evidenced visual/UX defects from QA. Avoid speculative commits.

## 2026-11-17 ~21:51 CET — Designer follow-up
- Observed main `bc5b1885cb185f7e489aec5fd9706023616aeac8`, 104 commits ahead of latest independently verified full green CI `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a` (#44044271962). Newer changes include seven-line diffs in `frontend/src/utils/setupFinalizeSafety.js` and its unit tests and QA two-tab Playwright test. Do not inherit old run's PASS for these newer changes.
- QA #18 still has no verified Chromium two-tab test result: QA-032 only source-fixed incorrect regex via `a0d87ee`; isolated authenticated QA browser is required, real E2E separately operator-gated.
- Latest `docs/coordination/handoffs/WEB_LATEST.md`: Web ran **14/14 source-backed V8 harness callbacks** covering V49 safety; explicitly NOT the Node `node --test`, npm full regression, Chromium, or GitHub Actions at this SHA.
- Backend #15 #34–36 TEST-ONLY source fixes for positive unused task execution member, strict required JSON arrays and AES negatives remain QA/CI unverified, no production restorable/replaySupported or reset HTTP423 change; BETA HOLD.
- No new confirmed Designer UX defect. Keep Designer implementation frozen unless QA provides a reproducible UX bug. Next task is to inspect QA's pinned synthetic browser run and a newer exact-SHA full CI; not to run real family operations or create speculative design work.

## 2026-11-18 ~00:03 CET — Designer source/QA pickup
- Moving main observed `1ba17931b4db7581b13c3a13b9ca4fe1e139c144` (41 commits ahead of previous Designer monitored `bc5b1885`). Only newer frontend paths in that comparison were `frontend/src/utils/setupFinalizeSafety.js` and `frontend/tests/setupFinalizeSafety.test.mjs`, owned by Web; no new Designer assignment or independently reported UI reproduction in QA issue #18.
- QA #18 latest documented two-tab Chromium test remains `SOURCE_FIXED / PLAYWRIGHT_NOT_RUN`. No operator-approved real V49 E2E test evidence in the reviewed issue. Do not promote synthetic source inspection to browser PASS.
- Backend #15 latest #41–44 test-only member roster preflight remains source-only and unverified at latest SHA. Production `restorable=false`, `replaySupported=false`, reset execute HTTP423, BETA HOLD.
- Last fully verified four-component GitHub Actions run remains `#44044271962` at `8c397d0cbe7d68fd52d5b3f6e18a3ed44cda8d2a`; current latest main not attested by this run.
- Designer decision: no speculative UX commits. Next work only after QA yields reproducible V49 interface defect or a later pinned CI candidate requires source/UX verification.

## 2026-11-18 ~08:54 CET — verified new red CI, scoped Web green
- Actual GitHub Actions [run #44196224270](https://github.com/o-kobylianskyi/family-points/actions/runs/44196224270) at `304819c52ed5668da0c93b4dedc93eb1740868f4`: **Web, Android, Compose SUCCESS** with real steps; **Backend Maven FAIL**, Full CI evidence FAIL and per-push report FAIL. QA/Release issue #15 records actual Backend 493 tests, 2 assertion failures expecting SecurityException but receiving IllegalArgumentException, 0 errors, 0 skips, in `DisposableLedgerSourceGraphVerifierTest`. Do not call full gate green.
- Verified Web job #134438387304 step-level SUCCESS: install, i18n, source-backed regression, lint, Vite compile. This does NOT execute QA Chromium two-tab scenarios; QA issue #18 latest remains SOURCE_FIXED / PLAYWRIGHT_NOT_RUN.
- Moving main at checkpoint `6b5e2a217d86124846cb8c64e879122824abb5bb`, **26 commits newer** than tested `304819c5`. Frontend changes since that run: `frontend/package.json` and new `frontend/scripts/check-v49-fixtures.mjs`, which runs Node `--check` for all `qa/e2e/tests/onboarding-v49-*.spec.js` as regression syntax gate. Source inspected; the additional gate has **not** been verified at latest SHA, and syntax PASS cannot imply browser PASS.
- No new confirmed Designer-owned UX defect; no Designer functionality changed. Continue maintenance-only. User+QA own isolated real E2E; BKL-002 production restorable/replaySupported false, Reset Execute HTTP423, BETA HOLD.
- Next: QA to provide real Chromium two-tab run ID/results and latest pinned CI after Backend corrections; Designer fixes only UX defects supported by QA evidence.
