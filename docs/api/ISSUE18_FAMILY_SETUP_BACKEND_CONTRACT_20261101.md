# Issue #18 — Family setup Backend contract and implementation gate (2026-11-01)

**Status: VERIFIED CURRENT API + PROPOSED CONTRACT; not an implemented resumable wizard.**
Owner: Backend. Target: first-family setup without destructive reset. This document is an architecture/consumer review artifact, not deployment approval.
Baseline inspected: `main` at 96532dd2781aace55a0bb5ddd9169ad6eed158f9; previously green onboarding repair eaeff27c (Actions 41250888001). Before changing code, rebase against latest main.

## 1. Invariants

1. New-family onboarding is separate from BKL-002. Neither browsing nor setup should invoke reset or mutate an existing family silently. Reset execute stays HTTP 423, no production/QA family wipe.
2. Every domain write is workspace-scoped, role-checked, protected by **the original client** `X-Workspace-Data-Epoch`, and fenced in the same outer transaction before domain mutations. Do not infer permission from account-level family visibility.
3. Browsing, previews, category filtering, recommendations, and initial wizard rendering are read-only. Catalog import creates **private templates only**; activation requires explicit parent confirmation. No purchases, ledger entries, currency zeroing or side-effectful price recommendations.
4. `MEMBER_GROUP` and `WORK_NODE_GROUP` are different entity types. A catalog category is neither a physical group nor a stable mapping into either. Mapping needs an approved typed contract.
5. `null` reward price is **unset**, never free/zero. Portable `TIME` maps to activation `TIME_BASED`; `ITEM` to `STANDARD`. Suggestions stay optional, never applied without explicit parent action.
6. Account identity, family identity, other families, global templates and existing private user customizations remain intact. A new onboarding flag must never trigger historical data rewrite.
7. Interrupted, retried or concurrent browser sessions must not duplicate domain effects. An idempotency key **without durable response replay** is insufficient to guarantee safe client retries.

## 2. Existing API (source-verified, not a promise of runtime deployment)

| Area | HTTP route | Auth/behavior | Limits |
|---|---|---|---|
| First family | `POST /api/onboarding/first-family` | authenticated; body `{familyName,ownerName}` | feature flag `familypoints.first-family.enabled=false` by default; creates family/owner setup, not durable wizard |
| Additional family | `POST /api/account/families` | `MANAGE_MEMBERS` or `ADMIN_OVERRIDE`; same body | flag `familypoints.additional-family.enabled=false` by default |
| Family selection | `GET /api/me/workspaces` | account-scoped workspace choices | not a setup progress API |
| Workspace | `GET /workspaces/{id}` | `canAccessWorkspace` | not a wizard lifecycle |
| Member groups | `GET/POST /workspaces/{id}/member-groups` | read scoped; create `canManageMembers || canManageRoles` | member group, **not** task folder |
| Global catalog | `GET /template-catalog` | catalog read | bundled entries; no implicit import |
| Catalog economy | `GET /template-catalog/economy-suggestions` | read only | not pricing authorization |
| Private list | `GET /workspaces/{id}/templates` | `canManageWorkspace` | scoped private templates |
| Selection preview | `POST /workspaces/{id}/templates/imports/preview` | `canManageWorkspace` | `{selectedCodes:[...]}`; `{selectedCodes,alreadyImported,toImport}`; no write |
| Selected import | `POST /workspaces/{id}/templates/imports` | `canManageWorkspace`, current epoch | import limited to 100 distinct codes per request; duplicate codes handled in scoped catalog import |
| Private draft | `GET /workspaces/{id}/templates/{templateId}/creation-draft` | `canManageWorkspace` | editable task/reward request preview |
| Optional recommendation | `POST /workspaces/{id}/templates/{templateId}/creation-draft/apply-economy-suggestion` | `canManageWorkspace` | response draft, no domain write |
| TASK activation | `POST /workspaces/{id}/templates/{templateId}/activate-task` | `canManageWorkspace` + `CREATE_TASKS`; `Idempotency-Key: UUID` | creates active definition after explicit request; duplicate key currently 409, **no saved result receipt** |
| REWARD activation | `POST /workspaces/{id}/templates/{templateId}/activate-reward` | `canManageWorkspace` + `MANAGE_REWARDS`; `Idempotency-Key: UUID` | same duplicate-key caveat |
| Data epoch | `GET /workspaces/{id}/data-epoch` | scoped | required for stale-write fencing |

