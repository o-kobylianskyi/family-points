# BKL-002 — Backend A/B/C/D workstream dispatch and integration boundaries

**Date:** 2026-11-18. **Authority:** user's explicit assignment: existing Backend is **Backend B (Restore Engine)**; connect separate Backend A, C and D. **Status:** proposed coordination contract; Architecture ACK REQUIRED for shared API/DB/schema and owner-policy decisions. This dispatch does not assign a runtime job to a disconnected chat or authorize release.

## Immediate project state
- Existing Backend A pickup: `docs/coordination/handoffs/BACKEND_A_LATEST.md`, B-A-01 reviewed exporter/inspector and missing authenticated original member ancestry.
- Existing Backend B pickup: `docs/coordination/handoffs/BACKEND_B_LATEST.md`, and exact producer request `docs/coordination/requests/BACKEND_B_RESTORE_CONTRACT_REQUEST_20261118.md`.
- Existing unified Backend historical #61–64 multi-category synthetic PostgreSQL atomicity belongs to **B**. Do not rename historical commits or restart test work.
- Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD. QA independently accepts tests; Release owns actual deployment. Never mutate real family data.
- A and B own separate journals/handoffs; C and D must initialize theirs at pickup. Shared contract changes require an IMP record and Architecture decision. Historical unified backend log remains historical.

## Workstream assignments and source ownership

| Track | Accountability | First large work package | Exclusions / handoff |
|---|---|---|---|
| **Backend A — Backup & Portable Archive** | Source capture, encrypted/versioned `.fpbackup`, authenticated original-family member/role ancestry (after privacy approval), source category/FK closure, snapshot consistency, media inclusion, bounded file sizes, retention 30 days and sealed source digest | **B-A-02** audit full original member roster/role/deleted history & all member reference fields; propose privacy-minimal vNext archive and legacy-v1 non-restorable compatibility; send written field/schema/digest proposal to B/C/D/Architecture before code | Owns `WorkspacePortableBackupExporter`, `ProtectiveBackupArtifactCodec`, portable manifest/version and source serialization. **Not** Restore apply, owner reauth or target ledger reconciliation |
| **Backend B — Restore Engine (CURRENT Backend)** | Consumer import, FULL/SELECTIVE dependency-closed replay, target FK/ID maps, transaction/rollback, cross-tenant isolation, protected target snapshot integration and immutable outcome receipt; synthetic two-PG18 tests | **B-B-01→02** finish consumer dependency matrix and B's existing A archive request; retain #61–64 atomic multi-category PG harness, extend safe consumer orchestrator with explicit dependency interfaces while A/C contracts await sign-off | Owns target replay/orchestration, **not** source archive schema, global owner credential proof or independent financial policy. No production execute unlock |
| **Backend C — Owner Security & Reset** | Durable owner binding/enrollment/transfer, fresh local-password / Google reauth, 5-min one-use permit, typed family-name confirmation, operation/epoch/nonce reservation & audit, dependency-safe FULL/SELECTIVE Reset and transactional owner guard | **B-C-01** inspect current `WorkspaceEpochTransactionGuard`, reset preview/challenge, ownership schema and synthetic `DisposableRestoreApprovalGate/Store`; produce production owner + durable approval + Google-only fallback contract, transaction boundaries and migration/rollback plan. **No destructive enable** | Owns authorization and reset endpoint, not producer archive format nor consumer restore/financial math; B consumes approved permit contract |
| **Backend D — Financial Integrity** | Financial category-source graph and immutable economics: POINT_TYPES, POINT_LEDGER, TASK EARN/PENALTY, REWARD_PURCHASE, REFUND, POINT_EXCHANGE, reputation/group ledger, obligations, signed source-to-target semantic reconciliation | **B-D-01** audit actual producers/schemas and existing #24–44 tests, enumerate all supported/unsupported source types and history closure. Define pure preflight/reconcile contract, counts, SHA semantic digest and required negative/failure tests; issue B/A signed archive field requests | Owns financial invariants & read-only graph verification, **not** target SQL restore orchestration, source encryption or owner permits. B consumes D's validation APIs |

