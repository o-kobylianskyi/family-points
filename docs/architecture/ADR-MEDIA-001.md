# ADR-MEDIA-001 — FamilyPoints media ownership, storage and secure image API

**Date:** 2026-10-23  
**Decision:** Architecture AGREED for implementation planning; Backend, Web, Android and Release reviews still required in IMP-0007.  
**Owner:** Architecture / Planning (P).  
**Requests:** XTR-20261022-W-01 (Backend work), XTR-20261022-B-01 (Architecture decisions).  
**Impacts:** IMP-0001, IMP-0004 (workspace isolation), IMP-0007 (media).  
**Release state:** NOT IMPLEMENTED, NOT TESTED, NOT DEPLOYED.

## 1. Objective and separation of responsibilities
- A **static decorative reference asset** (dashboard illustration, branding, text-free icon) can ship directly with Web/Android. Backend image APIs must NOT block local rendering of already bundled and committed artwork.
- A **managed media resource** (user-supplied family photo, member/group avatar, task/category icon override or curated selectable catalog image) requires Backend-managed authorization, storage, assignment and lifecycle. A media record is not the graphic/HTML UI itself.
- A mockup screenshot, complete flattened card, or crop with baked-in text is NOT an acceptable production UI image. Web/Android render real localized labels, values and live controls. Curated images must be verified, text-free illustrations/assets with known provenance.
- User prefers managed images **in the database**. For FamilyPoints' initial scale, choose **PostgreSQL for metadata AND image bytes** rather than privately writable VPS folders or an external cloud object store. This makes DB backup, restore and DEV/BETA separation explicit and atomic. Future object-storage migration is possible behind the same media API but is NOT authorized implicitly.

## 2. Ownership and available slots — v1
The workspace is the security boundary and owns all v1 private media; independent workspaces must never share uploaded binaries or an upload reference.

| Media target | Slot | Owner/entity | Who may change |
|---|---|---|---|
| Family/workspace | COVER, ICON | Workspace (`families`) | MANAGE_WORKSPACE or equivalent scoped workspace manager |
| Member | AVATAR | WorkspaceMember (`family_members`) | MANAGE_MEMBERS / scoped delegated right; self-avatar only if explicitly allowed by the server for that own member |
| People group | ICON | MemberGroup (`member_groups`) | existing scoped group management right, not automatic family-wide admin |
| Task folder/category | ICON | WorkNode(type GROUP), not MemberGroup | scoped task-hierarchy management right |
| Task | ICON override | TaskDefinition or its TASK WorkNode | scoped task management right |

Do **not** conflate UserAccount (global login identity) with WorkspaceMember. V1 does **not** create a global account avatar shared between unrelated families. The same global identity in multiple families may have distinct workspace member avatars. Global avatar is a separate future consent/privacy ADR.

RewardDefinition may adopt the same target abstraction later but must not delay the v1 slots. Differentiate family COVER (wide) from family ICON (square). Do not store image binary inside TaskInstance or point transaction.

## 3. Effective image rules / inheritance
- Explicit **target-level** selected upload or curated catalog asset → for TASK only, nearest authorized parent WorkNode(GROUP) icon → system-curated fallback by target type → initials/generic accessible placeholder if no catalog asset is available.
- For group/type fallback, target's current effective image is resolved at read time. Clearing an override restores inheritance; changing a category/group icon updates only descendants without individual override.
- A TaskDefinition associated with WorkNode(TASK) follows its ancestor WorkNode(GROUP) hierarchy; no inheritance through MemberGroup people trees unless a separate user-facing feature is agreed.
- Historical task execution/ledger content is immutable; dynamic icon resolution does not rewrite historical transactions. Clients may cache effective refs with version/ETag and invalidate when binding/ancestor changes.
- Avoid a 404 on missing optional image; return fallback metadata, and use a local SVG/initial placeholder even during offline use.

## 4. PostgreSQL persistence and safe migrations
Suggested additive schema (Backend owns exact DDL and migration numbering):
1. `media_assets`: opaque UUID key, nullable `workspace_id` for system curated assets (non-null for private uploads), `origin=CATALOG|UPLOADED`, status, content fingerprint, safe MIME, dimensions, original/normalized size, version, timestamps, optional catalog key, creator/audit fields.
2. `media_blobs`: `media_asset_id`, variant `ORIGINAL_SANITIZED|THUMB|CARD|COVER`, `BYTEA` payload, optimized MIME/size/dimensions/checksum. Originals are **sanitized/re-encoded**; never retain unsafe EXIF/GPS original bytes. Bound thumbnail variants may be generated at ingest.
3. `media_bindings`: workspace id, `target_type`, `target_id`, `slot`, `asset_id`, updated-by/version/time, unique target+slot within workspace; enforce target existence/owner via service and meaningful database constraints where practical. A binding must reference either a system catalog asset or an UPLOADED asset owned by the same workspace.
4. System catalog keys are stable and not user-supplied URLs. Approved binary assets are versioned in the application's controlled resources and safely imported into the database with idempotent seeding **outside schema migrations**. Do not seed fictional user members/tasks/images or upload crop assets with baked-in text.
5. Add nullable response fields to old DTOs / a separate read endpoint; existing clients and data remain valid. Any existing PUT that omits media must **preserve its existing binding**; explicit clearing must be a separate operation.

