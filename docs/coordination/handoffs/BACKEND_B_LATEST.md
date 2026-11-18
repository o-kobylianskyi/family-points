# FamilyPoints — Backend B handoff / ownership (2026-11-18)

**Current direction:** Backend B — Restore Engine. **Assigned by project owner in chat on 2026-11-18.** Architecture's recent working split (Backend A — Backup & Portable Archive; Backend B — Restore Engine) is accepted as the intended coordination boundary. The canonical GitHub `logs/architecture.md` and `README.md` still refer to a single Backend at the time of this handoff, so final shared ADR/impact-record routing is **pending Architecture acknowledgment**, not silently asserted.

## Scope owned by Backend B
- Restore/import execution engine for **FULL and SELECTIVE** recovery, scoped transaction and dependency order.
- Source→target **member, currency, rate, task, reward, group, media and ledger FK/ID mapping** at the Restore boundary. Must never infer identity from numeric ID/name/role coincidence.
- Restore-time immutable source graph verification, idempotent replay/exactly-once operation, authorization consumption **in the same DB transaction as data writes**, rollback/no partial writes, restore receipts, recovery from failure, tenant isolation.
- Test-only PostgreSQL 18 rehearsals, negative/security tests, restore-scope API contracts **only after Architecture approval**; QA handoff on pinned exact SHA.
- Integration tests of archive **consumer**: Backend A is producer; B may read sealed V50 archive and request missing source fields/signed original membership via Architecture, but must not unilaterally change archive format.

## Scope owned by Backend A (not Backend B)
- Backup capture/exporter, scope enumeration, snapshot source digest, artifact encoding/encryption, portable archive schema/version V50+, integrity and retention of source archive, archive format migration and approved original `FAMILY_MEMBERS` source catalog.
- Backend B reviews/ACKs archive contract and supplies precise restore-consumer requirements. No conflicting writes to `WorkspacePortableBackupExporter`, `ProtectiveBackupArtifactCodec`, `ProtectiveBackupPortableFile`, `ProtectiveBackupImportInspector` or migration schema without named cross-track agreement.
- Backend A should not independently enable Restore Execute or implement the consumer replay transaction.

## Concrete transition from former unified Backend work
- Existing `docs/coordination/handoffs/BACKEND_LATEST.md` and `docs/coordination/logs/backend.md` remain immutable historical predecessors; append-only only.
- Earlier Backend BKL-002 batches #25–64 span financial source preflight and target restoration. Their **restore-engine, explicit ID-map, rollback/authorization, PostgreSQL rehearsal** tests belong to Backend B.
- Source archive creation and original-source-category schema decisions belong to Backend A. Existing TEST-ONLY `DisposableLedgerSourceGraphVerifier` is **shared contract-sensitive**: Backend B maintains consumer-side verification/tests; any additional original source fields or category changes require Backend A + Architecture review.
- Begin Backend B's separately labeled work with **B-B-01** rather than ambiguous unified package #65. Separate journal entries, tests, QA requests, and status.

## Observed safety state / CI
- On Nov 18, previous exact-SHA runs #44196224270 (493 Maven/2 failures) and #44262590272/#44262614624 (496 Maven/1 failure each) documented finance exception expectations. Later repair/source commits exist, but **current exact-SHA all-four PASS NOT VERIFIED** in this handoff; verify Actions jobs/logs, no PR-only lookup shortcuts.
- Current test-only cross-category work #61–64: `DisposableDurableRestoreApprovalStore` QA-only SQL, `DisposableMemberMappingPreflight`, `DisposableMemberBindingReceiptVerifier`, source verifier and 2-PG18 restore rehearsal. None establishes real owner reauthentication, production durable grants or portable identity after source DB loss.
- **Production RESTORE:** `restorable=false`, `replaySupported=false`; Reset Execute HTTP423; BETA HOLD. Never reset actual families without fresh explicit scoped authorization.

## Critical P0 dependencies on Backend A / Architecture
1. Authenticated, privacy-reviewed portable **original member catalog / historical memberships** with schema/version and immutable ID ancestry. Current V50 lacks `FAMILY_MEMBERS`.
2. Full category closure and clear semantics for `REFUND`, points vs reputation ledger, purchases, media, family-wide vs selected restoration.
3. Valid owner/guardian authentication and explicit source→target identity pairing approval; cross-family/shared-account/soft-deleted member rules.
4. Durable approved Restore receipt/operation and release gate with exact SHA all four jobs and QA. **No speculative production unlock.**

## Backend B next work
- **B-B-01:** independent consumer-side archive/target dependency matrix and contract-gap request to Backend A + Architecture: original members, account/role privacy, all FK categories and historical ledger ancestry.
- **B-B-02:** synthetic two-database restoration crash/rollback proof with exact FK/ID mapping; no live DB.
- **B-B-03:** design and implement production restore only after architecture + Backend A artifact contract approved, with explicit owner authentication and migration/release review.
- Hand all real user/device E2E to QA, Release does deployment; Backend B owns repeatable synthetic integration tests.

## Coordination endpoints
- Backend B: `docs/coordination/handoffs/BACKEND_B_LATEST.md`, `docs/coordination/logs/backend-b.md`
- Legacy read-only intake: `docs/coordination/handoffs/BACKEND_LATEST.md`, `docs/coordination/logs/backend.md`
- Architecture: `docs/coordination/logs/architecture.md`, `docs/coordination/IMPACT_REGISTER.md`
- QA: GitHub Issue #15 / exact-SHA and Postgres18 evidence.
