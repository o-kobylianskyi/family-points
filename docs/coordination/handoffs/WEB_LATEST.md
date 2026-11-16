# FamilyPoints Web — rolling latest handoff

**New role chat:** FamilyPoints — Web Frontend (WEB-02), pickup 2026-11-16 Europe/Berlin after old chat reached hard cap. **Repo:** `o-kobylianskyi/family-points`; integration branch `main`. **HEAD inspected at pickup:** `e51c0972aa3dd7a2dd5960cb5e450b40bae4750c` (2026-11-16 20:25Z). Other tracks push concurrently: re-fetch HEAD before every edit. Old emergency handoff: [20261116_WEB_CONTINUE_AFTER_LIMIT.md](20261116_WEB_CONTINUE_AFTER_LIMIT.md).

## Source and decisions checked
- Read `AGENTS.md`, `docs/coordination/README.md`, chat-capacity pilot, Web journal, Web QA packet and issue #18/#20. Release status must not be inferred from commits alone.
- Source checked: `frontend/src/utils/setupFinalizeSafety.js` and `frontend/tests/setupFinalizeSafety.test.mjs` (current file content fetched at pickup); `frontend/src/utils/setupCatalogSelection.js`, its node test file, and `frontend/package.json`.
- Latest Web/Designer commits after prior screenshot: `2717222` and `442a458` guard step PATCH epoch/revision/step; `cb5c44f` centralizes repeated-step guard; `5c969dc` covers filtered partial deselect. **Do not duplicate these.**
- CI optimization issue #20 adds `npm run test:regression` and lint to main CI. Full same-SHA jobs should be confirmed through Release with exact run ID and executed steps.

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
