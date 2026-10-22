# FamilyPoints — Web journal

New batches: React/Vite UI, Soft Family parity, accessibility, i18n and REST consumption. Retain existing Web entries in root development log.

## 2026-10-22 — Track initialized (DOCUMENTATION ONLY)
- Web's Soft Family dashboard/theme redesign is in current development history; this setup does not assert tested parity or deployment.
- Incoming: IMP-0001 visual/native parity and images; IMP-0002 auth/registration; IMP-0004 workspace switch; IMP-0005 authoritative synced state.
- Next: test light/dark/mobile layouts against agreed design, record screenshots, declare any Backend media contract needs before coding.


## 2026-10-22 — Web track ownership accepted; coordination onboarding

- **Assigned scope:** React/Vite frontend, layout, Soft Family light/dark design parity, responsiveness, accessibility, localization, Web API client consumption and web-only UX tests. Android UI/Compose, Java/DB/REST ownership, shared architectural contracts and all production releases remain with their respective tracks.
- **Reviewed integration HEAD:** `43e990ead5ab06b287066336292c17a2bd29b7b7` (observed 2026-10-22, before this journal update) on `feature/task-soft-delete`; checked all new coordination files, AGENTS, existing dev/checklists, concept/operations history and relevant Web/backend source. `main` still points to older `c6847c6389`. Future batches must refresh HEAD.
- **IMP-0001 (Web producer, ACK of scope):** visual Soft Family approved; Web hero/sidebar visual match remains unverified. Web-specific parts include dashboard, themes, illustrated icons, responsive cards; Backend/Architecture must decide tenant-scoped avatar and family-cover media contract before upload functionality; Android adapts natively.
- **IMP-0002 (Web consumer: ACK / NOT VERIFIED):** reviewed `frontend/src/api/authApi.js`, `accountApi.js`, `AuthContext.jsx`, Backend `UsernameAvailabilityController.java`, `GoogleAccountLinkController.java` and `SecurityConfig.java` at reviewed HEAD. Public username matcher is committed; actual BETA exposure/Google feature flags, errors and auth regression tests remain unverified.
- **IMP-0003 (Web consumer: ACK / BLOCKED pending Android+Release):** reviewed `DownloadAppPage.jsx` manifest, APK HEAD and SHA paths; Web owns UI and metadata presentation, NOT the signed APK or publishing. Signed package/device verification and Release gate remain external.
- **IMP-0004 (Web consumer: ACK / NOT VERIFIED):** reviewed `AuthContext.jsx`, `ChooseFamilyPage.jsx`, `authApi.js` workspace list/activate/onboarding and `taskApi.js` scoped URLs. Cross-family isolation and invitation E2E negative tests pending Backend/Release validation.
- **IMP-0005 (Web consumer: ACK of proposal, BLOCKED):** read backend-authoritative ledger requirement and `taskApi.js` existing workspace-scoped endpoints. Offline idempotent replay/conflict contract is not yet agreed/implemented; Web must not display speculative synchronized balances.
- **Current Web blockers:** uncommitted `family-hero.webp` and `family-promo.webp` images exist only in separately provided local ZIP (repo currently has SVG fallback only); Web screenshot parity, Vite lint/build, cross-page light/dark contrast, and four-language theme UI checks remain pending. No runtime, server, CI, VPS or DB verification was performed in this onboarding batch.
- **Next Web work:** confirm source-controlled artwork and visual acceptance of dashboard at desktop/mobile/light/dark, then standardize Members/Tasks/Points/Rewards/Settings. Register cross-track impact BEFORE changing any REST/media/auth contract. Commit only Web changes on an isolated `feature/web-*` branch as concurrency increases, integrate via reviewed PR.
- **Release hold:** only Release/DevOps decides deploy/tag/APK publication. No changes to Contabo, Flyway, Java, Android or signing in this batch.
