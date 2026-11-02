# FamilyPoints — Reward History gap: complete user-visible reward lifecycle

**Date:** 2026-11-02. **Priority:** P1 for early BETA usability (coordinate with Issue #18 and BKL-002). **Status:** SOURCE-CONFIRMED UI GAP; implementation not yet accepted.
**Source:** frontend/src/pages/RewardsPage.jsx tab set: catalog, requests, manage; purchases shown only in canManage 'recentPurchases' as purchases.slice(0,10). Backend RewardController GET /workspaces/{workspaceId}/rewards/purchases and /requests and /obligations/open already exist. No separate history tab observed. Do not claim all receive/use state is persisted without Backend audit.

## Product requirement / UX
Add an independent 'Reward history' tab in Web Rewards and equivalent Android screen/section. Each member sees own complete entitled/purchased/redeemed history; authorized family managers can filter by member and status. Keep history separate from available catalog and active requests. Make list chronological with date/time, reward name (snapshot at transaction), beneficiary, points/currency charged, source and current lifecycle status. Explicitly distinguish REQUESTED, APPROVED/REJECTED, PURCHASED, DELIVERED/RECEIVED, ACTIVE/TIME_STARTED, COMPLETED/EXPIRED/CANCELLED where supported; never invent a status and clarify missing backend events.

User desires parent to acknowledge reward received and time-based reward duration/remaining time. First audit whether receivedAt/confirmedBy/consumedAt etc actually exist; if not, design schema+API append-only lifecycle history with actor/time, immutable audit and idempotency. Avoid treating purchase as delivered. Do not reset accrued ledger/purchases by a UI display change.

UX: pagination/load more (not slice(0,10)), filters by member/type/status/date, newest first; default member sees own; manager full family with membership ACL; localized UA/RU/DE/EN, mobile-responsive and max initial items with Show all/collapse as appropriate. Display empty states and loading/error, account/family epoch isolation, no cross-family stale history.

## Owner dispatch
- Backend P0 technical investigation / P1 implementation: inventory RewardPurchase, RewardRequest, obligation and entitlement lifecycle; confirm persisted statuses and API response shapes. Add read-only paginated scoped history endpoint; only implement received/consumed acknowledgement semantics if agreed with Economy v0.7 and transaction rules, actor/permission + idempotency audit. Preserve existing /purchases compatibility. No synthetic backdated received status.
- Web P1: separate Reward History tab for everyone; member-only own, manager filters, all purchases and related event state, no arbitrary 10-row truncate. No create/purchase POST on navigation.
- Android P1: equivalent history list and filters after API contract, offline cache read-only (old epoch invalidates). No background acknowledge purchase/received.
- Designer P1: timeline semantics, icon by reward kind (TIME/ITEM), neutral states, four languages and delivery vs purchase distinct.
- QA P1: child cannot see siblings; managers can filter; pagination total/ordering, historical reward after definition edited/deleted, duplicate replay, TIME status transitions, incomplete event fallback, cancellation, cross-tenant A/B and stale account switching. Verify no duplicate economic ledger writes.
- Architecture: ensure BKL-002 FULL/SELECTIVE reset and file restore categories include reward requests/purchases/obligations/event history with dependency and retention policy; immutable security audit separately preserved.

## Acceptance
Historical purchase remains visible when older than 10 records, with correctly scoped member, timestamp and price; parent can inspect one child's history; own member cannot inspect another child's history; purchase/received/consumed are never conflated; tests and actual UI source confirm completeness.

This ticket does not authorize real family mutation, migration or release.
