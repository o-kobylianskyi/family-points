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

## 2026-10-31 implementation delta — password challenge (NOT execute)
- Backend POST /challenge currently **requires additional `password`** alongside `operationId` and `snapshotDigest`. It validates local BCrypt account password, workspace manager, same initiating member, `CONFIRMED` operation and matching digest; stores five-minute single challenge in V43 and returns `{operationId,challengeId,expiresAt,executionEnabled:false}`. Existing passwordless Google accounts cannot use this path: alternate recent OAuth reauthentication is still pending. Incorrect password/role returns 403; invalid body 400; missing operation 404; stale/unconfirmed or duplicate challenge 409. Rate limiting and fresh data snapshot verification are NOT complete. Web must keep destructive UI disabled and must not assume this challenge is authorization to execute.
- POST /execute remains an unconditional 423 for authorized workspace. The original proposed `methods`, `confirmationPhrase` and `reauthProof` contract is **not yet implemented**; do not build Web calls against the proposed shape before final backend security review.

## 2026-10-31 — Implemented source delta: content-v2 and lock order (CI pending)
This supersedes earlier implementation-gap paragraphs; proposed execute DTO above remains unimplemented.
- Prepare/confirm/challenge require the original epoch and lock family before operation rows. Wrong-family/initiator ACL remains in place.
- snapshotDigest is opaque **reset-content-v2**, SHA256 over canonical selection, current epoch, scoped row CONTENT and FK metadata. Any same-family scoped edit may invalidate it even outside selected categories; unrelated family changes and protected global credentials do not. Counts alone no longer establish freshness. Old count-v1 operations MUST be recreated with NEW operationId; never replay them as approved.
- Confirm rechecks current content and returns409 RESET_SNAPSHOT_STALE before status change. Challenge checks content again after password reauth, before proof metadata insert. Protocol audit/challenge/throttle rows do not themselves invalidate digest.
- Password attempt limiting persists in REQUIRES_NEW before family fence; >5 attempts/15min returns429. Starting challenge within an already same-family fenced application transaction returns409 RESET_REAUTH_NEW_TRANSACTION_REQUIRED, avoiding a self-block through limiter FK.
- Current challenge request is {operationId,snapshotDigest,password}; response {operationId,challengeId,expiresAt,executionEnabled:false}. No usable verifier secret is exposed. Google fresh reauth, challenge consumption and execute protocol remain incomplete.
- Execute continues unconditional423 RESET_EXECUTION_DISABLED for authorized callers. No automatic consumer retry, no destructive CTA, no live reset/deployment approval. HTTP error reasons are not yet the proposed universal machine-readable code DTO.

## Source/CI acceptance — 2026-10-31
Exact tested SHA `4d88716a9cde33a15a7f94867ee02d2a69b613c3`; [Actions 41234461782](https://github.com/o-kobylianskyi/family-points/actions/runs/41234461782) all four jobs SUCCESS. Backend: 313 tests, zero failures/errors/skips; 39 actual Spring/JPA/PostgreSQL transaction scenarios.
Content-v2, same-count stale confirm/challenge, protocol stability, persisted failed-password budget and nested-lock rejection verified. This supersedes CI-pending labels for this bounded delta only. Universal error-code DTO, Google fresh reauth, owner/financial/FK policy, usable one-use execution proof and execute remain unimplemented/unapproved. Consumer/runtime review and deployment gates still pending; authorized execute is always423.

## 2026-10-31 — Internal bound proof lifecycle (source; CI pending)
- HTTP challenge shape stays {operationId,snapshotDigest,password} -> {operationId,challengeId,expiresAt,executionEnabled:false}. No secret is disclosed; /execute stays unconditional423.
- V45 stores data_epoch, snapshot_digest, requested_by_member_id and SHA256 fingerprint of current account credential alongside verifier. Existing old unbound rows are retained but cannot be consumed. An operation already owning such a row requires NEW operationId and fresh prepare/confirm/challenge; no silent migration to valid authorization.
- Internal issue verifies current enabled account, active scoped membership and actual database password after controller's rate-limited reauth. Internal consume validates current operation/content/credential/actor/family/epoch, wall-clock expiry and exact verifier, then marks consumed in caller's existing transaction. Concurrent consumption has one winner; caller rollback restores availability. This does not authorize deletion or prescribe retry behavior for future execute.
- Proof service is a trusted internal building block: HTTP caller owns rate limiting; future executor must separately enforce final owner/role/financial/dependency policy and consume in SAME transaction as deletion/audit/epoch. Google fresh reauth and secure proof delivery/consumption HTTP DTO remain unimplemented pending final review.
