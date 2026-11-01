# Issue #18 — Replayable import receipts v1 (Backend, 2026-11-01)

**Status: SOURCE COMMITTED; pinned CI + QA required before consumer use.** Additive APIs. No reset, mass activation, ledger mutation, deployment or migration of real databases authorized.

## Why
Existing `POST /workspaces/{id}/templates/imports` is deduplicated by catalog code but does not produce an immutable operation receipt. After a client timeout it may be impossible to distinguish a committed import from a failed request. Existing activation's `Idempotency-Key` protects against duplicate activation but returns 409 on replay, not the prior object result. This change addresses **catalog import only**, not activation receipts.

## New endpoints
- `POST /workspaces/{workspaceId}/templates/imports/idempotent`
  - Header `Idempotency-Key: <UUID>`; `X-Workspace-Data-Epoch: <current server epoch>`.
  - JWT; `canManageWorkspace(workspaceId)`.
  - Body `{"selectedCodes":["<actual catalog code>", "..."]}` with no duplicate/blank/null entries and no more than the current catalog size (142 at this checkpoint).
  - Response `{"operationId":"<uuid>","replayed":false,"importResult":{"selectedCodes":[...],"alreadyImported":[...],"toImport":[...]}}`.
  - First request imports private definitions **without activation** and stores exact result in `workspace_template_import_receipts` within the same transaction. Reuse of the same workspace+operationId+same ordered selection returns the stored result with `replayed:true`, without a second import. Different selection and same key returns 409 `TEMPLATE_IMPORT_OPERATION_CONFLICT`. Invalid request returns 400. Stale epoch fails before receipt access.
- `GET /workspaces/{workspaceId}/templates/imports/receipts/{operationId}`
  - `canManageWorkspace`, no-store; reads the exact committed result, `replayed:true`; 404 if missing or belonging to a different workspace. No mutation.

## Safety invariants
1. Family row fenced in same transaction before receipt read, import writes and receipt insert.
2. Existing `/imports`, `/imports/preview`, private template editing, and TASK/REWARD activation endpoints remain unchanged.
3. `workspace_template_import_receipts` V47 is append-only per family+UUID (FK protects family); no backfill, no account or existing family edits.
4. Return counts are the **original operation result**, not a recomputed current view; refresh private templates independently for live state.
5. Existing fully imported codes are not duplicated because original private import keeps `(workspace_id,catalog_code)` conflict exclusion.
6. Family switch must clear cached operation keys. Offline clients must not generate new operations automatically after timeouts; query receipt first.
7. Current receipt fingerprint includes the ordered list. Same codes in different order under same key are considered a conflict. This is intentional and must be preserved by consumers.

## Independent verification
New disposable PostgreSQL tests cover replay with identical response, changed request conflict, cross-family receipt isolation, rollback on failure and concurrent same-key submissions. Require exact-head CI acceptance and actual HTTP security/body tests before enabling consumer calls.

## Pending / NOT implemented
- Immutable results for TASK/REWARD activation requests: current 409-on-duplicate remains.
- Server-side review/commit of group/template/edited task and reward drafts as one atomic wizard operation.
- Approved Designer catalog-to-group mapping; do not seed group from catalog category alone.
- Full `setupState` reconciliation, POST_RESET state (blocked by BKL-002 execute423), Web/Android runtime integration.
- Consumer-facing standardized stable error JSON, permission QA, current deployed schema verification.

## Web / Android handoff
On user-confirmed import, use UUID once, persist pending key scoped by account+workspace+epoch+selection, send Idempotency-Key and epoch. On timeout **GET receipt** before retry; retry only exact same key/payload after verifying current epoch. Show imported vs previously imported counts, no automatic activation/purchase/price zeroing. Child or unauthorized member must not call manager import endpoints. QA targets fresh disposable family and separate workspace A/B.
