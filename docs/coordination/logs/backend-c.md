# FamilyPoints — Backend C implementation journal

## 2026-11-18 — C-C-00: take ownership and segregate Reset from Restore
- Direction assigned: **Backend C — Security & Reset**.
- Inspected baseline `main` `129d3dca1542a28905934726828f7fae95c1814a`, legacy reset ADR/XTR, revised pre-BETA scope, and Backend B latest handoff.
- Created dedicated handoff `docs/coordination/handoffs/BACKEND_C_LATEST.md`.
- Security review conclusion: never equate restore authorization with reset authorization; original owner identity, fresh server reauth and durable one-use operation-bound approval are mandatory. Financial mutation plans are owned by D.
- **Evidence:** coordination only; implementation NOT STARTED, tests NOT RUN, destructive execution NOT AUTHORIZED.
- **State:** Reset HTTP 423; BETA HOLD.
- **Next:** C-C-01 repo inventory / security baseline; contract ACK with Integrator, then C-C-02 owner and reauth tests.

## 2026-11-18 — C-C-01: live production source audit (completed, no mutation)
- Inspected actual `WorkspaceDataResetPreviewController`, `WorkspaceDataResetPrepareController`, `WorkspaceResetChallengeController`, `WorkspaceResetProofService`, `ResetChallengeAttemptLimiter`, `WorkspaceSecurity`, `CurrentUserService`, `Workspace`, `WorkspaceMember`.
- Security blocker **C-SEC-01:** `canManageWorkspace` permits delegated `MANAGE_WORKSPACE`/`ADMIN_OVERRIDE` but cannot prove original owner. Current reset controllers all use that predicate; fresh reauth only proves current actor. No safe original-owner field observed in inspected workspace/member entities. Require Architecture/Integrator owner-provenance decision before adding destructive authorization.
- Challenge proof lifecycle is more advanced than initial C-C-00 assumption: existing server already has random/hashed secret, TTL, durable consume and epoch binding; avoid redundant implementation or accidental external proof exposure.
- Source audit and explicit 10-group negative-test matrix: `docs/coordination/requests/BACKEND_C_SECURITY_RESET_AUDIT_20261118.md` @ `bd291acb`.
- **Status:** audit COMPLETED, C code NOT MODIFIED, Maven/PG NOT_RUN; Reset Execute HTTP423, BETA HOLD.
- **Next:** get ownership schema/migration ACK, implement isolated `canResetWorkspace` with fail-closed tests, re-check CI exact SHA.

## 2026-11-18 — C-C-02A: original owner provenance analysis and contract
- Inspected `Workspace`, `WorkspaceMember`, `WorkspaceController`, `WorkspaceSecurity`, `CurrentUserService`, `UserAccountService` and `WorkspaceJoinService`. Existing mutable role/privilege is not reliable creator provenance; `WorkspaceController.createWorkspace` blocks direct creation in favor of registration.
- Published explicit migration and original-owner authorization request: `docs/coordination/requests/BACKEND_C_OWNER_PROVENANCE_CONTRACT_20261118.md` @ `daad2432`.
- Plan requires **verified** creator account+member+workspace provenance, no guessed legacy owner backfill; own-workspace and account lifecycle checks, cross-workspace denial and retention of HTTP423. Coordinator must assign registration/migration file ownership first.
- **State:** C-C-02 policy implementation BLOCKED ON CONTRACT ACK, no Java or SQL migrations applied; no tests executed, BETA HOLD unchanged.
- **Next:** once approved, implement owner binding transactionally in registration and standalone reset-only ACL with negative tests; do not infer current manager=original owner.