Media bytes in PostgreSQL count toward backup sizes: no external media writable volume for v1, but Release must check disk/backup/restore budget and workload limits. Suggested conservative v1 resource limits: up to 5 MiB input, max 4096x4096 dimensions / 16 megapixels, normalized thumbnails/covers, per-workspace quota configurable (initial target 100 MiB, not a product guarantee). Backend and Release may refine via documented resource testing without changing data ownership.

## 5. Media API contract — proposed endpoints, consumer-review required
- `GET /workspaces/{workspaceId}/media/catalog?category=...` returns only vetted curated keys/metadata for selection; optional public versioned read-only catalog URL for text-free assets.
- `POST /workspaces/{workspaceId}/media/uploads` authenticated multipart, returns opaque media id + variants only after successful validation and persisted normalized bytes.
- `GET /workspaces/{workspaceId}/media/{assetId}/variants/{variant}` authenticated and authorized for **specific bound target visibility**, not merely knowing UUID. Follow privacy policy for children and cross-family scopes.
- `PUT /workspaces/{workspaceId}/media/bindings/{targetType}/{targetId}/{slot}` with `{ "assetRef": { "kind": "CATALOG|UPLOAD", "id": "..." }, "expectedVersion": ... }` atomically assigns allowed media after scoped target authorization; explicit `DELETE` clears override and restores default/inheritance.
- Existing authorized family/member/task DTOs gain `effectiveImage` metadata: mediaRef/catalogKey, variant hints, source OWN|INHERITED|DEFAULT, version/ETag, and an authenticated fetch path; **do not expose unrestricted raw private URLs**. Avoid base64 expansion in listing DTOs.
- This is a proposed API shape: Backend can refine names in a recorded nonbreaking IMP-0007 review; Web/Android must not silently assume exact URL shape before ACK.

**Web bearer-token caveat:** native `<img src>` does not attach JWT Bearer headers. Web should use authorized fetch → Blob/objectURL with cleanup, or a separately reviewed secure short-lived signed resource mechanism. Android uses an authenticated HTTP client. Catalog images may use cacheable public/static URLs; private workspace images must not become publicly readable because a URL leaked.

## 6. Privacy, validation and lifecycle
- The server validates workspace ownership, target visibility and target-specific edit permission on **both upload/assignment and every private read**; member-group scoped delegates cannot escalate into unrelated families.
- Accept only JPEG/PNG/WebP with content sniffing and decode/re-encode; do not allow executable SVG, remote URL fetch, local path import, ZIP, image bombs or arbitrary content types. Enforce input byte count, output dimension/pixel count, and image processing memory/time limits.
- Normalize rotation/color, strip all EXIF/GPS metadata, produce size-appropriate thumbnail/card/cover variants; return `X-Content-Type-Options: nosniff` and safe content disposition. No malicious tracking/referrer leakage.
- Private thumbnails require `Cache-Control: private` with bounded cache lifetime and cache invalidation/revocation on permission changes; purge/logout/account-switch clears workspace-specific client caches. Do not store raw private image URLs in logs, analytics, public HTML or unscoped CDNs.
- Assign/clear with optimistic versioning. Keep uploaded bytes until no bindings/references remain, then soft-mark orphaned and garbage collect only after configured grace period (suggest at least 30 days) plus backup/restore retention constraints. Do not delete curated catalog bytes on user unlink.
- Atomic DB transaction for upload metadata+bytes and assignment; no orphan files. Upload duplicate/idempotency and concurrent edits need tests; rollback must not strand broken image refs.
- Audit actor + workspace + target + old/new ref for assign/removal, but not image bytes, secrets or private identity details.

## 7. Integration order, verification and Release gates
1. Architecture decision is recorded here; Backend can proceed to an **isolated implementation branch**, while reviewing this contract with Web/Android/Release. Backend may not silently ship breaking contract changes.
2. Backend: safe additive migration, normalized byte storage, curated catalog import, workspace-scoped upload/read/assign/clear, effective image resolution and tests; then provide consumer DTO and exact SHA. Run real PostgreSQL Testcontainers fresh and upgrade tests and two-workspace negative authorization.
3. Web: use existing checked-in static decorative assets immediately, independent of media API; later add select/upload/crop component with private Blob reads, family/member/group/task bindings and graceful fallback.
4. Android: native Compose image picker, resize/upload, private scoped cache and offline fallback; preserve role access and app theme behavior.
5. Release: verify DB/media growth limits, backup/restore (including binary bytes), migration from existing DB, Web/Android backward-compatibility, CI and credentials; no deployment until independent approval.

**Required acceptance:** workspace A must not fetch/upload/assign/delete B's asset even knowing UUID; unauthorized child must not see another member's private image; task category inheritance and override/reset stable; unsafe/huge/EXIF payloads rejected/sanitized; original stable catalog versioned; old update clients do not clear bindings; two-family DB migration and restored bytes work.

## 8. Non-goals and outstanding reviews
No cloud object store, uncontrolled file share, CDN for private images, global identity avatar, content-moderation feature, user-accessible arbitrary URL image, or auto-generated mockup graphics. Release resource quotas and detailed ACL policy for self-avatar require track implementation review. This ADR unblocks **design**; it does not claim Backend code or database tables exist.

**User-facing image issue:** The approved 11 dashboard reference crops remain a separate **Web static asset packaging/import** task; absence of this Backend API does not prevent Web from showing clean local assets. The actual ZIP contains cropped reference images and may require artwork/typography separation per design rules before final acceptance.
