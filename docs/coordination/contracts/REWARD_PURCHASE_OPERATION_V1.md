# FamilyPoints — Reward purchase operation / receipt wire contract (Draft PR57)

**Scope:** Backend B V54 additive idempotency candidate, not a deployed API. **BETA HOLD**; no legacy-route migration, active client financial POST, merge or deploy before independent QA/Architecture release gate.

## Endpoint and headers

All endpoints authenticate the current account and use workspace security scope. Both mutations require the client-supplied **original stable UUID** as the `Idempotency-Key` header and a valid `X-Workspace-Data-Epoch` header whenever workspace data epoch is nonzero. At epoch zero an absent epoch header is accepted for legacy compatibility; clients should send the correct epoch explicitly.

| Purpose | Method and route | Role |
| --- | --- | --- |
| Buy for currently authenticated member | `POST /workspaces/{workspaceId}/rewards/{rewardId}/purchase/idempotent` | Active workspace member |
| Buy for selected member | `POST /workspaces/{workspaceId}/rewards/{rewardId}/purchase-for-member/{memberId}/idempotent` | Workspace member with `MANAGE_REWARDS` or `ADMIN_OVERRIDE` |
| Recover recorded purchase | `GET /workspaces/{workspaceId}/rewards/purchases/receipts/{operationId}` | Exact original actor and workspace; manager receipts still require manager permission |

HTTP 200 mutation response and GET receipt JSON shape:

```json
{
  "operationId": "00000000-0000-4000-8000-000000000001",
  "purchase": {
    "id": 123,
    "rewardDefinitionId": 456,
    "memberId": 789,
    "memberName": "example",
    "rewardTitle": "example",
    "pointTypeId": 10,
    "pointTypeCode": "POINTS",
    "priceAmount": 7,
    "durationMinutes": null,
    "status": "READY",
    "purchasedAt": "2026-11-22T20:00:00",
    "resolvedAt": null
  },
  "replayed": false
}
```

Example values are illustrative, not live data. Response is `Cache-Control: no-store`. The actor/workspace/UUID combination identifies one durable receipt. `purchase.id` is authoritative immutable identity; `priceAmount`, `pointTypeId`, `pointTypeCode` are server-derived snapshots of the original purchase. Clients **must not** provide a price, currency, or second generic `spend` for reward purchases.

## Exact identity and reconciliation

- Immutable intent: original `operationId` UUID plus originating actor account, workspace, route kind (`SELF` or `MANAGER`), target member, reward and original validated `dataEpoch`. A different operation with the same key under the same actor+workspace returns 409. Epoch changes invalidate old mutation UUID even if all other fields match.
- Two concurrent same-key submissions serialize on the PostgreSQL family row lock and join the same transactional purchase/ledger/receipt boundary. One response has `replayed=false`; the other `replayed=true`, same `purchase.id`, one original debit.
- A subsequent GET fetches the current persisted purchase entity by receipt link and returns `replayed=true`; it does **not** post a debit. It may succeed after the original POST response was lost.
- Required client reconciliation: match the **exact original UUID and scope**, authoritative purchase/member/reward IDs, point type code/ID and nonnegative original price; never turn an unverified receipt into a confirmed charge or erase the stored original intent.
- `401` authentication failure, `403` workspace/role denied, `404` receipt missing or foreign actor, `409` stale epoch or fingerprint conflict, `5xx` and network timeout do **not** prove the purchase did not commit. Keep the original intent in `UNKNOWN/HELD`; do not automatically retry POST, switch UUID or clear the journal. After authenticating the same account, only an authenticated **GET** by the original UUID is safe to initiate automatically.
- Receipt GET is deliberately **actor-scoped**, not a general family purchase lookup. A separate manager viewing a child's history cannot read another actor's operation receipt with their own account credentials. Use existing authorized purchase history for other use cases.

## Purchase lifecycle (authoritative server behavior)

The receipt row is durable and stores the purchase **ID**, not a fixed status. `RewardPurchaseResponse.from(purchase)` returns the **current** persisted status:

| Current status | Meaning | Client reconciliation |
| --- | --- | --- |
| `READY` | Purchase committed, not proof of reward delivery | Confirm original purchase; show unredeemed |
| `REDEEMED` | Same historical purchase, later redeemed | Confirm original purchase; distinguish redeemed, not usable twice |
| `CANCELLED` | Same historical purchase, now cancelled | `HISTORICAL_PURCHASE_CANCELLED_HOLD`: preserve original intent, present recorded purchase and request review; do not infer refund/no debit or create new POST |
| `PENDING_APPROVAL` | Not an expected completed direct purchase status | Fail closed, retain uncertainty |
| Missing, `FAILED`, unrecognized | Invalid/untrusted direct purchase state | Fail closed, retain original UUID/intent |

**READY is not fulfillment.** CANCELLED does not mean “not purchased” or “refunded.” Refund claims require separate authoritative ledger evidence. The existing approval-request purchase flow is a **different route** (`POST /workspaces/{workspaceId}/rewards/requests/{requestId}/purchase`) and is unchanged by PR57; the new direct operation refuses reward definitions requiring a request/approval.

## Errors and safe boundaries

- `400 REWARD_PURCHASE_OPERATION_INVALID` for missing/invalid operation or target arguments at service layer; HTTP malformed UUID may be rejected by the framework before the service.
- `403 WORKSPACE_ACCESS_DENIED`, `403 MANAGE_REWARDS_REQUIRED`, `403 REWARD_PURCHASE_RECEIPT_MEMBER_DENIED` as applicable.
- `404 REWARD_PURCHASE_RECEIPT_NOT_FOUND` for absent or other-actor receipt. Do not leak foreign account details.
- `409 REWARD_PURCHASE_OPERATION_CONFLICT` for immutable fingerprint reuse, `409 WORKSPACE_DATA_EPOCH_STALE` for stale/wrong epoch, and `409 REWARD_PURCHASE_RECEIPT_BROKEN` for an inconsistent persisted purchase link.
- Existing domain eligibility / unavailable reward / insufficient points responses remain authoritative in `RewardService`, not synthetic UI projections.

## QA and adoption gates

CI unit+PG tests cover exact once debit, same-key concurrency, original price after catalog change, zero cost, insufficient funds, outer rollback, receipt-status drift, actor isolation, stale epoch and manager permission negative. **A passing individual PR SHA does not certify combined release.**

Before either client enables financial purchase POST: independent exact-head CI and review of receipt INSERT-failure rollback, authenticated controller/JWT/role/tenant tests, original UUID recovery after lost response across clients, existing request-approved route non-regression, and frozen V53→V54 combined clean+legacy PG17 migration. No automatic offline reward purchase or generic point spend fallback. Android SQLite on-device tests and Web browser synthetic recovery must be executed on explicitly approved disposable fixtures. Client implementation remains gated behind Architecture and QA acceptance.

**Safety:** all source branches Draft/unmerged. Restore Execute OFF; Reset Execute HTTP423; physical purge OFF. No production/Contabo/real family operations.