## Contract interfaces (Architecture must approve; proposals, not implemented claims)
1. **A → B/D: SignedArchiveContract vNext**: stable snapshot UUID, tenant/epoch, version, selected category digest, original family member ancestry with role/type/status/historical ID references, POINT_TYPES and all required FK parent/child records, schema/size limits; preserve legacy v1 fail-closed. Privacy, deleted users and cross-family membership policy require Architecture decision. Must not expose credentials or secret auth fields.
2. **C → B: RestoreAuthorizationContract**: authenticated owner ID (not just ADMIN), fresh reauth proof (Google-only included), exact workspace/snapshot/plan/mapping digest, typed family name, operation UUID and epoch, expiry <=5m, persistent one-use state, same-transaction consumed permit + target write, durable audit/receipt. No synthetic HMAC/actor as real production authority.
3. **D → A/B: FinancialClosureContract**: immutable source-type/parent ancestry by member/currency, signed amount & rate history, source/target FK mapping requirements, per-row SHA-256 semantic equality and exact cardinality, ledger balances, reward obligations, all group/reputation history, unsupported REFUND/unknown types fail closed. A exports needed inputs; B applies; D verifies.
4. **B → Web/Android/QA: RestorePlan/Receipt vNext** after Architecture sign-off: dry-run selection/dependency/conflicts/retained data, protected target snapshot, stable operation receipt and new dataEpoch, idempotent lost-response GET. No unsafe client CTA until actual Backend/QA gate.
5. All shared changes: add **IMP-XXXX** in `docs/coordination/IMPACT_REGISTER.md` before incompatible wire/DB behavior; record required A/B/C/D/Web/Android ACK, tests and Release migration gate. Do not let multiple owners edit the same shared file concurrently.

## Integration phases / nonblocking parallel work
- **Phase 0 (now):** A/B already active; C and D independently inspect actual source and create `docs/coordination/logs/backend-c.md`, `backend-d.md` and `docs/coordination/handoffs/BACKEND_C_LATEST.md`, `BACKEND_D_LATEST.md`. Never assume chats receive notices automatically.
- **Phase 1 (parallel, no production enable):** A prepares signed source ancestry proposal; B builds target orchestration behind fail-closed adapter; C designs real owner proof/migrations and one-use integration; D defines audited finance source graph. Architecture resolves only shared disputed contract details.
- **Phase 2:** A archive contract + D financial contract reviewed by B; C authorization reviewed by B and Architecture. Contract lock, tests and compatible migrations before putting them into a production service.
- **Phase 3:** B integrates signed A input + D validators + C permit with protected backup, one transaction and audit; preserves source and unrelated tenants; QA runs independent pinned Testcontainers and Web/Android E2E on approved disposable family only.
- **Phase 4:** Release pins exact final SHA, all four non-skipped CI, backup/restore rehearsal and signer verification; Architecture/QA approve; user authorizes any deployment separately. BETA HOLD before all gates.

## Anti-overlap and verification rules
- No component unilaterally changes `ProtectiveBackupImportInspector` or shared service DTOs without notifying its producer/consumer; A owns producer format, B owns consumer use, D owns finance proof, C owns approval. Architecture resolves coupled edits.
- Each batch: role-prefixed work ID B-A-XX/B-B-XX/B-C-XX/B-D-XX, source paths, SHA, tests actually executed (PASS/FAIL/SKIP/NOT_RUN), open dependencies, next large package; append role journal + rolling handoff. QA owns independent test bundles/retakes, not routine operator-driven micro-tests.
- The current Backend B continues its already-started rollback/one-use rehearsal; do not reassign to A or force restart.
- Separate cross-role exact-SHA full CI checks from scoped docs-only green. No claiming test-only proofs as production VERIFIED or treating CI green as BETA approval.

## Initial request / ACK checklist
- [ ] Architecture acknowledges workstream split, stable member identity/owner policy, A↔B and C/D contracts, file/module ownership and any required IMP(s).
- [x] Backend A began scoped source audit (B-A-01); final contract ACK outstanding.
- [x] Backend B accepted Restore ownership, issued consumer archive request; final contract ACK outstanding.
- [ ] Backend C picks up security/reset assignment; creates separate journal/handoff and publishes B-C-01 contract.
- [ ] Backend D picks up finance assignment; creates separate journal/handoff and publishes B-D-01 closure matrix.
- [ ] QA records four parallel queues and pins exact SHA, all failures, errors and skips.
- [ ] Release keeps HOLD and never publishes/destructively tests without explicit owner authorization.

**Canonical issue:** https://github.com/o-kobylianskyi/family-points/issues/15
