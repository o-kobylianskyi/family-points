# Issue #18 — WorkNode GROUP Backend implementation (2026-11-01)

Source package: e4ccf357 + df29620b + 72bd5031; verify exact latest CI before consuming in Web/Android. **No reset execute or deployment authorization.**

Distinct entity types: `MEMBER_GROUP` manages people and roles; `WORK_NODE_GROUP` is a task hierarchy node. Catalog TASK category labels are not automatically mapped into WorkNode groups. Designer/Architecture must explicitly approve any proposed mapping.

## Implemented API

Base: `/workspaces/{workspaceId}/work-node-groups`. JWT and workspace identity required.

| Method | Path | Permission | Effect |
|---|---|---|---|
| GET | base | canAccessWorkspace | List active task-folder groups in parent/sort order |
| POST | base | canManageWorkspace AND CREATE_TASKS | Create group under optional existing active same-family GROUP |
| PUT | base/{groupId} | canManageWorkspace AND CREATE_TASKS | Rename, move, reorder group |
| DELETE | base/{groupId} | canManageWorkspace AND CREATE_TASKS | Soft-deactivate **empty** group only (204) |

POST/PUT body:
```json
{"title":"Chores","description":"Optional","parentGroupId":null,"sortOrder":0}
```
Response:
```json
{"id":42,"parentGroupId":null,"title":"Chores","description":"Optional","sortOrder":0,"active":true}
```
`X-Workspace-Data-Epoch` must equal the original client generation on writes. Same-family writes take the family row transaction lock. At current epoch0 the shared guard still supports the legacy missing-header transitional behavior; clients **must always send the header**. GET has no write side effects.

Errors: 400 invalid title/description, 403 insufficient permissions, 404 missing/foreign/deactivated parent or group, 409 duplicate case-insensitive sibling title, cyclic ancestor move or non-empty folder. Global exception envelope localization still requires consumer review; don't parse message string as guaranteed stable JSON error code.

## Constraints and deliberately unimplemented scope
- No FK schema changes or automatic default group creation.
- A folder can be deactivated only when it has no active direct children (either GROUP or TASK). Empty-group deactivation doesn't touch tasks/transactions.
- No automatic import, catalog mapping, bulk create or activation in these endpoints. No assumptions that every existing task is already linked to WorkNode GROUP.
- Name-based duplicate prevention is serialized across cooperating family-fenced API writes. A future bulk importer should use durable operation receipts/uniqueness; this API **does not** provide idempotent outcome replay.
- New package includes disposable PostgreSQL integration tests for nesting, duplicate names, cross-family parents, ancestry cycle, non-empty soft-delete, invalid title, and rollback. Exact-head CI/consumer acceptance required.
- BKL-002 `/data-reset/execute` remains disabled with HTTP423.

## Consumer handoff
Web: add a separate task-folder selector (do not reuse member-group DTO). Android: same distinction, no offline auto-creation. Designer: approve stable localized suggested group mapping before seeding. QA: parent/child role boundaries, family A/B, duplicate/cycle, non-empty folder, rollback/concurrency, task references and actual DEV SHA. Release: V46+ target environment and explicit acceptance gate.
