# FamilyPoints Media Catalog — precise Backend registration contract

**Owner:** Designer. **Status:** 18 SVG assets present in main, user-approved; registry integration is Backend-owned and remains PENDING. This document does NOT constitute API deployment.

## Source code finding
`MediaService.catalogPreview()` serves **only** keys included in the private `CATALOG` allowlist or the PNG-avatar registry; it looks up `classpath*:/media/catalog/**/{key}.svg`. Existing 18 files under `categories/` are not auto-discovered by this route. Therefore `/workspaces/{workspaceId}/media/catalog/{key}/preview` cannot yet return their images by approved key.

## Required backend change
- Add 18 entries to `CATALOG`, to `catalog()` as category items, and to ordered labels/metadata. Stable proposed identifiers in companion `ICON_REGISTRY_CONTRACT_V1.json`; resolve filename vs key divergence: **either** rename bundled SVG files to approved registry keys **or** implement a strict static map from key to filename. Do not allow path injection or free-form user-supplied path.
- Preserve workspace membership checks and all historical keys. Use appropriate MIME `image/svg+xml`, protect SVG sanitization/CSP.
- `catalogPreview` must resolve a unique classpath resource and return 404 on unknown key. No direct URLs constructed with untrusted text.
- Do not couple these icons to avatars, uploaded private images, or user balances.
- Web/Android: use catalog-provided keys and previews through authorized path. Android may need vetted PNG fallback or vector conversion; no assumption Android can render SVG bytes natively.
- The preferred matching for task/reward semantic categories is in the JSON manifest; treat it as mapping proposal, not active backend behavior.

## QA
1. 18 new selectable catalog entries, no collisions with existing keys or labels, stable ordering.
2. Each preview has bytes and expected MIME, cannot resolve path traversal or a foreign workspace.
3. Existing avatar/media bindings keep working; unregistered keys safely fallback.
4. Light and dark display at 24/48 px, localized labels, legacy categories and new template suggestions.
5. Catalog category choice never mutates personal family data before confirmation.

**Next owner:** Backend implement registry; Web/Android consume after API acceptance; QA test; Release publish independently.

## Backend implementation update — 2026-10-26
- Implemented fixed 18-key SVG registry in `MediaService` at commit `c406aa75197561810ae13e87522435bcdc5f323e`. Map resolves approved `category-*` keys to exact `media/catalog/categories/*.svg` resources. All 18 entries are in `catalog()`, with designer-specified category, labelKey, sortOrder.
- Existing legacy catalog and portrait PNG keys remain accepted. `bind` validates approved category keys; `catalogPreview` retains workspace membership check. MediaController responds `image/svg+xml`, `nosniff`, private no-store.
- Tests for 18 distinct catalog keys, SVG bytes, unauthorized workspace and unrecognized path-like keys at `587be2bbaa225c9b4a0a9381dd6d9dafbaf728a9`.
- CI `40409633907` pending as inspected; **no runtime PASS yet**. Authenticated DEV preview and 24/48px Web QA pending. Android might need PNG fallback.
- **Web/Android handoff:** discover keys through `GET /workspaces/{workspaceId}/media/catalog`, obtain preview at `GET /workspaces/{workspaceId}/media/catalog/{key}/preview`; maintain fallback for unavailable keys. Use `docs/design/ICON_REGISTRY_CONTRACT_V1.json` for proposed semantic mapping. No automatic changes to family bindings or economy.
