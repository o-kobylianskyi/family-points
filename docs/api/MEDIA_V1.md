# FamilyPoints Media API v1 — implementation draft

Source: `feature/backend-media-assets-v1`, draft PR #5. Applies to ADR-MEDIA-001 and XTR-20261023-P-01. This contract is **not deployed**.

## Global curated bank — user approved 2026-10-24

Two storage/security domains are deliberately separate:

- **CATALOG:** curated reusable system images, one global binary per stable asset key/version, no `workspace_id`; selectable by any authorized family without copying bytes or consuming its private quota. Catalog images are public/cacheable only after artwork provenance and text-free content are verified.
- **UPLOAD:** normalized family-owned image bytes in PostgreSQL; `workspace_id` is mandatory, private authenticated read, strict cross-family isolation and 100 MiB per-workspace planned cap.

The currently implemented `GET /workspaces/{workspaceId}/media/catalog` returns **catalog metadata only**, with `key`, `category`, `labelKey`, `version`, `selectable`, and `previewPath` (`null` until verified image binaries are available). Existing keys are placeholders/default identifiers; **the image bank itself is not populated yet**. Never present a null preview as an available rendered image.

### Planned content categories

AVATAR (people/characters/animals), FAMILY, COVER, GROUP, TASK_CATEGORY, TASK and later REWARD. Initial approved art must be text-free and have documented provenance. A family can pick any suitable avatar regardless of age/gender. Curated references are global and shared by key; uploaded images remain family-scoped.

### Existing draft routes

- `GET /workspaces/{workspaceId}/media/catalog` — workspace-authenticated catalog metadata.
- `POST /workspaces/{workspaceId}/media/uploads` multipart form field `file`; currently JPEG/PNG only, max 5 MiB, server-side normalization; permission required.
- `GET /workspaces/{workspaceId}/media/bindings/{type}/{targetId}/{slot}` — authorized effective image reference.
- `PUT /workspaces/{workspaceId}/media/bindings/{type}/{targetId}/{slot}` — `{"assetId":null,"catalogKey":"member-default","expectedVersion":0}` (one of assetId or catalogKey).
- `DELETE /workspaces/{workspaceId}/media/bindings/{type}/{targetId}/{slot}?expectedVersion=N` — remove override.
- `GET /workspaces/{workspaceId}/media/{assetId}/variants/THUMB` or `CARD` — private read of bound normalized bytes with JWT. Native browser `<img src>` cannot attach Bearer JWT: use authorized fetch and Blob object URLs.

### Remaining before client integration

Commit and verify real curated image binaries, idempotent import or versioned static deployment, preview delivery, usable fallback render, optional category/search filtering, content provenance and localization, version invalidation tests, effectiveImage additions to family/member/group/task DTOs, scoped delegated authorization, WebP decision, orphan cleanup, real two-family tests and V35 + V36 migration integration. Web/Android/Release reviews and CI required; no merge/deploy automatically authorized.

## Renderable catalog previews
The catalog includes 15 checked-in text-free original SVG illustrations (9 avatars plus family, cover, group, task and reward symbols). Preview URL template from `CatalogItem.previewPath` replaces `{workspaceId}` with the current workspace. `GET /workspaces/{workspaceId}/media/catalog/{key}/preview` returns vetted bundled SVG only (never user SVG); unknown key = 404; workspace membership required. Private uploaded JPEG/PNG is never served by this endpoint. The icons are bundled in app resources so no DB seeding is necessary for v1 catalog and no duplicate binary is stored per family. The artwork can be visually upgraded later while maintaining stable catalog keys/version rules.

## Approved Soft Family dashboard header override — 2026-10-24

The existing, approved `frontend/public/assets/soft-family/reference/hero-family.webp` is the **unchanged default** for the Web dashboard header. `GET /workspaces/{id}/media/bindings/WORKSPACE/{id}/COVER` returns `source=DEFAULT`, `mediaId=null`, and no remote image `path` when no explicit cover is assigned; client continues to use its existing static hero image and CSS. The default catalog key `family-cover-default` is a fallback identifier, **not an instruction to replace approved dashboard artwork with the bundled SVG**.

An authorized family manager may explicitly choose a catalog cover or upload JPEG/PNG, then PUT the WORKSPACE COVER binding with `expectedVersion`. For an uploaded cover, the effective reference uses the **CARD** normalized media variant (800px bounding dimension), not the 192px THUMB intended for avatars; a curated cover uses its vetted catalog preview. A private image URL requires JWT-authenticated fetch and isolated temporary Blob URL, not an unrestricted `img src`. Manager-only clear with `expectedVersion` returns DEFAULT with incremented version, restoring the original static Web hero. No decorative text, motto, controls, sizes, light/dark overlays or approved Home design is changed by this Backend requirement.

Follow-up visual acceptance: a dedicated wide COVER render size and client crop/position settings should be designed without assuming that a normalized 800px CARD is sufficiently sharp at 1920px desktop. These are **not implemented in this change**.

## Wide header render — 2026-10-24
- A private image upload now produces three normalized, EXIF-free raster variants: `THUMB` (192px), `CARD` (800px), and `COVER` (2048px maximum dimension); source aspect ratio preserved, no enlargement beyond uploaded resolution. The single family quota includes all three stored blob byte lengths. Input still JPEG/PNG ≤5 MiB, original max 4096px/16MP.
- Workspace COVER own binding now resolves to private `/workspaces/{workspaceId}/media/{assetId}/variants/COVER`, requiring authenticated read. Other bindings continue to use THUMB. `COVER` is a binary rendition distinct from target slot `COVER`.
- The existing approved dashboard hero remains the fallback when `source=DEFAULT` and no private image path is provided. Web can crop/position CSS background without changing the original approved art. Per-family crop/position persistence is still pending and not asserted here.
