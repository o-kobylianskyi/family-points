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
