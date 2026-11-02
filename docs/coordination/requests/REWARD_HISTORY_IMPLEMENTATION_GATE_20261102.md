# Reward History — Backend source audit and Android/Web implementation gate (2026-11-02)

**Priority:** P1, coordinated cross-platform. Builds on REWARD_HISTORY_20261102.md and REWARD_HISTORY_ANDROID_20261102.md. **Actual source verified on main 93052fe4.**

## Source facts (not assumptions)
- Backend RewardController already exposes `GET /workspaces/{workspaceId}/rewards/purchases`, `/requests`, and `/obligations/open`, but **no dedicated history endpoint**.
- `RewardPurchaseResponse` already contains id, rewardDefinitionId, memberId/name, rewardTitle **snapshot**, pointTypeId/code, priceAmount, durationMinutes, status, purchasedAt, resolvedAt.
- Purchase status enum is **PENDING_APPROVAL, READY, REDEEMED, CANCELLED**. A state named DELIVERED or RECEIVED must **not** be fabricated from READY/REDEEMED. `RewardObligation` has OPEN/COMPLETED semantics independent of purchase.
- `RewardRequestResponse` has requestedBy member, title, status, approved price/duration, createdAt and reviewedAt. Request and purchase cannot be concatenated without explicit stable identity, event type and deduplication rules.
- `RewardsPage.jsx` tabs are catalog/requests/manage; manage-only `recentPurchases` renders `purchases.slice(0,10)`; Android contract/user-visible history not yet verified.

## Backend coherent package
1. Create read-only, paginated `GET /workspaces/{workspaceId}/rewards/history?memberId=&cursor=&limit=&status=&from=&to=` (illustrative endpoint; publish tested contract). Return stable order by (eventAt DESC, eventType, eventId), next cursor, filter authorization and immutable source event ID. Use *existing persisted* purchase/request/obligation states first; no placeholder event statuses.
2. Member/child sees only self, regardless of supplied memberId; authorized manager sees workspace scoped members. Query/joins must tenant fence source rows, snapshots and IDs. Test cross-account same family and different family, invalid cursor, cancellation, stale session/epoch.
3. Define a canonical DTO: eventId, eventType (REQUEST/PURCHASE/OBLIGATION as actual source), eventAt, memberId/name, rewardTitle, priceAmount/currency, durationMinutes, sourceId, sourceStatus, optional linkedRequestId and purchaseId, plus pagination. Dates normalized and deterministic.
4. **Separate functional extension (explicit agreement required before claiming support):** parent confirmation of reward receipt, TIME start/end and usage. Current purchases already have `REDEEMED`, but its meaning must be audited before identifying it as received; if extra lifecycle events needed, implement a durable, authorized append-only event model with actor/time and idempotent mutation endpoints, not an unverified UI toggle.
5. Ensure history retention, reward history FK and economic ledger consistency in FULL/SELECTIVE Backup/Reset/Restore under ADR-FAMILY-RESET-002.

## Web & Android implementation
- Web: add History as fourth peer tab; remove 10-row truncation for history, cursor pagination, filters, explicit loading/empty/errors and four locales. Catalog and request actions must not POST on navigation.
- Android: first-class Rewards > History, member own and manager filtered family view, DTO+cursor paging, cached history **read only** keyed by account/workspace/member/dataEpoch, invalidate on switch/reset. Do not mark received from a local cache action.
- Designer: item timeline/visual labels exactly for server-supported statuses, distinguish purchase vs receipt; translations Ukrainian/Russian/German/English.
- QA: 11+ purchases, old renamed/deleted reward title snapshot, correct cursor order and dedup, member privacy, manager filters, wrong-family response, cached cross-session quarantine and no phantom ledger entries. Synthetic first; device/live tests remain deferred.

**Acceptance:** An ordinary member sees own complete purchase history beyond 10; parent sees selected child's history; no unauthorized family/member data; no invented delivered status; GET-only history view, zero ledger mutations. Feature completed only after Backend API tests + Web build/QA + Android Gradle source tests.
