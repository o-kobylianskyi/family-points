# Designer — Category icons consumer QA follow-up (2026-10-26)

## Verified Web source progress
Web log reports commits `5ebd00e`, `9a59f33`: global TemplatesPage uses Designer templateCategoryMapping to resolve category-* media catalog keys, authenticated preview blobs, workspace-scoped cleanup, 40px card thumbnails, fallback icons. This is **source completion for global template catalog**, not proof of private library or live task/reward presentation.

## Open Web requirements
- Private template cards, task detail/edit/create and reward list/detail/create: preserve explicit user-selected media precedence and fall back to category key only when none bound.
- Verify all 18 media entries accessible under actual DEV Backend deployed SHA; light/dark, 24/40/48px, UI widths 360/390/1366, four locales and A/B family switch; record screenshots and HTTP behavior.
- Invalid key/401/403 should fall back without broken images or persistent token exposure.

## Open Android requirements
- Implement and QA 18 Designer category assets on actual task/reward surfaces; existing MediaApi/preview decoder is not proof of surface integration.
- Use authenticated scoped media catalog key; verified SVG decoder or bundled raster/vector fallback. Do not pass arbitrary SVG to BitmapFactory.
- No private icon overwrite; offline fallback and cross-account cache isolation.
- Record debug APK SHA, emulator/device screenshots for light/dark, 24/40/48px, exact workspace and 4 locales.

## Current evidence limits
CI run `40411800542` successful at prior commit `d015b23db6`, while head run `40414272037` at `8c0fd220d8` in progress when inspected. Current HEAD is not declared CI green. No DEV authenticated screenshot/smoke was personally verified. QA remains pending, no BETA publication authorized.
