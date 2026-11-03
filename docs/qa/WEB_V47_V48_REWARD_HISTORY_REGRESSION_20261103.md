# Web V47/V48 + Reward Purchase History v1 — regression and release gate

**Date:** 2026-11-03. **Owner:** Web. **State:** source committed, exact-head build/QA not yet confirmed. No deploy, real family reset or destructive action.

## Scope verified from Backend source

- V47 `POST /workspaces/{id}/templates/imports/idempotent`, read receipt; exact ordered catalog codes and UUID operation, epoch guard.
- V48 `POST /workspaces/{id}/templates/{templateId}/activate-task/idempotent`, corresponding reward activation and GET receipt; result is `createdDefinitionId`, not full task/reward DTO.
- Purchase History v1 `GET /workspaces/{id}/rewards/history`; keyset `purchasedAt DESC, id DESC`; `items, nextCursor, hasMore`, filters `memberId,status,from,to,limit,cursor`. **NOT** full reward request/fulfillment lifecycle.
- Allowed purchase statuses: PENDING_APPROVAL, READY, REDEEMED, CANCELLED. Do not infer DELIVERED or parent-confirmed RECEIVED.
- Backend read ACL: member only own; manager can inspect entire workspace or one same-workspace member. Web rejects a response containing sibling rows for own or selected-member views.

## Web commits addressed in this gate

- V47: `5779070`, `8677770`, `1137162`, `16da330`
- V48: `4dcb9fe`, `50cf9b6`, `9aa6687`, `8796b91`, `5aa038f`
- Purchase History: `def7f7b`, `45cc355`, `e72f391`, `df9044e`, `b1886fd`
- Regression corrections: `ccaf1c2` undefined load-more validator, `3202560`/ `9326078` member scope, `54498d4` test.

## Synthetic acceptance matrix (only disposable family, no live data writes)

| ID | Scenario | Expected result | Evidence |
| --- | --- | --- | --- |
| W-47-01 | Select 0 templates | No POST; explicit selection needed | Manual/Playwright pending |
| W-47-02 | Confirm mixed TASK/REWARD and lose POST response | Original UUID and ordered payload remain; GET receipt before explicit replay | Backend integration/Playwright pending |
| W-47-03 | Switch family or change epoch during import | Old operation not replayed in new family/epoch | Node unit + browser pending |
| W-47-04 | Same UUID different ordered selection | Backend conflict, no extra imports | Backend test pending pinned CI |
| W-48-01 | Activate private TASK, recover via receipt | One created TASK, receipt ID used as ID only | Backend+browser pending |
| W-48-02 | Activate private TIME REWARD with 30m and zero price | Correct duration and deliberate zero, no purchase/ledger write | Backend+browser pending |
| W-48-03 | Child accesses manager-only private activation | 403, no activation | Backend HTTP/browser pending |
| W-48-04 | Timeout followed by same UUID replay | Exactly one definition; no new UUID | Backend test pending pinned CI |
| W-H-01 | 27 purchase records in two overlapping cursor pages | All unique IDs visible; no arbitrary 10-row cap | Node test added, runtime pending |
| W-H-02 | Member sees sibling's purchase in malformed response | Reject page, display error rather than leak row | Node test added |
| W-H-03 | Manager filter for one child receives another child's purchase | Reject page, display error | Node test added |
| W-H-04 | History during workspace switch/epoch change | Old cursor and rows cleared; no previous identity data | Browser pending |
| W-H-05 | Rename/delete reward definition | Stored snapshot title/amount retained | Backend integration pending |
| W-H-06 | Backend returns unknown delivery status | Fail closed; never invent parent receipt | Node test added |
| W-H-07 | Navigating history or loading another page | GET-only, no economic ledger mutation | Network QA pending |

## Local operator commands (from `frontend` directory)

```powershell
git pull origin main
npm run test:regression
npm run build
npm run lint
```

**Gate:** The actual checks above MUST be recorded with exact SHA and outputs before Web QA PASS. Code commits and older BUILD SUCCESS are not proof for the latest tree. BKL-002 reset/restore and release remain separate blockers.
