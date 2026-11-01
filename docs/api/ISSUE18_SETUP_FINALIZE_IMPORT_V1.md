# Issue #18 — explicit atomic setup finalize-import (V49)

**Scope:** newly created family wizard only. Source committed; exact-head CI/QA and Architect acceptance required before enabling. `familypoints.setup.enabled` defaults to **false**.

## Endpoint
`POST /workspaces/{workspaceId}/setup/finalize-import`

Header `Idempotency-Key: <UUID>` and `X-Workspace-Data-Epoch: <current epoch>`; authenticated `canManageWorkspace`.
Body:
```json
{"expectedRevision":7,"selectedCatalogCodes":["<real-catalog-task-code>"]}
```
Result:
```json
{"operationId":"<uuid>","replayed":false,"workspaceId":123,"dataEpoch":0,"revision":8,
 "setupStatus":"COMPLETED","importResult":{"selectedCodes":["..."],"alreadyImported":[],"toImport":["..."]}}
```
`GET /workspaces/{workspaceId}/setup/finalize-import/receipts/{operationId}` recovers a durable family/epoch-scoped result, no-store and read-only.

## Transaction and security
- Under the same family-row epoch lock, validate feature-enabled setup row, expected revision and all seven steps, import **private** catalog templates through V47 idempotent import, then mark `setupState` completed, then persist an immutable V49 finalize result. All writes are in a single transaction; errors rollback import and setup closure and receipts.
- Same UUID and byte-stable serialized request replay returns the original result, no second write. Different payload + same UUID =>409. Cross-family receipts are invisible. Stale epoch or missing legacy-family setup state cannot commit.
- Never auto-create a GROUP, TASK, REWARD, reward purchase or points transaction. The external user already edits/activates individually with explicit approval; this operation is **not an atomic commit across those previous actions**. A skipped step is a deliberate user choice and cannot be interpreted as an executed activation.
- `POST /setup/review` remains a read-only preview, *not* a commit capability. Server revalidates revision and selected codes at commit time.
- V49 adds only `workspace_setup_finalize_receipts`; no data backfill, no data deletion, no reset changes.

## Tests and known limits
- Disposable PostgreSQL regression: replay, fingerprint mismatch, rollback with injected domain mutation, incomplete-step guard and concurrent same-key serialization. All tests and Flyway migration assertions require exact-head green CI.
- Existing /setup/complete remains for metadata-only closure and is not redefined. For Web/Android, prefer an explicit confirmation flow displaying what *private* imports do. Do not automatically create/activate tasks, or imply finalized setup imported active tasks.
- Review and finalize requests are separate HTTP calls; if another client changes state, revision 409 is expected.
- BKL-002 data-reset execute remains HTTP423; there is no post-reset wizard activation.
