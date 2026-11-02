# Reward Purchase History v1 — Backend source contract (2026-11-02)

Status: SOURCE COMMITTED; compile/integration/CI and consumer acceptance PENDING. This is the first read-only slice of Architect REWARD_HISTORY_IMPLEMENTATION_GATE_20261102.md. It MUST NOT be advertised as the complete Reward History event stream.

## Endpoint

`GET /workspaces/{workspaceId}/rewards/history?memberId=&cursor=&limit=&status=&from=&to=`

Authenticated existing workspace middleware and RewardController.assertWorkspace.
- Normal member: server forces `currentMemberId`; explicit another `memberId` is rejected. Client filters are not trusted.
- Manager: requires existing MANAGE_REWARDS or ADMIN_OVERRIDE and may select an existing member *within current workspace*; missing memberId means all workspace purchases.
- `limit`: 25 default; 1..100. Keyset order `purchasedAt DESC, id DESC`; query is bounded to limit+1 with source membership+workspace fence.
- `cursor`: base64url (unpadded) ASCII `purchasedAt|id`; opaque transport, not an authorization token. Incorrect format fails closed. Filtering cannot escape workspace scope, including when cursor was obtained for another member.
- `status`: existing RewardPurchaseStatus values PENDING_APPROVAL, READY, REDEEMED, CANCELLED only. `from`, `to` are ISO LocalDateTime bounds against purchasedAt, inclusive.
- Response: `{items: RewardPurchaseResponse[], nextCursor: string|null, hasMore: boolean}`. Existing purchase snapshot title, amount, point currency, duration, status and purchasedAt/resolvedAt preserved.
- No POST, ledger write, inferred DELIVERED/RECEIVED status, request-history merge or obligation inference. Historical snapshots are not re-priced.

## Important scope gap before cross-platform full Reward History
- This v1 returns PURCHASE rows only. Reward requests and obligation completions are not emitted. Do not label this API as a complete REQUEST/PURCHASE/OBLIGATION lifecycle stream.
- A unified event-based history needs explicit event identifiers, event types, stable cross-type cursor, request-to-purchase deduplication, lifecycle semantics and server ACL tests.
- `REDEEMED` is the existing purchase status, NOT evidence a parent confirmed physical receipt, and no parent acknowledgement endpoint is authorized.
- Backend source still requires current-head Java compile + PostgreSQL integration (11+ purchases, tie timestamps/page boundary, manager/member sibling denial, workspace A/B, invalid cursors, canceled/renamed reward snapshot, no ledger mutation), then Web/Android consumer tests and QA.
- BKL-002: history retention/FK/ledger provenance must be approved under reset/restore matrix before any execute.

## Safe example

```json
{
  "items": [],
  "nextCursor": null,
  "hasMore": false
}
```

Do not call the status FINAL or use this as evidence Reset Execute can be unlocked.
