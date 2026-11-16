# FamilyPoints Web — recovery handoff after conversation hard limit
Date: 2026-11-16 Europe/Berlin. **Trigger:** user screenshot proves Web role chat hit "maximum length of discussion" with **no advance warning** despite requested estimated 50/75/85/90/95% rule. User requests reliable early handoff. New chat should **start now**, not wait for thresholds.

## Repo and source of truth
- Repo: `o-kobylianskyi/family-points`, branch `main`.
- This handoff constructed from GitHub after old Web conversation became inaccessible. Latest reviewed HEAD at drafting: `20e564c09c` (other tracks continue pushing); **fetch current HEAD again** on startup.
- Startup read: `AGENTS.md`, `docs/coordination/README.md`, `PROJECT_STATE.md`, `IMPACT_REGISTER.md`, `ARCHITECTURE_DECISIONS.md`, `CHANGE_PROTOCOL.md`, `docs/coordination/logs/web.md`, and two real V49 QA packets.
- Read cross-track issue [#18](https://github.com/o-kobylianskyi/family-points/issues/18) and [#20](https://github.com/o-kobylianskyi/family-points/issues/20), DESIGNER_TO_QA_REAL_V49_ACCEPTANCE_20261116.md, WEB_TO_QA_V49_REAL_ACCEPTANCE_20261116.md, CI_EVIDENCE_LOOKUP_PROTOCOL_20261116.md.

## Latest Web progress and tested evidence
- V49 seven-step onboarding Web UI (catalog 142 / six packs) built, with checks on family/epoch/revision/ordered codes and no implicit writes. Full Backend feature-enabled acceptance **NOT DONE**.
- Receipt-idempotency: pending UUID retained across lost finalize reply, successful complete server GET status+revision+7 steps required before clearing pending, no second finalize POST; immutable cross-tab marker guards and stale race checks.
- Real V46 `NOT_STARTED steps:{}` renders 7 steps PENDING and 0/7; malformed/foreign metadata fail closed. Browser route synthetic prior 3/3 PASS (operator, not pinned current SHA); two synthetic finalize stale-vs-complete scenarios 2/2 PASS (operator, no same SHA provided).
- **New browser source tests NOT_RUN:** finalize wrong-revision/wrong-epoch, `onboarding-v49-epoch-race-synthetic.spec.js` (commit `6890a41a`), `onboarding-v49-loading-retry-synthetic.spec.js` (commit `758dfe498`), and newest Web-chats screenshot describes another epoch race and GET /setup 503 retry test. Last screenshot states Node source 31/31 PASS, Vite build PASS, lint 4 warnings 0 errors on SHA `758dfe4`, but this is **Web-chat-reported**, not independently verified in this handoff. The screenshot's "Web job SUCCESS" is not same as full four-component or browser E2E PASS.
- Latest Designer/Web changes also improved responsive layout, keyboard labels/focus and duplicate step-action guard. Check latest commits, do not assume screenshot's tested SHA covers latest HEAD.
- All real/manual tests are explicitly owned by **QA with operator**, NOT Web. Web should keep development + local source tests and deliver automated Playwright packs to QA; do not request user to rerun them separately.
- Release CI: historic full 4-component green run `43934619348` on SHA `3c0cf014` confirmed; this is historical, not current HEAD. New per-push report may show SUCCESS with all heavy steps SKIPPED. Read jobs/test step details. Latest runs can be queued/in progress.
- BKL-002 HTTP423 / **BETA HOLD**. No PROD/BETA deployment or real family destructive tests authorized.

## Immediate Web actions
1. Read current Web and Designer source diffs since `758dfe498` and identify any new regression/UX gap, especially epoch race and GET 503 retry; preserve source changes from other tracks.
2. Pass new synthetic Playwright cases through QA with exact SHA and safe fixtures. Run source tests and lint on changed files; if no local runner, mark NOT_RUN, never claim green.
3. Validate V49 receipt GET recovery + all 7 steps, duplicate POST/PATCH prevention, wrong revision/epoch and cross-tab pending identity. No real family writes.
4. Assist QA issue #18 with **real enabled-server V49 in disposable family only** after explicitly authorized environment. Do not take over real acceptance from QA.
5. Coordinate Web/Designer accessibility and mobile correctness; read Release actual selected/full CI steps. Preserve BETA HOLD.

## REQUIRED new-chat reliable handoff practice (repair after failure)
- Cannot read precise ChatGPT remaining context %, and previous estimates failed. **Never promise threshold warnings are guaranteed.**
- Maintain one rolling handoff after **EVERY substantial work batch** (and at least when source SHA/test status changes) in durable docs/coordination/handoffs/<track>_LATEST.md, versioned through GitHub. Keep it self-contained and current, not only a role journal. User can recover immediately even if hard cap occurs on next turn.
- At each batch closing message, include a terse explicit handoff-link and whether ready/current SHA. Offer a fresh chat proactively on substantial conversation growth **without percentage if not measurable**. Any "50/75/85/90/95%" is advisory and only if a justified estimate is possible.
- No hidden autonomous warning capability; messages only when user actively interacts. A hard cap might prevent another response, so do not defer drafting until a guessed 75%.
- User must be told the old chat cannot be resumed by assistant via direct chat access; recover from GitHub.

## Paste into new Web chat
```text
Продовжуємо FamilyPoints, напрям Web (новий чат після досягнення ліміту старого).
Прочитай AGENTS.md і docs/coordination/handoffs/20261116_WEB_CONTINUE_AFTER_LIMIT.md,
а також docs/coordination/logs/web.md з GitHub o-kobylianskyi/family-points.
Перевір актуальний main SHA, CI jobs/steps, Issue #18, Designer і QA handoff.
Віднови V49 регресії та передавай реальні тести QA. BETA HOLD, без реальних
руйнівних операцій і деплою. Наприкінці КОЖНОГО значного пакета оновлюй
довготривалий handoff та додавай посилання на нього у відповідь — не чекай
на неперевірювані 50/75/85%.
```
