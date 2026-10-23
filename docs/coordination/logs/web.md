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

## 2026-10-22 — Consolidated Web into single local Vite environment
- User confirmed one frontend environment: `family-points/frontend` on `localhost:5173`. No separate Web server/port required.
- Integrated Web-owned avatar component/CSS and member/tasks/points adoption, plus the verified two hero/promo WebP binaries **directly** into integration branch `feature/task-soft-delete` as commit `7077035b1371026414ad3887c8aa0af84c160fe9`, preserving all current Backend/Android/integration files. Seven exact paths transferred; no wholesale branch merge.
- Old Web branch `feature/web-soft-family` retained as archive for inspection; PR #2 to close as superseded, not to merge; `scripts/web/install-soft-family-assets.ps1` remains only in old Web branch, because binary assets are now tracked in integration and no longer need manual install.
- To view: start Vite from `family-points/frontend` on 5173 after `git pull --ff-only origin feature/task-soft-delete`. Do not delete a Windows worktree until checked for uncommitted files and removed through `git worktree remove`.
- Source integrated; CI/browser verification still pending. No server deployment.

## 2026-10-22 — Dashboard defects reported again; confirmed missing implementation
- User verified local 5173 after pull. Previous integration transferred illustrations/avatar infrastructure, **not** the requested reference exact-match fixes. Acknowledged misleading earlier completion language.
- Committed CSS correction `95159c47` to integration: sidebar promo image constrained within its panel; hero artwork uses contain and matches hero bounds instead of negative bottom/oversized height. Responsive overrides added; requires screenshot check.
- Outstanding: extract exact logo and graphic icons from provided reference only where original asset provenance and clean transparent extraction can be verified; do not call redrawn SVGs exact matches. Hero composition, responsive and both theme visual verification still open.
- No Vite/browser test run by assistant; no Backend/Android/DB/release changes.


## 2026-10-22 — Dashboard reference crop integration (PARTIAL — binary package local only)
- Inspected Backend integration branch and journal at `5d9fcbf4`: Backend has acknowledged media XTR-W-01 / IMP-0007 but API, schema, binary store and release backup are BLOCKED by Architecture ADR. **NO media database/API upload performed or claimed**. Separate calendar PR #4 is not a media implementation.
- Modified dashboard JSX, Sidebar and App.css to consume explicit `/assets/soft-family/reference/*` asset names with legacy SVG/image fallback. Removed duplicated main hero description. Adjusted hero and bottom-left promo panel; preserve real API data.
- Extracted an 11-asset WebP archive directly from user-approved reference image in this chat, including brand mark, 4 large stats, 4 quick-action icons, hero crop and full sidebar promo. Package not yet committed to GitHub binary storage; user must extract at repository root. **Source code without this package falls back to old graphics** — do not report full visual parity.
- Still requires actual browser side-by-side verification, exact alpha edges and responsiveness; extracted icons are reference crops and may contain partial background. No DB, VPS, Android, Maven or runtime smoke test.

## 2026-10-22 — User screenshot: reference assets visible, crop seams remain
- User screenshots confirm screenshot-cropped art is finally loading: bottom-left family promo, family hero and quick-action icons. Visible defects: rectangular hero raster background seam; duplicate/offset promo edge; hard square cropping around quick action icons. These are raster extraction limitations, not Git pulling failures.
- CSS commit `8fb40a51`: mask hero left/right seams, normalize promo frame (remove outer border), apply radial fade on quick action/empty-state icon crops, preserve theme and dynamic values. This is a workaround, not verified pixel parity or original transparent assets.
- Screenshot check after pull remains required; backend media DB upload API is still unimplemented, so images remain static assets. No deployment or build claimed.

## 2026-10-22 — Approved header plan vs browser fact
- User supplied crop pair: approved header stretches across available dashboard area and uses 3 distinct lines (welcome, white subtitle, teal unique motto) plus family art centered right and a right-side handwritten decoration; browser actual capped dashboard at `page-content max-width:1400px`, omitted subtitle and positioned artwork flush right without decoration.
- Updated 4 locale JSON files with `dashboardLive.familyMotto`, restored `dashboardLive.description` as subtitle (previous removal was incorrect), updated hero decoration, and added dashboard-scoped full-width CSS with responsive hero image and copy layout. Relevant commits `a67d522`, `f9f04b4`.
- Static asset photo is a screenshot crop, so perfect transparency/handwriting remains unavailable; handwritten text is typography approximation, not copied image. No browser run or full visual acceptance claimed. Deployment/Backend unaffected.

## 2026-10-23 — Dashboard header: image is full-section background
- User specifically requested hero art to be the background of the **whole header block**, not a separate image on its right. Preserved manually updated visual asset bytes: no files under `frontend/public/assets` created, replaced or deleted.
- Removed standalone `<img className="fp-hero-art">` from DashboardPage and set CSS `background-image` on `.fp-dashboard .fp-hero` using the same `/assets/soft-family/reference/hero-family.webp` URL with `background-size: cover`, position and gradient overlay for text contrast. Adjusted responsive styles and dark/light backgrounds. Original calligraphy/text remain HTML.
- Integration directly on `main`, no server deploy or database change. Build and actual browser screenshot validation pending; `cover` can crop edges at aspect ratios and may need separate user-approved positioning adjustment.

## 2026-10-23 — Approved Home visual baseline and branch workflow (documentation only)
- User confirmed the Home/dashboard visual design is finished: hero, cards, icons, sidebar and light-theme hover. Preserve all approved styles and manually edited image assets. Do not change Home appearance without explicit approval. Next Web scope: bug fixing on other pages.
- User requires `main` as default active development branch for all tracks; temporary branches only when an exceptional, documented safety need exists. Shared AGENTS.md, coordination protocol and README have been updated.
- The local Windows checkout was last reported on `feature/android-soft-family-structure` with 40 unpushed commits; this GitHub-only policy change does not switch it, transfer local commits or alter any images. Previous Web light-theme hover fix `a4f1672` was confirmed locally by user. No source, tests or deploy in this documentation update.
