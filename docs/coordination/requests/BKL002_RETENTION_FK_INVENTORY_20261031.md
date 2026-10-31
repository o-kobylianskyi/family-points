# BKL-002 — exact schema dependency and retention decision inventory, 2026-10-31

**Status: REVIEW REQUIRED / NO DELETION APPROVAL.** This inventory is generated from pg_catalog after V1..V45 on disposable PostgreSQL18 by WorkspaceResetProofUpgradePostgresTest. It contains schema metadata only: **70 application tables and 156 foreign keys**; Flyway bookkeeping is excluded from that count and always retained. No DEV/BETA/PROD schema or family contents were queried. CI evidence is appended after final success.

The disposition column is a conservative Backend review proposal/default retention, NOT an approved purge plan. All destructive operations remain disabled. It supersedes the assumption that 13 preview categories constitute complete dependency closure. Existing content-v2 snapshot traversal is a freshness mechanism, not a deletion graph or owner policy.

## Required owner and policy decisions

- No owner account/member field exists in families today. family_members, family_roles and legacy user_accounts.family_member_id do not establish original ownership. Do not infer owner from lowest ID, first timestamp, PARENT enum, system role code or the account's legacy default member.
- Proposed new-family solution: explicit immutable/audited owner binding at authenticated creation with deliberate transfer/delegation; legacy families require verified audited enrollment/backfill and must fail closed while unconfigured. This is a schema/policy proposal, not an assignment of any real family owner.
- A future executor must retain family ID, original/designated owner account + active membership + minimum admin permissions, all other-family identities and credentials, global bundled catalog, immutable security/reset audit and migrations.
- Decide exact MVP category combinations and whether retained history implies archiving referenced task/reward definitions rather than deleting them. No automatic co-delete to make FKs pass.
- Financial zeroing with history retained requires append-only reviewed adjustment postings; economic-history deletion needs explicit approved source/entitlement/FK closure. No balance rewrite, invented refund or implicit price change.
- Resolve family-admin access to GLOBAL account credentials before relying on owner-password reauth. Workspace MANAGE_MEMBERS does not, by itself, prove entitlement to control an adult/shared global login. Any shared-account policy must address races with concurrent membership linking, including a consistent global-account locking rule.

## Table-by-table inventory

Scope columns are physical columns, not an assertion that every row is correctly scoped. An indirect table requires joining approved parents; a globally shared identity must never become a deletion target merely because it has a legacy member FK. Incoming FK count identifies dependent constraints, not row counts.

