# V48 task-template activation — authenticated recovery and diagnostics (PR56)

**Scope:** SQL-backed LocalDate fingerprint fix and disposable PG service transaction tests. Current PR56 is a Draft/unmerged candidate; this is not a deployed fix, not a production reset instruction and does not authorize replay of the owner's unresolved original UUID.

## Actual server call path

`POST /workspaces/{workspaceId}/templates/{templateId}/activate-task/idempotent`
- `Authorization: Bearer <scoped valid JWT>`
- `Idempotency-Key: <original stable UUID>`
- `X-Workspace-Data-Epoch: <actual workspace epoch>` (required after the first reset)
- `Content-Type: application/json`, body = complete original `TaskDefinitionCreateRequest` including `startDate` and optional `endDate` as `YYYY-MM-DD`, `dueTime`/slot times as time values, actors and currencies set explicitly.

The endpoint is owned by `TemplateActivationReceiptController`; class-level `@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")`, task route additionally `@PreAuthorize("hasAuthority('CREATE_TASKS')")`. It delegates to `TemplateActivationReceiptService.activateTask`, which validates epoch, scopes persisted `template_activation_receipts` by workspace+UUID, hashes the canonical task payload with explicit Jackson2 `JavaTimeModule`, and on new intent invokes `WorkspaceTemplateController.activateTask` -> `TaskController.createDefinition` -> real `TaskService.createDefinition` domain validation and JPA writes. V40 activation claim, V48 receipt and definition must share a Spring transaction; failure rolls back all.

Read-only recovery: `GET /workspaces/{workspaceId}/templates/activations/receipts/{originalOperationId}` with authenticated same manager workspace role and `Cache-Control: no-store`. It never creates a task, and 404 is **not** proof no definition was created on an unpatched server or failed intermediate state.

## Distinguish errors without accessing private tokens

| Observed case | Server source boundary | Safe interpretation |
| --- | --- | --- |
| HTTP 401 | `SecurityConfig.anyRequest().authenticated()`; rejected/missing Bearer or workspace-scoped JWT failing validation/session can leave request anonymous | Authentication problem; **not** evidence of LocalDate failure or no task |
| HTTP 403 | Workspace isolation or `canManageWorkspace`/`CREATE_TASKS` method authorization | Workspace/role boundary; do not switch account or retry using new UUID |
| HTTP 409 | Stale `X-Workspace-Data-Epoch`, mismatched receipt fingerprint or already submitted V40 legacy claim | Preserve original UUID/body; do not blind POST |
| HTTP 404 on receipt GET | No receipt visible to this workspace under current auth | Keep UNKNOWN until independently reconciled, do not infer no task |
| HTTP 500 containing `Cannot fingerprint activation request` | Old Jackson2 mapper with missing Java time module or other serialization error | PR56 adds explicit `JavaTimeModule`; the fix only applies if its exact candidate is actually running |
| Network loss/timeout | Unknown server commit state | Keep UUID and original immutable request, use read-only receipt reconciliation |

**Important distinction:** `JwtAuthenticationFilter` logs rejection causes server-side (HTTP method/route, exception class/message), while a full local failure after JWT validation can occur in domain/controller; do not print Authorization, Cookie, JWT, password, email, or family payload in diagnostics. `WorkspaceDataEpochFilter` rejects stale/missing epoch on authenticated writes, not reads.

## Safe QA sequence (disposable synthetic workspace ONLY, with separate owner approval for browser)

1. Record exact local API artifact SHA/image digest and route base origin, never credentials. Verify the backend artifact includes PR56's Jackson2 JavaTime module fix; source branch CI alone cannot verify the owner's localhost is updated.
2. With a **new synthetic operation UUID, never the owner's pending UUID**, verify HTTP 401 for no token, HTTP 403 for an authenticated workspace member lacking `CREATE_TASKS`, and that a valid workspace manager can reach validation without being rejected at JWT/filter stage. Distinguish response status and sanitized server log.
3. On an authorized disposable workspace, submit one non-null `LocalDate` + `LocalTime` activation with legitimate template/assignee, verify one TaskService definition and matching V40/V48 claim+receipt, then GET original UUID; same body/UUID returns `replayed=true` without another definition, changed date/template returns 409.
4. Inject a controlled receipt-insert constraint failure in disposable PostgreSQL to verify actual TaskService work node, task definition, activation claim and V48 receipt rollback; restore fixture schema and retry same synthetic UUID once.
5. Lost-response test: make a single synthetic POST, deliberately drop its client response after server commitment, then only authenticated GET same operation UUID to recover; never generate a new key or use legacy non-idempotent route.
6. Verify role/workspace switching fails closed; no production-family mutation or owner's real pending UUID replay. Run browser-backed Web PR55 guard regression separately and independently sign off.

## Current test boundary

PR56 originally proved deterministic JavaTime fingerprint and SQL-backed transaction, not full HTTP/JWT. The expanded candidate adds **real TaskService** in disposable PG via a bridged test controller, V40/V48 receipt replay and domain rollback on receipt-insert error, plus targeted JWT-filter scope tests. The bridged TaskController is not a live servlet authentication test; operator-approved disposable browser and actual deployed artifact comparison remain separate gates.

**Safety:** PR56 Draft/unmerged, BETA HOLD; no tag, main merge or deploy. Reset Execute HTTP423; Restore Execute disabled; physical purge OFF.
