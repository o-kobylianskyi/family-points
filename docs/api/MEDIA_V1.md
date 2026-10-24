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