| Table | Direct scope / review anchor | Incoming FKs | Default retention / unresolved policy |
|---|---|---|---|
| `account_email_tokens` | GLOBAL: not reset-owned | 0 | PROTECTED IDENTITY — preserve |
| `account_external_identities` | GLOBAL: not reset-owned | 0 | PROTECTED IDENTITY — preserve |
| `account_workspace_memberships` | workspace_id | 1 | PROTECTED IDENTITY — preserve |
| `device_app_policies` | indirect: inspect FK/semantic links | 0 | DEVICE CONTROL — preserve; OS/device state is separate |
| `device_installed_apps` | indirect: inspect FK/semantic links | 0 | DEVICE CONTROL — preserve; OS/device state is separate |
| `device_parent_override_sessions` | indirect: inspect FK/semantic links | 0 | DEVICE CONTROL — preserve; OS/device state is separate |
| `economy_settings` | family_id | 0 | ECONOMY CONFIG — retain currencies/defaults/forms/rates unless separately approved |
| `families` | root id | 31 | PROTECTED IDENTITY — preserve |
| `family_members` | family_id | 32 | PROTECTED IDENTITY — preserve |
| `family_role_permissions` | indirect: inspect FK/semantic links | 0 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `family_roles` | family_id | 3 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `group_compositions` | indirect: inspect FK/semantic links | 0 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `group_membership_roles` | indirect: inspect FK/semantic links | 0 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `group_memberships` | indirect: inspect FK/semantic links | 1 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `group_permission_grants` | indirect: inspect FK/semantic links | 1 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `group_point_transactions` | indirect: inspect FK/semantic links | 0 | FINANCIAL HISTORY — protected until explicit retain/adjust/purge policy |
| `group_roles` | indirect: inspect FK/semantic links | 4 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `managed_devices` | workspace_id | 4 | DEVICE CONTROL — preserve; OS/device state is separate |
| `media_assets` | workspace_id | 2 | PRIVATE MEDIA — preserve unless explicit reviewed unlink/retention |
| `media_bindings` | workspace_id | 0 | PRIVATE MEDIA — preserve unless explicit reviewed unlink/retention |
| `media_blobs` | indirect: inspect FK/semantic links | 0 | PRIVATE MEDIA — preserve unless explicit reviewed unlink/retention |
| `member_email_invitations` | workspace_id | 0 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `member_exception_periods` | indirect: inspect FK/semantic links | 0 | PEOPLE CONFIG — retain by default |
| `member_groups` | family_id | 7 | GROUP STRUCTURE — permissions/hierarchy/ledger dependencies unresolved |
| `pending_invitation_sessions` | indirect: inspect FK/semantic links | 0 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `point_exchange_rates` | family_id | 1 | ECONOMY CONFIG — retain currencies/defaults/forms/rates unless separately approved |
| `point_exchange_requests` | indirect: inspect FK/semantic links | 0 | FINANCIAL HISTORY — protected until explicit retain/adjust/purge policy |
| `point_transactions` | indirect: inspect FK/semantic links | 0 | FINANCIAL HISTORY — protected until explicit retain/adjust/purge policy |
| `point_type_name_forms` | indirect: inspect FK/semantic links | 0 | ECONOMY CONFIG — retain currencies/defaults/forms/rates unless separately approved |
| `point_types` | family_id | 17 | ECONOMY CONFIG — retain currencies/defaults/forms/rates unless separately approved |
| `reputation_transactions` | indirect: inspect FK/semantic links | 0 | FINANCIAL HISTORY — protected until explicit retain/adjust/purge policy |
| `reward_categories` | family_id | 3 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_definition_categories` | indirect: inspect FK/semantic links | 0 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_definitions` | family_id | 9 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_obligation_blocked_categories` | indirect: inspect FK/semantic links | 0 | REWARD ENTITLEMENTS/HISTORY — financial/fulfillment retention decision required |
| `reward_obligation_blocked_rewards` | indirect: inspect FK/semantic links | 0 | REWARD ENTITLEMENTS/HISTORY — financial/fulfillment retention decision required |
| `reward_obligations` | family_id | 2 | REWARD ENTITLEMENTS/HISTORY — financial/fulfillment retention decision required |
| `reward_purchases` | indirect: inspect FK/semantic links | 1 | REWARD ENTITLEMENTS/HISTORY — financial/fulfillment retention decision required |
| `reward_requests` | family_id | 2 | REWARD ENTITLEMENTS/HISTORY — financial/fulfillment retention decision required |
| `reward_requirement_blocked_categories` | indirect: inspect FK/semantic links | 0 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_requirement_blocked_rewards` | indirect: inspect FK/semantic links | 0 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_requirements` | family_id | 3 | REWARD DEFINITIONS/RULES — dependency review required |
| `reward_weekly_windows` | indirect: inspect FK/semantic links | 0 | REWARD DEFINITIONS/RULES — dependency review required |
| `role_assignments` | family_id | 0 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `role_definitions` | family_id | 3 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `role_permission_grants` | indirect: inspect FK/semantic links | 0 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `role_set_bindings` | indirect: inspect FK/semantic links | 0 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `role_sets` | family_id | 3 | ACCESS CONTROL — retain by default; owner/delegation and scoped detach policy unresolved |
| `task_audit_events` | family_id | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_definition_required_group_roles` | indirect: inspect FK/semantic links | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_definition_required_role_definitions` | indirect: inspect FK/semantic links | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_definitions` | family_id | 7 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_delegations` | indirect: inspect FK/semantic links | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_instances` | indirect: inspect FK/semantic links | 3 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_participants` | indirect: inspect FK/semantic links | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_reward_requests` | family_id | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_schedule_slots` | indirect: inspect FK/semantic links | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `task_suggestions` | workspace_id | 0 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `template_activation_requests` | workspace_id | 0 | PRIVATE TEMPLATES/PROVENANCE — explicit selection and idempotency pairing required |
| `temporary_child_sessions` | indirect: inspect FK/semantic links | 0 | DEVICE CONTROL — preserve; OS/device state is separate |
| `user_accounts` | GLOBAL: not reset-owned | 10 | PROTECTED IDENTITY — preserve |
| `work_nodes` | family_id | 2 | TASK STRUCTURE/HISTORY — dependency and ledger reference review required |
| `workspace_invitation_redemptions` | indirect: inspect FK/semantic links | 0 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `workspace_invitations` | workspace_id | 2 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `workspace_join_codes` | workspace_id | 0 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `workspace_join_requests` | workspace_id | 0 | INVITATION/ACCESS — preserve or explicitly revoke, never recreate |
| `workspace_private_templates` | workspace_id | 1 | PRIVATE TEMPLATES/PROVENANCE — explicit selection and idempotency pairing required |
| `workspace_reset_challenges` | workspace_id | 0 | SECURITY/RESET AUDIT — preserve |
| `workspace_reset_operations` | workspace_id | 1 | SECURITY/RESET AUDIT — preserve |
| `workspace_reset_reauth_limits` | workspace_id | 0 | SECURITY/RESET AUDIT — preserve |

## Semantic dependencies outside PostgreSQL foreign keys

1. media_bindings.target_type + target_id names WORKSPACE/MEMBER/MEMBER_GROUP/WORK_NODE/TASK without an FK to each target. Deleting tasks/groups/work nodes without scoped binding cleanup leaves stale references; REWARD is NOT a supported writable target. media_blobs stores immutable normalized bytes and cascades from media_assets; respect explicit retention/ACL, not broad UUID deletion.
2. task_participants.actor_type + actor_id distinguishes MEMBER/GROUP and has no actor FK. role_assignments.actor_type/actor_id and context_type/context_id, role_set_bindings.context_type/context_id, role_sets/role_definitions.owner_context_* have semantic references that need tenant-safe detach or preservation. Keep owner/admin permission survival explicit.
3. point_transactions.source_type + source_id is a logical reference, not an FK to tasks, reward purchases or exchanges. Retaining ledger while deleting referenced source history can destroy explanations even when every FK is satisfied. Group/reputation ledgers and amounts also need financial policy, not count-based clearing.
4. Template definition_json and catalog_code/version preserve catalog provenance and can carry draft references/currency/member selections. template_activation_requests pairs private template IDs with operation claims. Keep claims for retained templates; any approved selected-template removal must reconcile its claims and generation fencing, with no replay-driven duplicate activation.
5. managed_devices and its policy/session descendants mirror device-side state; a database reset cannot remove Android Device Owner or guarantee policy removal/re-enrollment.
6. Scheduled generation, in-flight background jobs, browser caches and Android offline queues depend on epoch, not SQL FK metadata. Epoch increment/audit/onboarding must be atomic with the eventual data changes and old queue entries quarantined.
7. security audit of reset is workspace_reset_operations/challenges/reauth budget; task_audit_events is business history with task/instance/member FKs. Do not confuse business-history cleanup with permission to erase security audit.

## High-risk FK components to resolve before implementing deletion

- Task definitions have restrictive/no-action incoming task instances, audit, participants, legacy required-group roles and reward_requirements. A task checkbox is not a safe standalone DELETE. work_nodes and group_compositions also have self/hierarchy references.
- Reward definitions are referenced by retained purchases/requests, task_reward_requests, requirements and category/obligation junctions. Purchases can carry active time entitlement/fulfillment; obligations link purchase to requirement. CASCADE on a child relation is existing DB behavior, not approval to discard that entitlement.
- Member removal is outside the currently proposed safe default: tasks, purchases, transactions, devices, invitations, account memberships and legacy global-account member FK all refer to people. Scope revocation must never disable/delete the global account or another family.
- Point types/rates/settings/forms have ledger/task/reward/exchange references. Preserve currency defaults and custom localized forms; do not rebuild them as a side effect of unrelated cleanup.
- Reset operation audit has restrictive family/member FKs. Its challenge operation key is composite (workspace_id,operation_id), preserving tenant pairing. V45 actor IDs are immutable binding/audit values, not cascading identity ownership.

## Exact foreign-key inventory

Rows show **dependent -> referenced** edges. Delete ordering must consider incoming edges AND approved retention, logical references and immutable audit. CASCADE/SET NULL/NO ACTION are facts of the current schema, not a proposed execution order.

| Dependent columns | Referenced columns | ON DELETE | Constraint |
|---|---|---|---|
| `account_email_tokens(account_id)` | `user_accounts(id)` | CASCADE | `account_email_tokens_account_id_fkey` |
| `account_external_identities(account_id)` | `user_accounts(id)` | CASCADE | `account_external_identities_account_id_fkey` |
| `account_workspace_memberships(account_id)` | `user_accounts(id)` | RESTRICT | `account_workspace_memberships_account_id_fkey` |
| `account_workspace_memberships(family_member_id)` | `family_members(id)` | RESTRICT | `account_workspace_memberships_family_member_id_fkey` |
| `account_workspace_memberships(workspace_id)` | `families(id)` | RESTRICT | `fk_account_membership_workspace` |
| `device_app_policies(device_id)` | `managed_devices(id)` | CASCADE | `device_app_policies_device_id_fkey` |
| `device_installed_apps(device_id)` | `managed_devices(id)` | CASCADE | `device_installed_apps_device_id_fkey` |
| `device_parent_override_sessions(authorized_by_member_id)` | `family_members(id)` | NO ACTION | `device_parent_override_sessions_authorized_by_member_id_fkey` |
| `device_parent_override_sessions(device_id)` | `managed_devices(id)` | NO ACTION | `device_parent_override_sessions_device_id_fkey` |
| `economy_settings(default_point_type_id)` | `point_types(id)` | NO ACTION | `fk26spp5jjfc3val5wc9uhr3658` |
| `economy_settings(family_id)` | `families(id)` | NO ACTION | `fklculytsilhw76ftfwh30lknge` |
| `family_members(family_role_id)` | `family_roles(id)` | NO ACTION | `fkav6hhlu8oj6rf5v8dsnv8i18y` |
| `family_members(family_id)` | `families(id)` | NO ACTION | `fkeynpxqr1l03689buyid3e1d8l` |
| `family_role_permissions(role_id)` | `family_roles(id)` | NO ACTION | `fkognf4n12fs0s2v1fb4dupr3sb` |
| `family_roles(family_id)` | `families(id)` | NO ACTION | `fktfpl0sc7rcpdjtwtyojqflhqp` |
| `group_compositions(child_group_id)` | `member_groups(id)` | NO ACTION | `fk72tlcmbw15hqbxd91k76ljff6` |
| `group_compositions(parent_group_id)` | `member_groups(id)` | NO ACTION | `fkfuag7i3uo2njrguo3e91cmie4` |
| `group_membership_roles(group_role_id)` | `group_roles(id)` | NO ACTION | `fkc04qhyj821yhx4qwyq6yrx1gq` |
| `group_membership_roles(membership_id)` | `group_memberships(id)` | NO ACTION | `fklago5tg63uak9053jrsfq0f9k` |
| `group_memberships(member_id)` | `family_members(id)` | NO ACTION | `fkb5q69qawtmw9dsefpy1xsjemt` |
| `group_memberships(member_group_id)` | `member_groups(id)` | NO ACTION | `fkngctlxthgxvleict0lcyrk195` |
| `group_permission_grants(group_role_id)` | `group_roles(id)` | NO ACTION | `group_permission_grants_group_role_id_fkey` |
| `group_permission_grants(member_group_id)` | `member_groups(id)` | NO ACTION | `group_permission_grants_member_group_id_fkey` |
| `group_point_transactions(point_type_id)` | `point_types(id)` | NO ACTION | `fkpan37yrm8tr2s7atd44lmas1u` |
| `group_point_transactions(group_id)` | `member_groups(id)` | NO ACTION | `fkpuvbbmu9ilywiiy9r237ri987` |
| `group_roles(member_group_id)` | `member_groups(id)` | NO ACTION | `fkcleybr11y5vitcounns89k06n` |
| `group_roles(role_set_id)` | `role_sets(id)` | NO ACTION | `group_roles_role_set_id_fkey` |
| `managed_devices(owner_member_id)` | `family_members(id)` | NO ACTION | `managed_devices_owner_member_id_fkey` |
| `managed_devices(workspace_id)` | `families(id)` | NO ACTION | `managed_devices_workspace_id_fkey` |
| `media_assets(created_by_member_id)` | `family_members(id)` | NO ACTION | `media_assets_created_by_member_id_fkey` |
| `media_assets(workspace_id)` | `families(id)` | NO ACTION | `media_assets_workspace_id_fkey` |
| `media_bindings(asset_id,workspace_id)` | `media_assets(id,workspace_id)` | NO ACTION | `fk_media_binding_asset_workspace` |
| `media_bindings(workspace_id)` | `families(id)` | NO ACTION | `media_bindings_workspace_id_fkey` |
| `media_blobs(asset_id)` | `media_assets(id)` | CASCADE | `media_blobs_asset_id_fkey` |
| `member_email_invitations(issued_by_account_id)` | `user_accounts(id)` | NO ACTION | `member_email_invitations_issued_by_account_id_fkey` |
| `member_email_invitations(member_id)` | `family_members(id)` | NO ACTION | `member_email_invitations_member_id_fkey` |
| `member_email_invitations(workspace_id)` | `families(id)` | NO ACTION | `member_email_invitations_workspace_id_fkey` |
| `member_exception_periods(member_id)` | `family_members(id)` | NO ACTION | `fkpl66l5irgmnjkahlf2te7l89x` |
| `member_groups(family_id)` | `families(id)` | NO ACTION | `fkj6akrjnmhc5g6h0kwq1059hd0` |
| `pending_invitation_sessions(account_id)` | `user_accounts(id)` | NO ACTION | `pending_invitation_sessions_account_id_fkey` |
| `pending_invitation_sessions(invitation_id)` | `workspace_invitations(id)` | NO ACTION | `pending_invitation_sessions_invitation_id_fkey` |
| `point_exchange_rates(to_point_type_id)` | `point_types(id)` | NO ACTION | `fkddtgrrxeher1oxecqn38wf4yq` |
| `point_exchange_rates(from_point_type_id)` | `point_types(id)` | NO ACTION | `fkiilk7c6yosd2radpwp9x50xh7` |
| `point_exchange_rates(family_id)` | `families(id)` | NO ACTION | `fkmf7y4lk5m9isdl202hjooj9g7` |
| `point_exchange_requests(to_point_type_id)` | `point_types(id)` | NO ACTION | `fk3syuqjk062g0erjbrwun51qc` |
| `point_exchange_requests(member_id)` | `family_members(id)` | NO ACTION | `fk60nb9hselk5xp9lsbo51e7haj` |
| `point_exchange_requests(from_point_type_id)` | `point_types(id)` | NO ACTION | `fkkymuoeurjm2k0j6bybojymmmq` |
| `point_exchange_requests(processed_by_member_id)` | `family_members(id)` | NO ACTION | `fkm95jl6y7nxc31b95tbk8vxrp2` |
| `point_exchange_requests(exchange_rate_id)` | `point_exchange_rates(id)` | NO ACTION | `fkod1xdc4djosi07g7hxona8sfw` |
| `point_transactions(point_type_id)` | `point_types(id)` | NO ACTION | `fk8nb70wuclf8bx298fj7opjbwj` |
| `point_transactions(member_id)` | `family_members(id)` | NO ACTION | `fkso9q14686eikg14r60jeigk5g` |
| `point_type_name_forms(point_type_id)` | `point_types(id)` | CASCADE | `point_type_name_forms_point_type_id_fkey` |
| `point_types(family_id)` | `families(id)` | NO ACTION | `fkp59t37brlfs7hsj3jdfjn24hb` |
| `reputation_transactions(member_id)` | `family_members(id)` | NO ACTION | `reputation_transactions_member_id_fkey` |
| `reward_categories(family_id)` | `families(id)` | NO ACTION | `reward_categories_family_id_fkey` |
| `reward_definition_categories(category_id)` | `reward_categories(id)` | CASCADE | `reward_definition_categories_category_id_fkey` |
| `reward_definition_categories(reward_definition_id)` | `reward_definitions(id)` | CASCADE | `reward_definition_categories_reward_definition_id_fkey` |
| `reward_definitions(family_id)` | `families(id)` | NO ACTION | `reward_definitions_family_id_fkey` |
| `reward_definitions(point_type_id)` | `point_types(id)` | NO ACTION | `reward_definitions_point_type_id_fkey` |
| `reward_obligation_blocked_categories(category_id)` | `reward_categories(id)` | CASCADE | `reward_obligation_blocked_categories_category_id_fkey` |
| `reward_obligation_blocked_categories(obligation_id)` | `reward_obligations(id)` | CASCADE | `reward_obligation_blocked_categories_obligation_id_fkey` |
| `reward_obligation_blocked_rewards(obligation_id)` | `reward_obligations(id)` | CASCADE | `reward_obligation_blocked_rewards_obligation_id_fkey` |
| `reward_obligation_blocked_rewards(reward_definition_id)` | `reward_definitions(id)` | CASCADE | `reward_obligation_blocked_rewards_reward_definition_id_fkey` |
| `reward_obligations(family_id)` | `families(id)` | NO ACTION | `reward_obligations_family_id_fkey` |
| `reward_obligations(member_id)` | `family_members(id)` | NO ACTION | `reward_obligations_member_id_fkey` |
| `reward_obligations(requirement_id)` | `reward_requirements(id)` | NO ACTION | `reward_obligations_requirement_id_fkey` |
| `reward_obligations(reward_purchase_id)` | `reward_purchases(id)` | CASCADE | `reward_obligations_reward_purchase_id_fkey` |
| `reward_purchases(member_id)` | `family_members(id)` | NO ACTION | `reward_purchases_member_id_fkey` |
| `reward_purchases(point_type_id)` | `point_types(id)` | NO ACTION | `reward_purchases_point_type_id_fkey` |
| `reward_purchases(reward_definition_id)` | `reward_definitions(id)` | NO ACTION | `reward_purchases_reward_definition_id_fkey` |
| `reward_purchases(reward_request_id)` | `reward_requests(id)` | NO ACTION | `reward_purchases_reward_request_id_fkey` |
| `reward_requests(approved_point_type_id)` | `point_types(id)` | NO ACTION | `reward_requests_approved_point_type_id_fkey` |
| `reward_requests(family_id)` | `families(id)` | NO ACTION | `reward_requests_family_id_fkey` |
| `reward_requests(requested_by_member_id)` | `family_members(id)` | NO ACTION | `reward_requests_requested_by_member_id_fkey` |
| `reward_requests(reward_definition_id)` | `reward_definitions(id)` | NO ACTION | `reward_requests_reward_definition_id_fkey` |
| `reward_requirement_blocked_categories(category_id)` | `reward_categories(id)` | CASCADE | `reward_requirement_blocked_categories_category_id_fkey` |
| `reward_requirement_blocked_categories(requirement_id)` | `reward_requirements(id)` | CASCADE | `reward_requirement_blocked_categories_requirement_id_fkey` |
| `reward_requirement_blocked_rewards(requirement_id)` | `reward_requirements(id)` | CASCADE | `reward_requirement_blocked_rewards_requirement_id_fkey` |
| `reward_requirement_blocked_rewards(reward_definition_id)` | `reward_definitions(id)` | CASCADE | `reward_requirement_blocked_rewards_reward_definition_id_fkey` |
| `reward_requirements(family_id)` | `families(id)` | NO ACTION | `reward_requirements_family_id_fkey` |
| `reward_requirements(reward_definition_id)` | `reward_definitions(id)` | CASCADE | `reward_requirements_reward_definition_id_fkey` |
| `reward_requirements(reward_request_id)` | `reward_requests(id)` | CASCADE | `reward_requirements_reward_request_id_fkey` |
| `reward_requirements(task_definition_id)` | `task_definitions(id)` | NO ACTION | `reward_requirements_task_definition_id_fkey` |
| `reward_weekly_windows(reward_definition_id)` | `reward_definitions(id)` | CASCADE | `reward_weekly_windows_reward_definition_id_fkey` |
| `role_assignments(family_id)` | `families(id)` | NO ACTION | `role_assignments_family_id_fkey` |
| `role_assignments(role_definition_id)` | `role_definitions(id)` | NO ACTION | `role_assignments_role_definition_id_fkey` |
| `role_definitions(family_id)` | `families(id)` | NO ACTION | `role_definitions_family_id_fkey` |
| `role_definitions(legacy_group_role_id)` | `group_roles(id)` | NO ACTION | `role_definitions_legacy_group_role_id_fkey` |
| `role_definitions(role_set_id)` | `role_sets(id)` | NO ACTION | `role_definitions_role_set_id_fkey` |
| `role_permission_grants(legacy_group_permission_grant_id)` | `group_permission_grants(id)` | NO ACTION | `role_permission_grants_legacy_group_permission_grant_id_fkey` |
| `role_permission_grants(role_definition_id)` | `role_definitions(id)` | NO ACTION | `role_permission_grants_role_definition_id_fkey` |
| `role_set_bindings(role_set_id)` | `role_sets(id)` | NO ACTION | `role_set_bindings_role_set_id_fkey` |
| `role_sets(family_id)` | `families(id)` | NO ACTION | `role_sets_family_id_fkey` |
| `task_audit_events(task_definition_id)` | `task_definitions(id)` | NO ACTION | `fk_task_audit_definition` |
| `task_audit_events(task_instance_id)` | `task_instances(id)` | NO ACTION | `fk_task_audit_instance` |
| `task_audit_events(performed_by_member_id)` | `family_members(id)` | NO ACTION | `fk_task_audit_performed_by` |
| `task_audit_events(family_id)` | `families(id)` | NO ACTION | `fk_task_audit_workspace` |
| `task_definition_required_group_roles(group_role_id)` | `group_roles(id)` | NO ACTION | `fkb7if5d4py1tn3oo3marogomby` |
| `task_definition_required_group_roles(task_definition_id)` | `task_definitions(id)` | NO ACTION | `fkid4afqqcyntj3ncijeuhrty98` |
| `task_definition_required_role_definitions(role_definition_id)` | `role_definitions(id)` | NO ACTION | `task_definition_required_role_definitio_role_definition_id_fkey` |
| `task_definition_required_role_definitions(task_definition_id)` | `task_definitions(id)` | CASCADE | `task_definition_required_role_definitio_task_definition_id_fkey` |
| `task_definitions(created_by_member_id)` | `family_members(id)` | NO ACTION | `fk5f0gxusc16lv6s2pbu2rjc4y6` |
| `task_definitions(family_id)` | `families(id)` | NO ACTION | `fk7lx9wwrsd38b5270pcclhprpt` |
| `task_definitions(work_node_id)` | `work_nodes(id)` | NO ACTION | `fka4peaeubhwn3rfmvper51pdm1` |
| `task_definitions(assigned_member_id)` | `family_members(id)` | NO ACTION | `fkb60kylyuxbuhfaa7g26btvcfa` |
| `task_definitions(preferred_member_id)` | `family_members(id)` | NO ACTION | `fkhgxyymtvlcg5oepuvcpsks43t` |
| `task_definitions(reward_point_type_id)` | `point_types(id)` | NO ACTION | `fkmjmqm8mabt4gbqw9er1jggnef` |
| `task_definitions(target_group_id)` | `member_groups(id)` | NO ACTION | `fkmtvpx4xfaeuc1n5xvsilay8uj` |
| `task_definitions(penalty_point_type_id)` | `point_types(id)` | NO ACTION | `fknms759lnh61mabt1p9kt0kk34` |
| `task_definitions(responsible_member_id)` | `family_members(id)` | NO ACTION | `fkofyphp4foe7rlybqrv3l7m6lf` |
| `task_delegations(delegated_by_member_id)` | `family_members(id)` | NO ACTION | `fkkrdr49st4vbm4ti1bfp0nhl14` |
| `task_delegations(task_instance_id)` | `task_instances(id)` | NO ACTION | `fknewkkainfy5ere829nnjydpwl` |
| `task_delegations(to_member_id)` | `family_members(id)` | NO ACTION | `fkorsj1rg5qb739e5nrydvt21a6` |
| `task_delegations(from_member_id)` | `family_members(id)` | NO ACTION | `fks8ijqmo4yh04frjex6fsdrrvi` |
| `task_instances(member_id)` | `family_members(id)` | NO ACTION | `fk6ii2rhn7py82qoawfelus847l` |
| `task_instances(task_definition_id)` | `task_definitions(id)` | NO ACTION | `fkdmac0r0uu9cnx7gm3nl7jwb6b` |
| `task_instances(reward_point_type_id)` | `point_types(id)` | NO ACTION | `fkg06sw3sba5tc6r93g4tkk7vhj` |
| `task_instances(penalty_point_type_id)` | `point_types(id)` | NO ACTION | `fkl9xjrto5qqr7s3r0eio8ywbj3` |
| `task_instances(original_assigned_member_id)` | `family_members(id)` | NO ACTION | `fksds80kfge6sgdc0loysr24mmn` |
| `task_participants(task_definition_id)` | `task_definitions(id)` | NO ACTION | `fkmochscglakliqttbxmay4se83` |
| `task_reward_requests(approved_point_type_id)` | `point_types(id)` | NO ACTION | `task_reward_requests_approved_point_type_id_fkey` |
| `task_reward_requests(approved_reward_definition_id)` | `reward_definitions(id)` | NO ACTION | `task_reward_requests_approved_reward_definition_id_fkey` |
| `task_reward_requests(family_id)` | `families(id)` | NO ACTION | `task_reward_requests_family_id_fkey` |
| `task_reward_requests(requested_by_member_id)` | `family_members(id)` | NO ACTION | `task_reward_requests_requested_by_member_id_fkey` |
| `task_reward_requests(requested_point_type_id)` | `point_types(id)` | NO ACTION | `task_reward_requests_requested_point_type_id_fkey` |
| `task_reward_requests(requested_reward_definition_id)` | `reward_definitions(id)` | NO ACTION | `task_reward_requests_requested_reward_definition_id_fkey` |
| `task_reward_requests(task_instance_id)` | `task_instances(id)` | CASCADE | `task_reward_requests_task_instance_id_fkey` |
| `task_schedule_slots(task_definition_id)` | `task_definitions(id)` | CASCADE | `task_schedule_slots_task_definition_id_fkey` |
| `task_suggestions(requested_by_member_id)` | `family_members(id)` | NO ACTION | `task_suggestions_requested_by_member_id_fkey` |
| `task_suggestions(reviewed_by_member_id)` | `family_members(id)` | NO ACTION | `task_suggestions_reviewed_by_member_id_fkey` |
| `task_suggestions(workspace_id)` | `families(id)` | NO ACTION | `task_suggestions_workspace_id_fkey` |
| `template_activation_requests(template_id)` | `workspace_private_templates(id)` | CASCADE | `template_activation_requests_template_id_fkey` |
| `template_activation_requests(workspace_id)` | `families(id)` | CASCADE | `template_activation_requests_workspace_id_fkey` |
| `temporary_child_sessions(child_member_id)` | `family_members(id)` | NO ACTION | `temporary_child_sessions_child_member_id_fkey` |
| `temporary_child_sessions(device_id)` | `managed_devices(id)` | NO ACTION | `temporary_child_sessions_device_id_fkey` |
| `temporary_child_sessions(ended_by_member_id)` | `family_members(id)` | NO ACTION | `temporary_child_sessions_ended_by_member_id_fkey` |
| `temporary_child_sessions(started_by_member_id)` | `family_members(id)` | NO ACTION | `temporary_child_sessions_started_by_member_id_fkey` |
| `user_accounts(family_member_id)` | `family_members(id)` | NO ACTION | `fk4hhslaywin09sjvgifa1oevps` |
| `work_nodes(family_id)` | `families(id)` | NO ACTION | `fk97xpcf2m4bo1p6j6ui8mnyhmy` |
| `work_nodes(parent_node_id)` | `work_nodes(id)` | NO ACTION | `fkit24fgimwnoowpkresk40p196` |
| `workspace_invitation_redemptions(account_id)` | `user_accounts(id)` | NO ACTION | `workspace_invitation_redemptions_account_id_fkey` |
| `workspace_invitation_redemptions(invitation_id)` | `workspace_invitations(id)` | NO ACTION | `workspace_invitation_redemptions_invitation_id_fkey` |
| `workspace_invitation_redemptions(membership_id)` | `account_workspace_memberships(id)` | NO ACTION | `workspace_invitation_redemptions_membership_id_fkey` |
| `workspace_invitations(created_by_account_id)` | `user_accounts(id)` | NO ACTION | `workspace_invitations_created_by_account_id_fkey` |
| `workspace_invitations(member_role_id)` | `family_roles(id)` | NO ACTION | `workspace_invitations_member_role_id_fkey` |
| `workspace_invitations(workspace_id)` | `families(id)` | NO ACTION | `workspace_invitations_workspace_id_fkey` |
| `workspace_join_codes(workspace_id)` | `families(id)` | NO ACTION | `workspace_join_codes_workspace_id_fkey` |
| `workspace_join_requests(requester_account_id)` | `user_accounts(id)` | NO ACTION | `workspace_join_requests_requester_account_id_fkey` |
| `workspace_join_requests(reviewed_by_account_id)` | `user_accounts(id)` | NO ACTION | `workspace_join_requests_reviewed_by_account_id_fkey` |
| `workspace_join_requests(workspace_id)` | `families(id)` | NO ACTION | `workspace_join_requests_workspace_id_fkey` |
| `workspace_private_templates(workspace_id)` | `families(id)` | CASCADE | `workspace_private_templates_workspace_id_fkey` |
| `workspace_reset_challenges(workspace_id,operation_id)` | `workspace_reset_operations(workspace_id,operation_id)` | RESTRICT | `fk_reset_challenge_operation` |
| `workspace_reset_operations(requested_by_member_id)` | `family_members(id)` | RESTRICT | `workspace_reset_operations_requested_by_member_id_fkey` |
| `workspace_reset_operations(workspace_id)` | `families(id)` | RESTRICT | `workspace_reset_operations_workspace_id_fkey` |
| `workspace_reset_reauth_limits(account_id)` | `user_accounts(id)` | RESTRICT | `workspace_reset_reauth_limits_account_id_fkey` |
| `workspace_reset_reauth_limits(workspace_id)` | `families(id)` | RESTRICT | `workspace_reset_reauth_limits_workspace_id_fkey` |

## Acceptance still required

Architecture/security: explicit owner/delegation + global-account manager policy, retained identity/access baseline and category/financial retention decisions. Backend: checked scoped closure/semantic cleanup, approved proof transport/fresh Google reauth, idempotent execute/audit/epoch/onboarding, fault injection and no-orphan/ledger consistency. QA: use disposable PostgreSQL then explicitly designated disposable DEV fixtures; Test14/Test2-11 are not destructive test targets. Web/Android: compatible confirmation/draft/queue/onboarding; Release: independent migration/deploy/backup/runtime gate.

No live data or migration was executed. Full reset remains unavailable.

## Disposable schema/test evidence — 2026-10-31
Exact tested SHA `b4f62fc9bd13b9c9aec223619f016bac2f7ebcc7`; [GitHub Actions 41249950772](https://github.com/o-kobylianskyi/family-points/actions/runs/41249950772) all four jobs SUCCESS. Backend: 329 tests, zero failures/errors/skips; PointMutationTransactionPostgresTest: 54 PASS. WorkspaceResetProofUpgradePostgresTest: 1 PASS, V44->V45 preservation/constraints/repeat-migrate no-op.
The emitted pg_catalog table/FK metadata was checked against this document:70 tables/156 edges exactly match. Source metadata contains no tenant records. New V45 bindings are additive and preserve old rows as unbound, not executable proof; source/test does not grant live migration approval. See XTR-20261031-B-01 for accountable policy decisions.
