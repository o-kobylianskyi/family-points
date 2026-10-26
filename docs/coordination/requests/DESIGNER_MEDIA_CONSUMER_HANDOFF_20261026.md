# FamilyPoints — Designer approved category icons: Web / Android / QA integration handoff

**Date:** 2026-10-26. **Status:** ASSIGNED TO CONSUMERS, NOT VERIFIED IN DEV OR BETA. **Owner:** Designer (assets), consumers: Web / Android / QA / Release.

## Verified implementation
- The 18 user-approved SVGs are committed in `src/main/resources/media/catalog/categories/`.
- Backend `MediaService.CATEGORY_ICONS` contains 18 strict key→file entries, `categoryIcons()` creates selectable catalog items, `catalogPreview()` checks workspace membership and resolves approved classpath paths; Controller responds `image/svg+xml` with `nosniff`.
- Web `EntityMedia.jsx` already calls workspace-scoped catalog and loads authorized Blob previews, but task/reward category **default auto-presentation** is not demonstrated.
- Android `MediaApi.kt` already has catalog and preview endpoints but a working SVG→Compose rendering strategy for category cards is not demonstrated. Do not assume generic BitmapFactory decodes SVG.
- CI run `40409863763` was **in_progress** at observation (HEAD `b2e7dd7183d`). No green/DEV deployment or emulator proof claimed.

## Required consumer work

### Web
1. Verify editor filtering and display of newly registered `TASK_CATEGORY` and `REWARD` entries, localized fallback label if `media.catalog.category.*` key is missing.
2. Resolve `docs/design/ICON_REGISTRY_CONTRACT_V1.json` mappings to catalog keys for task/reward category defaults (not arbitrary resource URLs); fall back to existing `task-default`/`reward-star`.
3. Preserve explicit workspace media selection and override precedence; don't rewrite existing family records.
4. Test light/dark at 24px and 48px, compact dialog, long lists, four languages, 401/403 and broken preview.

### Android
1. Validate GET catalog contains 18 new keys and GET preview uses bearer token, active workspace scope.
2. Render approved SVG content via safely supported vector/raster pipeline or bundled PNG fallback; **do not** pass SVG to BitmapFactory unchanged.
3. Present category defaults with explicit override and fallback, no background cross-account caching.
4. Emulator testing: dark/light, 24px/48px, workspace switch, offline cache, missing icon, Russian/German labels.

### QA / Release
- Verify catalog count and exact 18 keys, preview bytes/mime (including invalid key and foreign workspace), all-category visual fit, existing avatar previews/regression.
- Check Web/Android screenshots against confirmed deployed HEAD; imports of templates and economy suggestions remain side-effect-free until explicit action.
- Pin Backend/Web/Android deployed SHAs and independent CI/DEV smoke. No automatic BETA publication.

## Designer sign-off
**Creative assets:** approved by user, SVG files uploaded, keys/mapping supplied. **Consumer work/QA:** open. Existing avatars paused.