The 2026-11-01 Backend fix allows `Idempotency-Key` in CORS and maps portable reward enums correctly in creation drafts. No end-to-end wizard state, outcome receipt or WorkNode GROUP CRUD API is demonstrated by these changes.

### Catalog UX source of truth
142 bundled catalog templates: 110 TASK and 32 REWARD, 6 packs; 99 pack memberships and 43 entries outside packs (from Issue #18). The API import limit of 100 means the client cannot naively submit all 142 codes in one request. Pack overlap must be resolved to distinct catalog codes before preview and commit. Validate these counts against the currently bundled file in QA.

## 3. Proposed durable setup contract — **NOT YET IMPLEMENTED**

A single workspace-scoped aggregate state, distinct from reset operation state:

`GET /workspaces/{workspaceId}/setup` -> 200
```json
{
  "workspaceId": 123,
  "dataEpoch": 1,
  "entryKind": "NEW_FAMILY",
  "state": "IN_PROGRESS",
  "revision": 4,
  "steps": {
    "MEMBERS": "COMPLETE",
    "MEMBER_GROUPS": "SKIPPED",
    "WORK_NODE_GROUPS": "PENDING",
    "CATALOG_SELECTION": "COMPLETE",
    "TASK_REVIEW": "PENDING",
    "REWARD_REVIEW": "PENDING",
    "FINAL_REVIEW": "PENDING"
  },
  "lastCompletedAt": null
}
```
Suggested `state`: `NOT_STARTED|IN_PROGRESS|COMPLETED`; per-step: `PENDING|COMPLETE|SKIPPED`. `entryKind`: `NEW_FAMILY` now, `POST_RESET` reserved and MUST fail closed until approved BKL-002 execute. `dataEpoch` is the current scoped epoch, not a client-provided substitute for an already received original epoch.

`PATCH /workspaces/{workspaceId}/setup/steps/{stepCode}` with `If-Match: "<revision>"`, `X-Workspace-Data-Epoch` and body `{"status":"SKIPPED"}` or `{"status":"COMPLETE"}`; response is the new aggregate. Step-state writes must be transactional and optimistic-concurrency protected. Only managing adults may transition state; non-managers can read only an explicitly authorized read-only summary (pending permission decision). Do not mark COMPLETE merely because a page was visited.

`POST /workspaces/{workspaceId}/setup/review` with staged selection and group drafts -> **no side effects**:
```json
{
  "memberGroupDrafts": [{"clientKey":"parents","name":"Parents"}],
  "workNodeGroupDrafts": [{"clientKey":"chores","name":"Chores"}],
  "selectedCatalogCodes": ["TASK_...","REWARD_..."],
  "activationChoices": [],
  "expectedRevision": 4
}
```
Response: `{snapshotDigest,revision,alreadyPresent,toImport,proposedMemberGroups,proposedWorkNodeGroups,taskDrafts,rewardDrafts,blockers,unresolvedCurrencies,estimatedOperations}`. Require stable actual catalog codes in requests, not placeholders. Reject ambiguous typed group mapping and stale revision/epoch. Proposed operation is a dry-run; do not precreate rows.

`POST /workspaces/{workspaceId}/setup/commit` **proposed for coordinated design**, body `{operationId:UUID,expectedRevision,snapshotDigest,confirmedSelections,reviewedDraftOverrides}`. Exact DTO and transaction granularity must be agreed before implementation. The server must atomically record a durable operation outcome, request fingerprint, selected identity keys and actual created IDs; identical retry returns the original result; conflicting payload with same operationId returns 409. Operation status should support `SUCCEEDED|FAILED_RETRYABLE|REJECTED` and explicit immutable receipts; no silent partial-success masquerading as atomic. For implementation simplicity, alternatively compose already implemented import and activation APIs **only after** adding per-operation receipts/recovery and documenting a resumable multi-step saga. Never claim current 409 duplicate activation is equivalent to safe outcome replay.

`POST /workspaces/{workspaceId}/setup/complete` with `{expectedRevision}` requires final review, no unresolved blockers, and stores completed state; does not create anything by itself.

**Architecture decision needed:** durable state table/columns vs existing authoritative data, exact minimum mandatory steps, owner/manager authority, group mapping, single atomic commit vs resumable per-operation receipts. No Flyway migration until these decisions are reviewed.

## 4. Recovery and concurrency behavior

- **Refresh:** read server state; show actual already-imported private templates and active definitions; do not replay write requests just because a wizard reloaded.
- **Network timeout after import/activation:** reconcile receipt or existing scoped catalog/activation mapping first, then show user a recoverable state; never issue a new random activation key automatically.
- **Two tabs:** compare revision and epoch; second tab gets deterministic 409 and must reload. Family lock/unique scoped keys enforce no duplicate writes.
- **Catalog selection >100:** UI batches distinct codes or Backend introduces a reviewed batch endpoint; each batch must carry stable operation identity and recoverable outcome.
- **Multiple workspaces:** always key state and cache by workspace + epoch + account; no copied selection or stale offline activation across family switch.
- **Offline Android:** no implicit enqueue of wizard mutation, catalog activation or reward purchase. Await user confirmation online; stale epoch quarantines pending writes.
- **Auth:** child reward *requests* may use distinct child API but must not import/activate manager-only private templates. No privilege escalation through catalog IDs.
- **Historical workspace:** absence of setup row is not proof of new family; never force old families into setup or overwrite their data. Creation path should explicitly initialize state for newly created families only.
- **Reset:** POST_RESET state only inside future proven successful reset transaction with epoch/audit/onboarding; currently disabled.
- **Errors (proposal):** `SETUP_REVISION_STALE`, `SETUP_EPOCH_STALE`, `SETUP_OPERATION_CONFLICT`, `SETUP_PERMISSION_DENIED`, `SETUP_DEPENDENCY_BLOCKED`, `SETUP_CATEGORY_INVALID`; stable machine-readable codes needed before client localization.

## 5. QA and consumer acceptance

Test on newly created disposable workspaces only: zero selected, one item, mixed TASK/REWARD, six packs with overlap, all142 in safe batches, custom edits, unset/null and explicit-zero price, TIME 30-minute draft, unresolved currency, group types separately, refresh, interrupted request, same/different-key replay, two tabs, stale epoch, child403, cross-family A/B, exact created-IDs/counts, no ledger/purchase, no old-family overwrite, no reset. Execute transaction and rollback/fault-injection PostgreSQL tests before enabling new commit API. Web must reuse catalog category picker in task/reward forms and localize 4 languages; Android follows same DTO. Designer must approve typed suggested-group defaults and labels. Release pins tested SHA and target Flyway/schema separately. **Green CI does not mean deployed.**

## 6. Sequenced large implementation package after architecture review

1. Add minimal additive setup state + immutable operation receipt Flyway, repositories, DTOs, feature flag and migration tests. Existing families = unchanged (no automatic backfill).
2. Add scoped GET/state transitions with revision + epoch lock; idempotent creation initialization for fresh family and safe resume.
3. Add typed member-group and WorkNode GROUP creation/lookup APIs where truly missing, with distinct permissions and stable dedupe keys; require mapping approval before default seeding.
4. Implement full selection dry-run, catalog code dedupe, dependency/price validation and explicit commit or recoverable per-step receipts, maintaining task/reward editing semantics.
5. Add HTTP, permission, concurrent writer, rollback, replay, tenant and null/TIME integration tests; publish exact request/response fixtures and code table; run pinned CI.
6. Hand off versioned contracts to Web/Android/Designer and require independent QA/Release before enablement. Keep BKL-002 execute locked throughout.
