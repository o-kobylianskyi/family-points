# Web FamilyPoints — approved Soft Family dashboard parity

Status: **IN PROGRESS / unreleased**. Approved target is the **final dark desktop FamilyPoints mockup** supplied in the conversation on 2026-10-22, with a corresponding light variant. The mockup is the visual source of truth; its sample task/history contents are fictional and must not be inserted into the database.

## Package completed in feature/task-soft-delete

- `frontend/src/components/SoftFamilyIcon.jsx`: vector icons for coins, star, gift, checklist, crown, family, home, points, settings and reusable glyphs — consistent between Chrome/Windows and Android browsers.
- `frontend/public/assets/soft-family/family-illustration.svg`: source-controlled family artwork fallback.
- `frontend/src/components/Sidebar.jsx`: branded icons and sidebar family artwork; existing authorization/dynamic groups unchanged.
- `frontend/src/pages/DashboardPage.jsx`: branded icons and illustration loading with fallback; live API values retained; no fabricated tasks/reputation.
- `frontend/src/App.css`: coordinated desktop light/dark backgrounds, card gradients, typography, subtle borders and responsive spacing.
- Two exact-source **cropped assets** from the generated approved dark dashboard are prepared separately for local validation: `family-hero.webp` and `family-promo.webp`. The GitHub text-file connector cannot attach binary images, so these are bundled as a downloadable archive in the conversation. Copy them into `frontend/public/assets/soft-family` before visual acceptance; until copied, the app falls back to the committed SVG. **Do not mark exact visual match complete without these files.**

## Behavior and acceptance

- [ ] Run Vite build and lint, then verify both themes in browser at 1920px, 1366px, tablet and phone widths.
- [ ] Confirm real / API / family scoping and empty states; never seed invented sample tasks just to match the mockup.
- [ ] Confirm approved WebP hero and promo assets load; if not, SVG fallback works.
- [ ] Confirm high-contrast text, semantic backgrounds and keyboard focus.
- [ ] Compare screenshot against target: hero art; metric card layout; badges; task and history lists; quick actions and left sidebar.
- [ ] Screen-specific refinements in Tasks, Members, Points, Rewards, Settings still pending.
- [ ] Implement user/workspace-configurable avatar and cover images with secure storage, authorization and crop.
- [ ] Web theme switch persistent; Android native parity, Google native sign-in and full beta release gating remain separate work items.
- [ ] Never deploy BETA or publish APK before explicit approval.

## Assets: local install from supplied archive

Archive path inside ZIP: `frontend/public/assets/soft-family/family-hero.webp` and `frontend/public/assets/soft-family/family-promo.webp`.
Extract the ZIP into the local repository root, using overwrite when re-installing. Review image quality in browser and commit the images locally only after approval.
