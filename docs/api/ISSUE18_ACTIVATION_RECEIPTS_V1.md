# Issue #18 — Explicit TASK/REWARD activation receipts V48

**Status:** source implementation committed, pending pinned CI and QA. Additive, opt-in client API; no deploy authorized.

## Endpoints
All under `/workspaces/{workspaceId}/templates`, with scoped workspace access.
- `POST /{templateId}/activate-task/idempotent`: `canManageWorkspace` + `CREATE_TASKS`, body unchanged `TaskDefinitionCreateRequest`, `Idempotency-Key: UUID`, `X-Workspace-Data-Epoch`.
- `POST /{templateId}/activate-reward/idempotent`: `canManageWorkspace` + `MANAGE_REWARDS`, body unchanged `RewardDefinitionRequest`, same headers.
- `GET /activations/receipts/{operationId}`: managing user, workspace-scoped read-only recovery, 404 for missing/foreign operation.

Example response:
```json
{"operationId":"00000000-0000-4000-8000-000000000001",
 "templateId":123,"kind":"TASK","createdDefinitionId":456,"replayed":false}
```
Same-key/same-kind/same-template/same-serialized request returns stored ID with `replayed:true` and no second activation. Same key with different fingerprint, kind or template yields 409. A failed creation rolls back the existing V40 claim and V48 receipt together under the family's original-epoch transaction lock. Receipt is immutable. The ID identifies the created object; consumers should retrieve its latest definition separately using existing scoped API rather than assuming the original full DTO is persisted.

**Current limitations:** The established legacy `activate-task` and `activate-reward` endpoints retain 409-on-duplicate behavior. A V40 operation used before this feature cannot be recovered via V48 receipts. HTTP result is receipt metadata, not the full creation response. This design does not atomically commit selected groups, imports and activations as a single wizard transaction. Do not enable a new client workflow without matching Web/Android and QA approval.

## Testing / gates
V48 is additive. New tests on disposable PostgreSQL validate identical-key replay, tenant isolation, kind/template conflict, domain+receipt rollback and same-key concurrency. Standalone test checks invalid arguments and missing receipt. Existing tests asserting migration counts were updated for V48. Exact-head green CI and runtime tests on disposable family are required. No reset execute, seed of existing families, automatic purchases, or ledger mutation authorized.

## User flow
After parent reviews and explicitly confirms task/reward creation, persist a UUID per confirmed action, stable scoped payload and epoch. Retry with the exact same UUID/payload, or GET receipt on uncertain timeout. Do not generate a new UUID just because network response was lost. Clear cached pending actions on family/epoch switch; reject child-only manager access. Setup progress should be marked complete only after successful outcomes, not after form display.

## Coordination
Web/Android: new endpoint returns a receipt, not task/reward DTO. Designer: mapping remains unapproved. QA: test real permissions (including child403), TIME/null price semantics, same-key replay, stale epoch, another family. Release: verify V48 Flyway migration and pinned SHA before deploying, leave BKL002 execute HTTP423.
