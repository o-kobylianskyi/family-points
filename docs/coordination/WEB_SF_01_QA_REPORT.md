# WEB-SF-01 — consolidated Web QA handoff (2026-10-24)

Status: **SOURCE AUDIT COMPLETED / BROWSER ACCEPTANCE BLOCKED / NO-GO**. Basis: `docs/coordination/QA_TEST_CASES.md` WEB-SF-01. Environment: GitHub main source inspected; deployed BETA SHA unknown, logged-in test browser unavailable. No screenshots captured, npm build/lint/i18n not run (runner could not resolve github.com for cloning). **Do not interpret source observations as live regressions.**

## Confirmed source findings (fix together)
1. **F01 P1 — Untranslated Ukrainian field label.** File `frontend/src/components/TaskRewardNegotiationModal.jsx`, reputation amount field: `<span>Кількість</span>`. Steps to verify after patch: choose de/en/ru, open task reward request/review dialog, ensure field label uses locale and longest DE remains readable. Expected: all four locales properly translated. Actual source: always Ukrainian.
2. **F02 P2 — Unlocalized stepper accessibility labels.** File `frontend/src/components/NumberInput.jsx`, two buttons: `aria-label="Increase"` and `aria-label="Decrease"`. Steps: choose uk/de/ru, open points award/deduct, inspect browser accessibility tree / screen reader; expected local accessible names including amount context. Actual source: fixed English.

## Risk, not reproduced bug
- **R01** Duplicate mutation on near-simultaneous submit with delayed network: React state guards `saving/loading/submitting` in modal forms. Run Chrome Network Slow 3G and double click; check one POST, one ledger event. If reproducible, add immediate synchronous guard plus backend idempotency as appropriate. No evidence yet of failure.

## Ten required browser tests — BLOCKED (not PASSED)
1 Navigation/7 routes; 2 Tasks forms/dialogs; 3 Members/Groups; 4 Points modal + number control; 5 Rewards request/negotiation; 6 Auth/onboarding/settings; 7 keyboard/focus/Escape; 8 responsive 1440×900,1366×768,390×844,360×800, tablet and 200% zoom; 9 refresh and A/B workspace switching; 10 frozen approved Home screenshot parity.
For each run check both themes and four locales uk/de/en/ru, collect screenshot + browser/viewport, exact deployed SHA, console, overflow, focus, pending-save behavior. Use synthetic test accounts only. No deployment by QA.

## Release gate
Web owner: one correction package for F01/F02, verify R01, provide SHA/build/lint/i18n evidence, then QA reruns the full matrix. NO-GO pending independently observed P0/P1 passes. No approved Home asset or layout changes.
