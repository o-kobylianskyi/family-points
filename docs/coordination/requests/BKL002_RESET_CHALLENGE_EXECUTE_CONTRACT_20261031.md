# BKL-002 — Backend ↔ Web reset challenge/execute contract (proposal, 2026-10-31)

**Status: PROPOSED / NOT DEPLOYABLE / EXECUTE DISABLED.** This file aligns consumers; it does **not** authorize deletion. Current live code implements GET /preview, POST /plan, POST /prepare, POST /confirm, GET /data-epoch; **no POST /challenge or /execute endpoint**. Existing /confirm only checks literal phrase + stored digest + initiator; it does NOT reauthenticate and therefore is not sufficient authorization.

## Base
`/workspaces/{workspaceId}/data-reset`. JWT authentication, family-manager permission `canManageWorkspace`, member/workspace binding; all mutations carry `X-Workspace-Data-Epoch`. Return JSON, always no-store. Web must not render destructive CTA until backend exposes an enabled, security-tested execute contract and QA signs off.

## Existing endpoints — do not rename
- `GET /preview`: {workspaceId,executionEnabled:false,completeDependencyPlan:false,alwaysPreserved,categories}. Each category has code,recordCount,executable:false,warning.
- `POST /plan`: request {categories:[codes...]}; result {workspaceId,selectedCategories,executable:false,blockers,preserved}.
- `POST /prepare`: request {operationId:UUID,categories:[codes...]}; response {operationId,workspaceId,categories,snapshotDigest,status:'PREPARED',executable:false}.
- `POST /confirm`: request {operationId,snapshotDigest,phrase:'RESET <workspaceId>'}; response {operationId,status:'CONFIRMED',executable:false}. **This only records acknowledgement, NOT a security challenge**.
- `GET /workspaces/{workspaceId}/data-epoch`: {workspaceId,dataEpoch}.

## Proposed challenge — Backend P0 implementation needed
`POST /workspaces/{workspaceId}/data-reset/challenge`
Request: `{operationId:UUID,snapshotDigest:string}`.
Response: `{operationId,challengeId:UUID,expiresAt:ISO8601,confirmationPhrase:string,methods:['PASSWORD'|'RECENT_REAUTH'],executionEnabled:boolean}`.
- Server issues short-lived, one-use, workspace+account+operation+snapshot-bound challenge; no account password in response. Support Google-only accounts through approved fresh reauthentication method, not fictional password.
- Reject when plan still has blockers, FK closure unknown, snapshot stale, membership changed, or generation changed. Server generates a secure unpredictable challenge secret and keeps only its verifier. Rate-limit challenge/verification attempts. Do not claim existing /confirm fulfills this step.
- Currently `executionEnabled=false`; Web may show non-destructive readiness only.

## Proposed execution — Backend P0 implementation needed
`POST /workspaces/{workspaceId}/data-reset/execute`
Request: `{operationId:UUID,challengeId:UUID,confirmationPhrase:string,reauthProof:string,expectedDataEpoch:long,snapshotDigest:string}`, plus `X-Workspace-Data-Epoch`.
Success `200`: `{operationId,workspaceId,status:'SUCCEEDED',previousDataEpoch,nextDataEpoch,deletedCounts:{CATEGORY:integer},onboardingRequired:true,executedAt:ISO8601}`.
- All selected-category deletions, audit finalization and epoch increment MUST commit in one SQL transaction under family row lock, with DB-scoped idempotency; same operation/key replay returns identical final outcome, mismatching payload returns 409.
- Server independently re-checks family manager, actor, challenge, FK closure, ledger protections, snapshot, epoch, outstanding offline actions. Reset scope MUST preserve accounts, workspace identities, other families and global templates; never auto-clear ledger or balances without explicit approved financial policy.
- On any failure: rollback entirely and leave epoch unchanged. Challenge verifier must be single-use; no implicit retries. Client should re-read epoch and state after success and enter onboarding; no write replay.
- **Execution must remain disabled** until all listed safeguards, concurrency/fault-injection PostgreSQL tests, Web/Android coordination and disposable-workspace QA are complete.

## Error contract
`400 VALIDATION_ERROR`: invalid UUID/selection/body. `401 UNAUTHENTICATED`; `403 FORBIDDEN` wrong role/family/actor; `404 RESET_OPERATION_NOT_FOUND`; `409 RESET_SNAPSHOT_STALE | RESET_EPOCH_STALE | RESET_OPERATION_CONFLICT | RESET_DEPENDENCY_BLOCKED`; `410 RESET_CHALLENGE_EXPIRED`; `423 RESET_EXECUTION_DISABLED` until feature-enabled. Use stable machine-readable `code` with localized Web messages (never parse human text).
No implementation may claim these stable error bodies currently exist; HTTP mappings are target only.

## Web implementation gate
Maintain preview/plan/prepare/read-only state. Do not call /challenge or /execute until Backend publishes implemented DTOs and exact-head CI, PostgreSQL tenant/rollback/idempotency proof, plus QA approval. Web owns four-locale wizard, impact counts, reauth UX, stale-epoch quarantine, explicit onboarding, no automatic replay. Android owns epoch sync/queue quarantine. Release owns green pinned SHA and backups.

Backend current gap: `WorkspaceEpochTransactionGuard` is reusable but not wired into all mutating services; HTTP filter alone cannot prevent concurrent reset/write TOCTOU. Web must treat full reset as UNAVAILABLE.
