# FamilyPoints — ARCHITECTURE HANDOFF TO NEW CHAT — 2026-10-27

**User command:** assign priorities across specialist chats and hand off architecture coordination here. **Verified GitHub baseline:** e082c6b4f0b50f69d93356a63c8bcfa65b07ef0b, CI 40414828037 (Java FAIL, Web FAIL, Android PASS, Compose PASS). Always refresh main/CI before declaring outstanding issues.

## Current P0 CI defects
1. **Backend**: Java 229 tests, 3 failures on V41 migration count assumptions: `TaskExcuseConstraintUpgradeTest` expected 3 got 4; `PopulatedDatabaseUpgradeTest` expected V36..V40 = 5 got 6; `LegacyV35ConstraintRepairTest` expected 6 got 7. Inspect migrations and update tests correctly to include V41, do not weaken validation. Source: run 40414828037.
2. **Web**: Vite JSX syntax failure in `frontend/src/pages/SettingsPage.jsx:205`: adjacent JSX siblings not wrapped. Repair JSX tree, verify Vite.
3. Recheck both at newest commit, avoid fixing resolved defects twice.

## Product decision and BETA blocker
- **BKL-002 selective family reset MUST be functional before next BETA**, to test reset → onboarding → 142 task/reward global template suggestions → 6 packs → selective private import → separate task/reward activation. Current Backend V41 reset audit/idempotency schema + non-destructive dependency selection plan; actual transactional delete and authorized execute still not active. Web reset readiness panel is non-destructive; no preview/execute API verified.
- **Backend P0:** finalize category impact/dependency and protected scope preview, challenge/confirm authorization, transactional scoped reset, audit and idempotent request handling, monotonic family/workspace `dataEpoch` increment and stale operation rejection. Preserve login/account/family identities by default; protect other families and cross-tenant links. Tests incl. foreign keys and replay.
- **Web P0:** after Backend contract add localized reset checklist, impact counts, authentication/confirmation, error/rollback, onboarding restart, optional template import and explicit activation. Current readiness-only panel must not masquerade as reset capability.
- **Android P0/P1:** offline quarantine exists (SQLite v4 `reset_holds`, queue pause) but **has no backend reset event caller**. Wire epoch check when authenticated workspace loads and before synchronize/enqueue; never replay stale cached work after reset, never silently discard pending ops. DEV emulator QA; Huawei signed BETA unchanged.
- **QA P0:** use explicitly authorized disposable DEV workspace for end-to-end reset, no reset of user existing families. Check cross-family, accounts, balances, audit, idempotency, no orphan records, four currencies and 1/2/5/11/21/22/25 localization, 142/6 suggestions and one/new/mixed import. TPL-08 30-min TIME import still needs first selecting relevant private TIME reward. BETA only needs targeted smoke, not every exploratory testcase.
- **Release P1:** publish matched green SHA Backend/Web+V41 and signed Android when needed, after reset contract and targeted DEV QA; public beta manifest/version independently verify. No destructive action on actual user DB without fresh explicit scoped consent.
- **Designer/Media P1:** approved 18 category icons, ensure Web/Android consumers + localized labels, 142 seeds; economy v0.7 **preliminarily approved for implementation** (not auto-retroactive corrections). Validate suggested difficulty distinct from duration, opt-in adjustable rewards/prices, `null` != 0; tune on BETA. Keep 65 templates not in starter packs discoverable; avoid unsolicited catalog rewrites.

## P2 after next BETA
BKL-003/005 adult historical corrections; BKL-008 timer and 5m/2m/1m beep warnings; BKL-009 Android installed-app reward with title/icon/package and launch; BKL-010 Device Owner enforcement; BKL-011 adults/events.

## Working rules
Main-only GitHub; prefer complete coherent batches, no unrelated tiny edits; each specialist logs source SHA, test evidence, QA blockage and next task. Architecture percentages are Backend/Web/Android implementation estimates, NOT QA or Release percentages. No deployment/DB cleanup via architecture dispatch. All tasks are recorded in GitHub and visible to other chats after they read their logs; chats themselves cannot be messaged directly.

**Next architecture action:** check current main CI after handoff; route exact remaining CI blockers to Backend/Web, then validate reset Backend contract, update Web/Android/QA readiness, prepare release candidate.
