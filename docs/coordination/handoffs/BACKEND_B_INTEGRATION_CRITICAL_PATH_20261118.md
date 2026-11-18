# FamilyPoints — Backend B primary integration critical path

**Effective:** 2026-11-18. **Binding management order:** GitHub Issue #15 comment 6974473668 (product owner / Architecture). **No new feature/synthetic test scope.** Preserve previous B-B-01..08, C-C-08..11 and FIN-01..16 code as historical evidence; **Backend C and D are CLOSED as independent streams**, their unfinished obligations belong to Backend B. Backend A may handle only bounded nonblocking standalone source/archive work.

## Last confirmed independent CI evidence
- **Exact SHA:** `ab77611b866f4757ab579df5778e738e1a2bc685`; [Actions #44275375375](https://github.com/o-kobylianskyi/family-points/actions/runs/44275375375): independently QA-058 verified Java Maven/PG18 **702 tests, 0 failures, 0 errors, 0 skips** with Web, Android, Compose, Testcontainers2 preflight and exact-SHA full attestation PASS.
- Scope of evidence: **ONLY that SHA**. Current newer `main` includes C owner enrollment/FIN-16 currency overflow/A retention and B-B-08 changes which that run does NOT certify. Do not infer green from docs-only scoped runs that SKIP heavy jobs.
- Previous actual #44274406405 Java 684/0F/1E before QA fixture fix, and #44274753614 failed testCompile in B V2 lambda (fixed source subsequently). Need **one new immutable SHA + non-skipped full CI** before another feature push.
- QA-055 Testcontainers2 source preflight in CI; no new generic `org.testcontainers.postgresql.PostgreSQLContainer<?>`.

## C takeover — implemented assets vs genuine gaps
**Reuse (do not rewrite):** `security/WorkspaceRestorePermitScopeV2` and `WorkspaceRestoreSelectionClosureV2` canonical binary typed digest + C fixed golden vectors; `security/OriginalOwnerEnrollmentEvidence` review-only provenance policy; `DisposableOriginalOwnerBindingPostgresTest`, `DisposableOwnerPermitStorePostgresTest`, `WorkspaceResetProofConcurrencyPostgresTest` QA PG18 owner/permit/race cases. C-C-11 README `docs/coordination/requests/BACKEND_C_C11_OWNER_ENROLLMENT_QA_AND_B_V2_ACK_20261118.md` states 30/30 selected C test PASS at earlier red full suite SHA.
**Genuine P0 gaps now owned by B:** authenticated registration writer and verified legacy original-owner enrollment (not admin/oldest/email heuristic), authoritative fresh password/provider reauth, durable action-separated one-time RESET vs RESTORE grant issuance **bound to signed source and owner-reviewed mapping**, same-target-family lock + permit consume + native replay + epoch bump + immutable receipt in ONE transaction. Existing C API contracts/test-only QA table are NOT real issuer/writer. Must go through Architecture single IMP for shared schema/producer/HTTP.
**No new C tasks/commits requested.**

## D takeover — implemented assets vs genuine gaps
**Reuse (do not rewrite):** `DisposableLedgerSourceGraphVerifier` signed V50 source semantics and negatives, `DisposableSignedFinancialPostgresParityTest` sealed V50 archive→explicit mapping→PG18 source/target lineage parity, `DisposableFinancialMappingParityTest` exact source row/source_type/source_id/member/currency signed ledger reconciliation, `DisposableFinancialNativeFamilyGatePostgresTest` Flyway family/epoch and QA-only ledger/claim atomicity, FIN-16 source currency overflow denial regression. Historical FIN-08/09/12/13/16 tests are not a production restore engine.
**Genuine P0 gaps now owned by B:** consume A-authenticated vNext original member/TaskInstance/ledger/event ancestry, reconcile all ledger source events against newly mapped target parents and target member×currency balances; cover actual REFUND/POINT_EXCHANGE/REPUTATION/GROUP ledger where required, including overflow/duplicate/replay. Embed validated finance writes and receipts into **the same** authorized target transaction; no standalone QA-only finance path counted as end-to-end.
**No new D tasks/commits requested.**

## A source archive interface
Backend A is secondary only; existing `B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md` is a **proposal**, not sealed vNext artifact. Source V50 23 categories omit signed original `FAMILY_MEMBERS` and cannot recover historical identity after source DB loss. B must own completion of the blocking producer-consumer contract when A cannot deliver quickly. Signed vNext must independently prove source family/member/role/tombstone/TaskInstance/financial category closure and manifest digests without source PostgreSQL online. Never infer original owner or grant login from archived role.

## B current code / integration scope
- B-B-02..06 PG18 tests cover bounded source→target group/task/reward/point QA-only tables and rollback; B-B-07 plan digests and synthetic transaction; B-B-08 per-row original FAMILY_MEMBERS/ledger preflight and **C canonical binary V2 adapter** + golden vectors. B old text pipe hashes are NOT compatible with C V2. These do **not** constitute source-authenticated full 23-category restore, C durable grant or production native replay.
- Do not add another parallel planning/graph/fingerprint class. Integrate existing A/C/D validated pieces into one audited vertical slice with minimal acceptance tests only.

## Strict ordered queue (Architecture owns order, B executes)
1. **Stabilization:** QA pin latest source SHA, run full non-skipped Maven JDK21/PG18 + Web + Android + Compose + exact attestation. B fixes only **reproducible B-owned errors**; other red failures route directly to actual file under sole B responsibility if finance/security, A if isolated optional producer or QA fixture if appropriate. No speculative hardening.
2. **One source+owner contract:** approve single versioned IMP covering A signed self-contained member/TaskInstance/ledger manifest and category content digests, C canonical binary RestoreScopeV2 + actual owner enrollment/fresh reauth/one-time permit, D finance event closure, B target FK mapping and immutable receipt. No new incompatible DTO variants.
3. **One protected disposable E2E:** source archive .fpbackup → destroy independent disposable source DB → verify archive offline → authenticated target owner and reviewed one-to-one original member map → target family row lock/permit lock/atomic native Restore+ledger/epoch/receipt → post-commit finance parity and denied second replay. Add only missing P0 rollback, cross-tenant and concurrency negative cases. QA/Release later run user-approved disposable DEV flow; never real family data without separate authorization.
4. **QA acceptance / Release:** pin exact successful SHA with executed four components and all relevant Release gates, then V49 authenticated two-tab browser/device real E2E with approved disposable QA data. No production feature gate before independent signoff.

## Single blocker and owner register
| Critical gap | Accountable owner | Evidence needed |
|---|---|---|
| Current HEAD exact all-four CI certification | QA independently; B triages owned failure | run+SHA+jobs+Maven counts, non-skipped |
| Original signed vNext after independent source DB loss | Backend B integrator, A optional delegated producer | authenticated snapshot manifest/roster+category digest and source-loss proof |
| True original owner identity/fresh reauth and durable RESTORE permit | Backend B (former C) | production writer/proof and one-use grant receipt under SQL transaction |
| All financial event provenance/currency signed parity | Backend B (former D) | exact source event graph and mapped target native parent/ledger equality |
| Native Restore all selected FK rows + epoch + immutable receipt | Backend B | single atomic PG18 end-to-end with rollback/replay/tenant rejection |
| BETA/real-device release | QA+Release; owner approves disposable testing | independently verified end-to-end, V49 browser/device and deployment evidence |

**Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD. No owner approval for live family resets, production restore, destructive cleaning or deployment is implied by consolidation.

**Next Backend B task:** first confirm post-QA-058 full CI on one immutable newest SHA and triage actual Java PG failures; then integrate signed portable source and C owner/permit into one native target transaction rather than starting a B-B-09 synthetic substream.
