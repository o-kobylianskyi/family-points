# Designer — Economy v1 cross-client QA acceptance (2026-10-31)

Backend reference: `docs/api/DESIGNER_ECONOMY_MEDIA_V1_CONTRACT.md`. Web `WEB-ECONOMY-02`, Android explicit draft apply `58bc25a` with validation correction `58ce8e9`. Source implementation **does not equal** DEV/emulator acceptance. Current CI at check: earlier full SUCCESS `41235304877` for `0df2d812b`; newer run `41248956275` for `437880faf` still in progress.

## Shared acceptance cases (both clients)
1. Authenticated GET `/template-catalog/economy-suggestions`: `schema=familypoints.economy.suggestions.v1`, `version=1`, `sourceCatalogVersion=1`, `currencyCode=POINTS`, `autoApply=false`, `workspaceMutation=false`; 110 TASK / 32 REWARD; 11 numeric TASK suggestions / 12 numeric REWARD suggestions; others `null` (not free/0).
2. Global and private template detail: recommend only for matching code/kind, show review notice and difficulty distinct from duration, preserve existing family price and reward. Explicit zero as family-owned input is not missing.
3. Private template creation-draft: GET read-only; apply button available only when amount integral/applicable, `pricingState=OPTIONAL_SUGGESTION` and permission/confirmation valid. Tap sends **POST apply to draft only**; same workspace, templateId and kind; requiresConfirmation true, suggestionAppliedToDraft true. No activation/private-template save/ledger posting.
4. Confirm custom stored price remains untouched before Apply; Web modified JSON prevents silent overwrite; Android stale async response cannot replace new workspace or draft identity. `null`, absent, malformed, huge/non-integral suggested prices do not enable Apply; apply rejected without POINTS currency/permission.
5. TIME 30-minute `fp.reward.time.007` maintains defaultDurationMinutes=30 and **null price**; do not equate reward suggestion `kind=TIME` with activation `rewardKind` enum.
6. Cross-family Test/Test2 and admin/child permissions: scoped private drafts and apply blocked for unauthorized user. Error network/401/403/409/422 leaves editable user-owned data intact; no credentials in logs.
7. UI: UK/RU/DE/EN, dark/light, mobile/small viewport, fallback if economy GET fails; no broken templates listing. Record screenshots/network traces redacted of tokens.
8. Category SVG: independently fetch all 18 registered keys over authenticated `/media/catalog/{key}/preview`; verify SVG rendering, private-cache isolation, fallbacks and existing explicit TASK media precedence. Writable REWARD binding **not supported**: no speculative PUT/DELETE for `REWARD`.

## Execution evidence to capture
- Exact git SHA and GitHub Actions run, Web Playwright test count and console output, Android `:app:assembleDebug :app:testDebugUnitTest` output plus emulator screenshots.
- QA's existing read-only `qa/e2e/tests/economy-suggestions.spec.js` covers wire subset; add safe UI draft assertions, **never persist/activate/reset** on actual family. Any mutation-dependent acceptance requires explicitly designated disposable QA workspace and authorization.

P0 BKL-002 reset execute remains disabled; do not link this P1 package to release approval without separate gates.