## 2026-11-18 — C-C-02B: owner identity predicate and negative tests (source committed)
- Added isolated, side-effect-free `security/ResetOriginalOwnerPolicy.java` @ `867018a0`. Owner eligibility requires exact requested workspace, current authenticated enabled account, active/non-deleted member, current account-member link and explicit `REGISTRATION_VERIFIED` / `LEGACY_VERIFIED` binding of account+member+workspace. Unknown/null provenance fails closed. Role/ADMIN_OVERRIDE never substitutes provenance.
- Added `security/ResetOriginalOwnerPolicyTest.java` @ `8dd944ad` with seven JUnit test methods and multiple assertions: positive trusted binding, unknown/missing binding, cross-workspace shared account, delegated manager, member mismatch, disabled/deleted/revoked/unauthed, missing IDs, binding mismatch.
- **Not wired into production controllers**: canonical DB binding and signup/backfill migration have no Architecture ACK. A pure policy is a necessary identity predicate, **not** credential proof, owner-resolution service, approved endpoint auth, migration, or enablement.
- **Evidence:** new source and tests pushed to GitHub main. Tests are **NOT_RUN** (source-only connector; no Maven baseline); do not claim PASS or exact-SHA CI.
- **Safety:** `execute` remains HTTP423, no existing controller/DB/Flyway changed, BETA HOLD.
- **Next:** architecture approves provenance schema; C adds transactional resolver + protected `canResetWorkspace`, wires Reset endpoints and executes unit/PG CI. Meanwhile, C can improve isolated proof abuse/replay negative coverage without enabling execution.

## 2026-11-18 — C-C-03: one-use Reset proof negative test package
- Inspected `WorkspaceResetProofService`, `WorkspaceEpochTransactionGuard` and existing challenge controller test; found present 5-minute proof/epoch, credential fingerprint, account/member and conditional consumed-at protections.
- Committed `WorkspaceResetProofConsumptionSecurityTest` (7 JUnit cases, synthetic Mockito) @ `39a1ece4`: successful consumption only; consumed reuse, expiry, prior epoch, rotated password, wrong actor/member/secret/digest, other family/challenge and lost conditional update. No production business logic changed.
- QA request `docs/coordination/requests/BACKEND_C_QA_PACKET_20261118_PROOF_CONSUMPTION.md` @ `f7723020`; exact-SHA `mvn -Dtest=WorkspaceResetProofConsumptionSecurityTest test` acceptance pending.
- **Evidence:** source commits only, Maven NOT_RUN, PostgreSQL NOT_RUN, no CI green claimed; ownership provenance migration still unapproved.
- **Release:** HTTP 423, BETA HOLD; do not infer full concurrent DB race protection from Mockito tests.
- **Next:** verify QA execution and add isolated PostgreSQL physical concurrency tests after schema/owner provenance sign-off. Can audit rate-limit behavior in parallel.

## 2026-11-18 — C-C-04: persisted proof verifier integrity and reauth budget coverage
- Inspected `WorkspaceResetProofService`, `WorkspaceEpochTransactionGuard`, `ResetChallengeAttemptLimiter` and their current unit tests.
- Production hardening @ `1534d864`: a durable challenge with missing/wrong-typed actor/binding or non-hex/absent 64-char verifier hash now rejects `409 RESET_FRESH_CHALLENGE_REQUIRED` rather than risking NullPointerException/corrupt-state 500. Correct proof path and HTTP423 Execute unmodified.
- Extended synthetic proof tests @ `fbbae497`: absent/malformed verifier, missing account/epoch/member/digest/credential fingerprint. Extended rate limiter tests @ `e133438c`: null count fail closed, SQL error propagates, separate workspace/account keys.
- QA target and exact source SHA instructions: `docs/coordination/requests/BACKEND_C_QA_PACKET_20261118_PROOF_HARDENING.md` @ `5b4336ae`.
- **Evidence:** SOURCE_COMMITTED, Maven/JUnit/PG **NOT_RUN**; no green CI claimed. Backend C ownership/provenance migration still awaiting Architecture sign-off. No real Reset or Restore enabled.
- **Next:** inspect QA output; build reproducible PG18 concurrency test for token consumption and reset reauth throttling on disposable DB. Keep test-only until Integrator approves shared migration/owner contract.

