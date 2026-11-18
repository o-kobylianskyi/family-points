# Backend C — C-C-02 Owner provenance contract / architecture decision request

**Date:** 2026-11-18. **Severity:** P0 before enabling BKL-002 Reset. **Status:** AWAITING ARCHITECTURE / INTEGRATOR APPROVAL.

## Why a migration is needed

Current production `Workspace` (table `families`) has ID/name/timezone but no original creator account ID. `WorkspaceMember` (`family_members`) records a mutable role, type, active state and family, but not immutable creator provenance. `WorkspaceSecurity.canManageWorkspace` permits `MANAGE_WORKSPACE` or `ADMIN_OVERRIDE`; these are insufficient to authorize irreversible family Reset. `WorkspaceResetProofService` proves a current account credential but not original ownership. The normal `WorkspaceController.createWorkspace` explicitly forbids direct creation and directs registration through a separate flow.

**Do not infer owner from:** min(member ID), first join date, FAMILY_ADMIN role, MANAGE_WORKSPACE/ADMIN_OVERRIDE, member type, currently selected workspace, an OAuth email match, or an account that happens to be linked to that workspace.

## Proposed canonical identity record (requires approval)

`workspace_original_owner_bindings` / equivalent authoritative owner column:

- `workspace_id` — PK and FK to `families.id`, deletion restricted.
- `owner_account_id` — immutable original creator FK to global `user_accounts.id`.
- `owner_member_id` — exact workspace-local member FK; must belong to `workspace_id` with same account via `account_workspace_memberships`.
- `provenance` — enum `REGISTRATION_VERIFIED`, `LEGACY_VERIFIED`; **never** `INFERRED_ADMIN`.
- `verified_at`, `created_at`, verifier/operator audit reference for legacy mappings; do not put passwords/tokens in any field.
- Unique per workspace; no auto-transfer on member role changes. Owner transfer, if ever supported, must be separate audited flow with recent two-party authorization.
- On owner account deletion/disable, missing original member, role change or legacy unknown provenance: deny Reset; do not silently reassign to an arbitrary administrator.
- Validate `workspace_id,owner_member_id,owner_account_id` together with a same-workspace FK/DB constraint when feasible. At runtime rejoin active `family_members`, enabled `user_accounts` and active `account_workspace_memberships` under the workspace lock.

## Migration/backfill and registration

1. Introduce nullable/unpopulated or dedicated owner binding storage through a Flyway migration in a separately reviewed scope; migration itself MUST NOT elevate any existing admin to verified owner.
2. Update actual transactional **initial and additional family registration** to save exact creator account+new owner member+workspace binding atomically. Do not commit workspace without binding for newly created families.
3. Legacy workspaces: present an explicit owner-verification process to Architecture/Release using external trustworthy registration evidence. Until verified, `canResetWorkspace` returns false and capabilities explain unavailable original-owner provenance without revealing other families.
4. Preserve global user identities and all existing memberships in other families. Never use owner binding as generic `canManageWorkspace`; only Reset-specific destructive privilege.
5. Migration owner and registration source files must be assigned by Integrator before C edits to avoid races with A/B/D and Onboarding V49.

## Required server policy

`WorkspaceSecurity.canResetWorkspace(workspaceId)` must fail closed unless all hold:
- Authenticated current user account ID exactly equals **verified** original owner account ID for target workspace.
- Current auth details refer to the exact target workspace **and** bound active owner member ID.
- UserAccount enabled, owner membership active, not deleted; its account/member/workspace link exists and remains current.
- Verified provenance record exists, no duplicate/ambiguous binding, member workspace matches supplied path.
- Reauthorization via **fresh server credential** and operation/scope/epoch proof still separately required. Ownership alone does not execute anything.

Check the same policy at preview/plan/prepare/confirm/challenge and re-check in the future execute transaction; HTTP 423 remains until all release gates.

## Regression acceptance

| Case | Expected |
|---|---|
| Verified creator account A / family A, active bound member | Eligible for challenge only after fresh credential; never execute automatically |
| Account A is a manager in family B, original owner in family A | Reset A eligible; Reset B forbidden |
| Delegated `MANAGE_WORKSPACE` or `ADMIN_OVERRIDE` in A | Reset A forbidden even with valid own password |
| Unknown legacy owner record | All Reset privileges fail closed |
| Account/member mismatch within same workspace | Forbidden |
| Owner disabled, member soft-deleted, revoked workspace link | Forbidden |
| Owner role downgraded | Policy as architect approves; never infer new owner |
| Google-only original owner | Must provide explicitly verified fresh provider path; otherwise challenge disabled |
| Two concurrent admin/owner changes and challenge consumption | Lock ordering and rechecks prevent TOCTOU; no successful data mutations |
| Reset `execute` before release approval | HTTP 423, zero data mutations |

## Cross-track disposition

**Architecture/Integrator action requested:** approve owner binding schema and legacy provenance process; assign exact registration/owner migration files; resolve owner transfer policy, original-owner vs designated-admin policy, and shared family lock order. C will then implement the policy and tests in one coherent batch. This request is NOT permission to perform any real reset.

**Evidence status:** Source review only. No schema migration or Java writes in this package, no Maven/PostgreSQL CI claimed.
