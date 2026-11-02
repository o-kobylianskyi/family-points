# FamilyPoints — Android Reward History, coordinated with Web

**Date:** 2026-11-02. **Priority:** P1. **Product decision:** user explicitly requests Reward History in Android as well as Web. **Parent requirement:** docs/coordination/requests/REWARD_HISTORY_20261102.md.

## Android UI
- Add a first-class **History** tab/section under Rewards, not an admin-only recent purchases snippet.
- Child/member sees **own** reward requests, purchases and delivery/consumption history. Family manager with authorized permission can filter by member, status, reward and date. Parent viewing a child's history must enforce server ACL; never trust a client memberId alone.
- Show chronological event cards (newest first) with reward title/snapshot, beneficiary, timestamp, point currency and charged amount, lifecycle state, time-reward duration when supported. Paginate / load more, avoid hard-coded 10-row truncation.
- Clearly separate requested/approved/purchased/received/used/expired. Do not claim delivery/receipt if Backend has only purchase state; unknown historic fields need honest fallback.
- Ukrainian/Russian/German/English translations, both themes, compact mobile layout, loading/error/empty states and existing collapse behavior where applicable.
- Offline: cached **read-only** history only; scope cache to account+workspace+member and dataEpoch. On account/family switch or epoch change quarantine stale state and reload. No offline acknowledgement or speculative ledger changes.
- Use the **same Backend history API contract** as Web. Before Backend publishes it, Android can prepare UI/model but must not invent endpoints.
- Future received/duration/consumption confirmation requires dedicated authorized idempotent server action and audit; not a local-only status toggle.

## Contract dependency / acceptance
Backend: confirm RewardController currently exposes purchases/requests/open obligations; inspect actual persisted lifecycle and provide paginated member-safe history endpoint and event semantics. Avoid conflating purchase with delivery.
Android: implement complete list, filters and member permissions in one coherent package rather than small scattered tweaks.
QA: prove child own vs sibling denial, manager family filter, 11+ purchases, chronological ordering and page boundary, timestamp/point currency consistency, revoked member and cross-tenant isolation, role switching, offline epoch invalidation, no duplicate writes on viewing history.
Designer: unify status icons, terminology and timeline design across Android and Web.
Architecture: include reward history and lifecycle references in FULL/SELECTIVE BKL-002 backup/reset/restore matrix.

No real user data mutation or app release is authorized by this request.