## 2026-11-18 — C-C-05: real PostgreSQL 18 concurrency and rollback test source
- Inspected existing `WorkspaceDisposableRestoreRehearsalPostgresTest`, `DisposableDurableRestoreApprovalStorePostgresTest`, `WorkspaceResetAuditSchemaPostgresTest`, schema V43/V44 and production `WorkspaceResetProofService`.
- New `WorkspaceResetProofConcurrencyPostgresTest` @ `2119befd` uses actual production proof lifecycle (not cloned SQL), disposable Testcontainers PostgreSQL 18, Flyway, two tenant fixtures, BCrypt credentials and TransactionTemplate.
- Four test cases: simultaneous proof consumption exactly once, injected rollback then retry then replay denial, other-workspace isolation, persisted epoch advancement denies stale proof. No Reset/Restore Execute and no family data deletion.
- QA execution packet `docs/coordination/requests/BACKEND_C_QA_PACKET_20261118_PROOF_PG_CONCURRENCY.md` @ `45333c63`.
- **Evidence:** test source committed; actual Maven/Docker/PostgreSQL tests **NOT_RUN** from connector, no green result claimed; CI exact SHA must be independently checked.
- **Next:** triage actual QA failures; await Architecture owner provenance ACK before wiring destructive REST permissions. Maintain Reset HTTP423 and BETA HOLD.


## 2026-11-18 — C-C-06: owner product ACK and PostgreSQL proof QA follow-up
- Owner approved product decisions ABCD-DEC-01…08 in shared record `8571566f`; engineering schema/DTO and legacy owner verification still need Architecture ACK.
- CI run `44268695217` Java job was in progress at inspection. No current full green claimed. QA corrected two unrelated Testcontainers 2.x compile errors.
- PG18 proof suite expanded to 6 methods @ `52e79a29`: DB-persisted expiry + password rotation negatives. Existing two-thread replay, transaction rollback/retry, cross-family and epoch cases remain.
- Updated independent QA packet @ `a4b34db9` and issue #15 comment `6973470561`. Maven/PG tests **NOT_RUN** by C. No production Reset Execute (HTTP423), BETA HOLD.
- Next: QA exact-SHA results, fix failures, await engineering canonical original-owner registration/migration contract for controller integration.


## 2026-11-18 — C-C-07: action-specific permit contract and CI triage
- Inspected accepted Architecture `ADR-BKL002-ABCD-INTEGRATION-003` @5218b9a and issue #15; implementation decision accepted, exact per-consumer schema/DTO/IMP still required. No more routine user reapproval.
- Independently fetched #44268695217 Java workflow logs: 572 tests, 1 failure and 1 error, both Backend B `DisposableFinancialTargetRehearsalPostgresTest` synthetic duplicate-key; previous green does not cover moving HEAD. Published failure details to issue #15.
- Added `WorkspaceDestructivePermitContract` @306850b8: immutable action-scoped RESET vs RESTORE identity/snapshot/selection/mapping/epoch/expiry <=5min pure descriptor; not a grant or runtime authorization. Added 5 pure tests @fac58ba8; NOT_RUN.
- Concrete B consumer ACK request `docs/coordination/requests/BACKEND_C_TO_B_DURABLE_PERMIT_CONTRACT_20261118.md` @8901b7d; issue #15 comment 6973579412. Target family -> operation/permit -> rows lock order and atomic restore consume+epoch+receipt remain proposed pending exact IMP/consumer review.
- Safety: HTTP423 Reset, production Restore blocked and BETA HOLD. No Flyway, credential transport or destructive endpoint wired. New tests not independently executed.
- Next: obtain B ACK, file IMP for shared permit/storage; implement verified owner enrollment and transactional DB-backed scope/grant only with reviewed integration. QA pinned-SHA target test then full CI.


