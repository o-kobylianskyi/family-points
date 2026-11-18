# Backend C — C-C-01 Security & Reset source inventory (2026-11-18)

**Inspected:** GitHub `main` after C-C-00 docs; inspected specific current file contents (not a compilation or CI run). **No DB deletes; production Execute still HTTP 423.**

## Observed production code

| Source | Current implementation | C assessment |
|---|---|---|
| `controller/WorkspaceDataResetPreviewController.java` | GET preview/category counts, GET PostgreSQL FK graph, POST plan; `executionEnabled=false`; `@PreAuthorize(canManageWorkspace)` | Read-only inventory, NOT a complete FK/category delete plan. Preview access currently inherits delegated manager privileges. |
| `controller/WorkspaceDataResetPrepareController.java` | POST prepare idempotent snapshot record; POST confirm locks prepared operation and checks typed `RESET {workspaceId}` and snapshot; `@PreAuthorize(canManageWorkspace)` | Durable **acknowledgment**, NOT identity proof. No original owner check. |
| `controller/WorkspaceResetChallengeController.java` | POST challenge checks membership, password, operation, snapshot, epoch, rate limit; delegates to `WorkspaceResetProofService`; POST execute remains HTTP 423 | Fresh local-password check exists. Still allows delegated `MANAGE_WORKSPACE`/ `ADMIN_OVERRIDE` because no reset-specific original-owner gate. Provider-only accounts fail closed. |
| `service/WorkspaceResetProofService.java` | Random 256-bit internal secret, SHA-256 verifier, 5 min expiry, DB uniqueness/consume, account/operation/member/digest/credential/epoch binding, mandatory transaction | Foundational proof lifecycle exists; HTTP challenge intentionally returns only UUID and expiry, **not consumable secret**. No legitimate production executor; do not expose token until approved. |
| `service/ResetChallengeAttemptLimiter.java` | Independent `REQUIRES_NEW` database-backed 15 min attempt counter | Count survives failed challenge; further examine abuse model/actor+IP policy in tests. |
| `security/WorkspaceSecurity.java` | `canManageWorkspace` accepts `MANAGE_WORKSPACE` OR `ADMIN_OVERRIDE` for active same-workspace member | **P0 authorization gap** vs ADR-FAMILY-RESET-001: delegated manager is not necessarily original owner. Do not weaken general workspace management; add a separate reset-specific policy instead. |
| `security/CurrentUserService.java` | Resolves authenticated account and active membership; workspace-aware auth details enforce bound account/member | A same-account membership in another workspace does not imply ownership; no explicitly verified workspace creator/original owner provenance observed in inspected entity/model. |
| `entity/Workspace.java`, `entity/WorkspaceMember.java` | Family has ID/name/timezone; member has type/role/family/active/deleted; no original-owner identity field in these two entities | Cannot securely infer original owner by member type, role name, current admin grants, or smallest member ID. Confirm canonical source and schema with Architecture before implementing owner gate. |

## Blocker C-SEC-01 — provenance-based original-owner authorization

**Observed:** all three reset-related controllers use `@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")`, and that predicate allows a delegated workspace manager or any active holder of `ADMIN_OVERRIDE`. That is weaker than the agreed high-impact Reset policy. `WorkspaceResetProofService.currentCredential` binds the currently logged-in account+membership, but it does not establish that account as workspace original owner. Reauth proves **who** is acting, not **whether** they are permitted to reset.

**Required decision from Architecture/Integrator:**
1. Identify authoritative persistent **original owner account ID** for every workspace, or approve an explicit auditable ownership migration/backfill. Never auto-migrate using guessed first member/ADMIN role.
2. Determine how owner transfer, owner account disable/delete and legacy workspace without owner provenance behave. Unknown owner => Reset capability fail closed, even with `ADMIN_OVERRIDE`.
3. Add reset-only predicate, e.g. `canResetWorkspace(workspaceId)`, resolving current enabled global principal + exact current active member + immutable/authorized original-owner binding; do not modify `canManageWorkspace` for unrelated operations.
4. Require this predicate at **preview, plan, prepare, confirm, challenge and future execute**, and **inside execution transaction** under workspace/ownership lock.
5. Keep provider-only account reauth unavailable until explicitly supported OAuth proof or secure local password setup; do not silently accept JWT age or typed phrase alone.
6. Decide shared epoch lock owner (C reset vs B restore) and avoid mismatched lock ordering.

## Negative tests before approving any execute

- Original owner of A with active account+membership and fresh local password can preview/challenge; cannot reset B unless separately original owner of B.
- Delegated MANAGE_WORKSPACE member and ADMIN_OVERRIDE-only member of A are **denied** Reset even with their own valid password.
- Child, deactivated/soft-deleted member, revoked membership, disabled account and actor changing workspace between stages denied.
- Two accounts share same member? Guard by authenticated account **and** exact owner binding; password of unrelated account cannot satisfy proof.
- Owner whose credential rotated since challenge fails consumption; 5min expiry, token reuse, concurrent double-consumption and changed digest/epoch rejected.
- Google-only account fails closed; no implicit local password bypass.
- Preview produces no audit/epoch/financial data mutation; GET and POST endpoints do not expose cross-workspace counts.
- Same operation UUID with different categories/digest returns conflict; exact replay returns original immutable operation state.
- Execute stays HTTP 423 until full FK/financial closure, epoch atomicity, two-workspace PostgreSQL tests and Release gate pass.

## Other findings / no-overlap constraints
- `WorkspaceDataResetPreviewController.plan` explicitly reports incomplete dependency closure; all categories `executable=false`. Treat warning strings as design placeholders, never an executable permission matrix.
- `WorkspaceDataResetPrepareController.confirm` is non-reauth acknowledgment, correctly reported `executable=false`; no Web client may treat confirmation as execute authorization.
- Proof secret is generated internally but not transported in current response; **do not** wire proof consumption or enable Execute from C independently. Authorization transport policy and typed-family-name confirmation belong to Integrator-approved contract.
- Current `WorkspaceResetProofService` is reset-only. Backend B has separate disposable Restore approval experiments; do not reuse one action's proof for the other.
- The legacy 2026-10-24 XTR/ADR labels have outdated freeze notices. Revised 2026-10-26 BETA scope supersedes the historical reset hold, but never authorizes a real data wipe.
- **Cannot assert all repository tests pass:** connector provides source inspection only, not a local Maven/PostgreSQL environment or pinned green CI evidence.

## C-C-02 work order (blocked on owner provenance confirmation)
1. Architecture/Integrator signs off canonical original-owner persistence and migration/backfill policy.
2. Backend C adds reset-only Spring Security authorization and focused Mockito/security filter tests without touching generic manager permissions.
3. Include owner provenance recheck within challenge transaction and future execute boundary.
4. Publish exact code SHA, independent CI results and separate QA packet; keep HTTP423 and BETA HOLD.
