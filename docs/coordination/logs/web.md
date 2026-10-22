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


### Web work branch created (2026-10-22)
- Dedicated Web-only branch: `feature/web-soft-family`, based on current integration SHA `3bc7a88de10298ddb6b6f2acbe8172361f7d5617`; separate from Android, Backend and Release branches.
- Future Web commits and Web-specific journal updates belong here; integrate only via review/PR to the then-current integration target, rechecking shared impacts first.
- This action created a branch and updated its Web journal only; it did not build, deploy or change application/DB/server code.


## 2026-10-22 — Web artwork import helper (SOURCE COMMITTED, artwork still missing in Git)
- Checked previously generated ZIP in the task container: valid `family-hero.webp` (18,294 bytes) and `family-promo.webp` (11,404 bytes); exact source SHA-256 recorded in Web acceptance doc.
- Added `scripts/web/install-soft-family-assets.ps1` on `feature/web-soft-family` (commit `f8b0164f`). Searches common user download locations or accepts full `-ZipPath`, checks archive member path and SHA-256, writes only the two target WebPs and optionally stages them for Git.
- The local Windows error `Expand-Archive: path ... does not exist` means the file wasn't saved at the assumed path. The helper does not download the ZIP automatically. Original binary assets have **not** been pushed to the repository; they require one actual local download/import and Git commit, or a compatible binary-upload mechanism.
- **No build, browser, CI run, BETA release or database operation performed here.** Other workstreams remain on their own branches; do not change deployment or Java/Compose source to solve artwork installation.

### Web integration review (2026-10-22)
- Opened **draft PR #2** (`feature/web-soft-family` → `feature/task-soft-delete`), https://github.com/o-kobylianskyi/family-points/pull/2. This is a review/CI gate, not release approval.
- CI run `39844008339` observed **in progress**; no passing result claimed. User must import and commit the exact two approved WebP images before marking visual asset delivery complete. The verified installer is committed and can stage only those images.

## 2026-10-22 — Backend request: uploaded/default media and image inheritance
- Web user requirement: curated default illustrations everywhere, optional custom uploads for member avatars, people groups, task group/categories, task override with category inheritance, plus family workspace cover and optional family icon.
- Initiated [XTR-20261022-W-01](../../coordination/requests/XTR-20261022-W-01.md) on integration branch `feature/task-soft-delete`, registered IMP-0007 and GitHub issue #3 (Backend). This is an **incoming Backend assignment**, not a Web or Backend implementation.
- Contract proposal preserves MemberGroup vs WorkNode distinctions, stable media IDs, server-side scoped authorization, defaults and fallbacks, old-client compatibility and private cache. Backend must ACK; Architecture must decide media ADR; Android/Release will review. Web image picker and crop UX blocked on approved API.
- Until Backend contract is ready, continue source-controlled bundled illustrations and image fallback styling only. Do not invent upload endpoints or leak private media.
