# BKL-002 — A/B/C/D parallel backend pickup and interface control

**Inspection date:** 2026-11-18 (Europe/Berlin). **Status:** coordination evidence only; no automatic inter-chat task execution. **User decision:** preserve current backend as **B (Restore Engine)**. Source of truth for assignment: [dispatch](requests/BKL002_BACKEND_ABCD_WORKSTREAM_DISPATCH_20261118.md), commit `fa15925465ad`.

| Stream | Pickup evidence in GitHub | Current actionable milestone | Unresolved interface |
|---|---|---|---|
| **A — Backup/Archive** | `handoffs/BACKEND_A_LATEST.md` created; B-A-01 source audit, 23-category V50 and missing member provenance. No A journal observed at this inspection | **B-A-02:** privacy-minimal signed original member/role ancestry vNext, category closure, schema compatibility design, detailed source FK columns | A version proposal requires B consumer and Architecture approval; cannot append `FAMILY_MEMBERS` naively |
| **B — Restore Engine** | `handoffs/BACKEND_B_LATEST.md`, `logs/backend-b.md`; B-B-01 exact 23-category source→target FK matrix `requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md` commit `99f5f812a9` | **B-B-02:** fail-closed target orchestration/PG18 rehearsal and explicit mapping interfaces; preserve previous #61–64 rollback suite | Needs signed roster A; owner proof from C; finance closure D. No independent production restore |
| **C — Owner Security & Reset** | `handoffs/BACKEND_C_LATEST.md`, `logs/backend-c.md`; scope and security contract drafted `965d689` | **B-C-01→02:** inspect current owner/auth/preview implementation, propose owner-binding + Google-only reauth and durable RESET permit boundary. Preserve HTTP423 | Important: C owns **authorization/one-use grants** for Reset/Restore shared security; B owns Restore consumer and atomic permit *consumption* within restore transaction; separate RESET and RESTORE action domains. Architecture arbitrates shared migration/API |
| **D — Financial Integrity** | No `BACKEND_D_LATEST.md` or `logs/backend-d.md` found at inspection; not assumed running | **B-D-01:** independently read production TaskService/RewardService/PointService/PointExchangeService/ledger and test-only signed graph verifier, build coverage matrix for TASK/REWARD_PURCHASE/REFUND/POINT_EXCHANGE/reputation/group and immutable source-target reconciliation; explicit source fields requested from A | D does not own replay SQL, archive format or owner credentials. Do not assign unavailable D acknowledgment |
| **Architecture/Integrator** | `logs/architecture.md` does not yet show ACK for ABCD dispatch or consumer-producer contract | ACK/resolve archive signed identity, owner-original policy, immutable member ID, deleted member semantics, source/target category closure and module ownership. Add IMP before incompatible schema/API/crypto changes | Users' chat direction is authoritative for stream routing; Architecture contract acceptance still separate |
| **QA** | Existing issue #15 and QA handoff; large historic synthetic queue | Separate suite receipts from A/B/C/D, pin exact candidate SHA, FAIL/ERROR/SKIP, require real disposable tests only after explicit approval | Source-only review does not equal PASS |
| **Release** | Actual four-non-skipped green #44264218493 SHA `ef222689d8e928ee008dbcbd04dce0c51f2e86ad`: Backend **506/0/0/0**, 98 Release safety checks; ZIP provenance source matched, recorded in release log. | Verify new post-split SHA and preserve HOLD matrix | Historical green not proof of latest HEAD; no release approval |

## Shared interface decisions requiring explicit ACK
1. **SignedArchiveContract (A→B,D):** minimal original member family ownership plus historical/disabled people, source ID/role/type/status, private-field redaction, signed category closure, source epoch/snapshot/digest, archive vNext and old v1 fail-closed.
2. **ApprovalContract (C→B):** actual immutable owner account, fresh password/Google proof, action RESET vs RESTORE, one-use operation ID, expiry ≤5 min, exact snapshot+map/plan hash+target epoch and transaction-coupled persistence. **B may invoke C service but not mint or approve its own owner credentials**.
3. **FinancialClosure (D→A/B/C):** all source-type parents, amount and point_type semantics, no silent refunds, signed original and target semantic row counts/invariants, group+reputation; reset category policy and selective FK restrictions.
4. **TargetApplyContract (B→QA/Web/Android):** only after contracts 1–3 approved; preview/eligible scope, conflict/replacement plan, protective verified snapshot, idempotent receipt and new epoch. Never expose a destructive endpoint before acceptance.

## Non-overlap operating rule
- **A:** exporter/portable serialization, artifact/backup storage; **B:** import replay and target reconciliation plumbing; **C:** owner and authority, Reset executor; **D:** pure financial accounting invariants/validators.
- Do not edit another stream's classes or shared DTO/Flyway without a named owner ACK. Add a tracked IMP to `IMPACT_REGISTER.md` before any incompatible contract; consumer review before code.
- Every new stream maintains `docs/coordination/handoffs/BACKEND_<letter>_LATEST.md`, `docs/coordination/logs/backend-<letter>.md` and exact SHA/real test results. The historical `BACKEND_LATEST.md` is continuity, not the new combined owner.
- QA accepts independent isolated PostgreSQL tests asynchronously. Release only approves tag/deploy after exact-SHA and full operator gate. All owner/dev data unaffected. Reset Execute HTTP423, importer `restorable=false`, `replaySupported=false`, **BETA HOLD**.

## Next checkpoint triggers
- D journal + handoff materialize after first pickup; A adds its distinct journal after first implementation batch.
- Architecture acknowledges contract split and publishes explicit IMP/decision.
- B emits consumer integration implementation with exact SHA and test run evidence.
- CI on newest merged SHA actually executes all four component jobs; inspect jobs/steps and Maven logs, not green overall status alone.

**Issue:** https://github.com/o-kobylianskyi/family-points/issues/15