## 2026-11-18 — C-C-08: large PG18 durable permit and original owner binding QA
- Architecture `ADR-BKL002-ABCD-INTEGRATION-003` has accepted implementation principles; C/B exact wire, shared Flyway/IMP, legacy owner evidence and Restore receipt remain consumer-reviewed gates.
- Added `src/test/.../security/DisposableOwnerPermitStore.java` @ `5903f595`, **test-only** PostgreSQL QA permit tables. Simulates action-specific SHA256-verified 256-bit random token, 5min TTL, scoped family/owner/snapshot/selection/mapping/epoch, target family FOR UPDATE -> permit FOR UPDATE -> token consumed -> epoch bump -> receipt in one JDBC transaction. No actual owner auth, no production component/HTTP/migration.
- Added `DisposableOwnerPermitStorePostgresTest.java` @ `f824738e`, 6 tests: success/no replay, rollback and retry, cross-action/family denial, wrong owner/token/epoch, duplicate issuance/expiry, real 2-thread race exactly once.
- Added `DisposableOriginalOwnerBindingPostgresTest.java` @ `926012f0`, 6 tests: absent legacy, verified owner/delegated denial, deactivation, revocation, provenance uniqueness, shared-account neighbor denial. Synthetic enrollment in QA table is NOT authenticated legacy enrollment.
- QA packet @ `ce3a75ea` `docs/coordination/requests/BACKEND_C_C08_LARGE_PG18_SECURITY_QA_20261118.md`. Implementation impact/request @ `05a773ba` `docs/coordination/requests/BACKEND_C_C08_IMPLEMENTATION_IMPACT_20261118.md`. Issue #15 shared cross-team handoff comment `6973705838`.
- **Evidence:** 12 additional PG18 JUnit test methods SOURCE_COMMITTED, Maven/Testcontainers NOT_RUN by Backend C. Prior actual CI #44268695217 572/1 failure/1 error (B-owned QA fixture); historical current full green NOT established.
- **Safety:** no production Flyway, owner enrollment, actual permit issuance, RESET/RESTORE Execute, real data mutation or deployment. HTTP423 + BETA HOLD.
- **Next:** exact SHA QA failures triage and Backend B consumer ACK/REVISE. Once IMP/owner provenance and real credential contract signed, implement production owner resolver and strictly scoped durable permit store atomically with approved operations.


## 2026-11-18 — C-C-09: B conditional ACK / canonical restore permit scope v2
- Received and reviewed Backend B **CONDITIONAL ACK** `BACKEND_B_TO_C_RESTORE_PERMIT_CONSUMER_ACK_20261118.md` @25d2b6cc and Architecture Review 02 issue #15. It confirms action-scoped C auth, single transactional B RESTORE consumer, target family lock -> grant -> categories -> durable receipt; requires source workspace/schema/manifest/root+epoch, canonical mapping+selection closure digest and approved IMP before shared migration.
- Corrected C-C-08 QA-only original-owner fixture double-PARENT role insertion @ `58c40601`, then eliminated assumption of a particular SQL `ON CONFLICT` target @ `4ffcf6ac`. No production code affected.
- Added `WorkspaceRestorePermitScopeV2.java` @ `8345305b` with independently bound sourceWorkspaceId/schemaVersion/snapshotId/sourceSnapshotEpoch/snapshot+manifest SHA256 and target workspace/owner/targetEpoch, canonical injective sorted source→target mapping SHA and binary versioned domain-separated scope SHA; JUnit `WorkspaceRestorePermitScopeV2Test` 7 test methods @ `2ce5003b`.
- Added `WorkspaceRestoreSelectionClosureV2.java` @ `598b26c3`: sorted complete declared category list with RESTORE/PRESERVE/BLOCK, source counts, category/closure/conflict SHA, source/target family+schema digest; 5 JUnit tests @ `06b73c4f`.
- Exact field/encoding/owner-review proof and recipient ACK request `docs/coordination/requests/BACKEND_C_C09_CONTRACT_V2_B_ACK_20261118.md` @ `e6cb818a`; issue #15 coordinator/QA comment `6974144577`.
- CI context: QA-052 found six prior `WorkspaceResetProofConcurrencyPostgresTest` fixture errors from V41 `confirmed_at` CHECK and repaired fixture @cee9f3f3. New C-C-09 source **NOT_RUN** by C; no current exact-SHA green claimed.
- **Safety:** no production owner persistence, permit issuance, schema Flyway/HTTP changes, external calls, destructive Reset/Restore. Reset HTTP423, BETA HOLD.
- **Next C-C-10:** incorporate B/A/D final field-level ACK and Architecture IMP; implement audited immutable original owner binding and authenticated registration writes, server-issued durable owner permit with actual PostgreSQL transactional consumption and complete QA. Do not wire dangerous endpoints early.
