# Issue #18 — read-only setup review v1 (Backend)

Status: committed source; independently verify pinned CI before consuming. This is **not** the final transactional setup/commit API and does not authorize any automatic activation or reset.

## Endpoint
`POST /workspaces/{workspaceId}/setup/review`

Auth: `@workspaceSecurity.canManageWorkspace(workspaceId)`. The setup feature must be enabled (`familypoints.setup.enabled=true`; disabled by default). This call is read-only; no `X-Workspace-Data-Epoch` write header is consumed. An older revision results in 409.

Request:
```json
{"expectedRevision":3,"selectedCatalogCodes":["task.actual-code","reward.actual-code"]}
```
Use actual catalog codes, not the example placeholders.

Response:
```json
{
  "workspaceId":123,
  "dataEpoch":0,
  "revision":3,
  "setupStatus":"IN_PROGRESS",
  "selectedCount":2,
  "alreadyImportedCount":1,
  "toImportCount":1,
  "selectedCodes":["task.actual-code","reward.actual-code"],
  "alreadyImported":["task.actual-code"],
  "toImport":["reward.actual-code"],
  "requiresExplicitConfirmation":true,
  "createsActiveDefinitions":false
}
```

Reuses the same catalog code validation and workspace-private existence query as `POST /workspaces/{id}/templates/imports/preview`. Unknown, duplicate, blank, null or excessive catalog codes fail. Empty selection is valid and has zero counts. No catalog import, new group, active task/reward, purchases or ledger entries occur on this endpoint.

The review result **is not a durable snapshot, digest, reservation or authorization to commit later**. Concurrent imports can change `alreadyImported` between preview and explicit import. Clients must re-check the latest epoch/revision and use the existing idempotent import/activation operations, their operation UUID and matching receipts. The proposed atomic cross-domain `setup/commit` is not implemented; do not claim atomicity across separate group/import/activation requests. The only fully scoped status row is the feature-gated `setupState` created for newly provisioned families. Historical families remain unenrolled.

## QA cases
- Authorized managing adult on a disposable freshly created workspace, setup enabled: empty selection, mixed TASK+REWARD, entire 142 catalogue, previously imported codes.
- Same workspace stale revision or COMPLETED => 409, no preview call. Wrong family =>403. Old family missing setup row =>404. Disabled feature =>503.
- Unknown/duplicate codes =>400; no domain writes or ledger entries in every case.
- Two-tab refresh / epoch change / another family: review never authorizes a later write, which must independently enforce original epoch.
- Database change between review and import: review counts can be stale; idempotent import retries and receipts are authoritative for actual outcome.

Web and Android: use this as display-only review before explicit confirmation, and do not fabricate groups or infer null reward pricing. Parent approval remains separate. Designer must approve typed task folder mapping. Release controls actual enablement. BKL-002 execute stays HTTP423.
