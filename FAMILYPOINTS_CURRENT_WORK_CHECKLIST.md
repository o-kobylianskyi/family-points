# FamilyPoints — Current Development Checklist

> Working branch: `feature/task-soft-delete`
>
> Purpose: current implementation checkpoint and immediate development order.
>
> This file is not the full product backlog. Product/domain decisions belong in `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`.

Last reviewed against GitHub: **2026-10-04**

---

# 1. Current checkpoint: Roles / Role Sets / Context model

## 1.1 New role catalog foundation

- [x] Flyway `V7__hierarchical_role_catalog.sql`
- [x] `RoleDefinition` entity
- [x] `RoleAssignment` entity
- [x] `RoleSetBinding` entity
- [x] `RoleVisibility`
- [x] `RoleContextType`
- [x] Role repositories exist
- [x] `RoleSet` extended with:
  - [x] `systemCode`
  - [x] `visibility`
  - [x] owner context
  - [x] `systemDefault`
- [x] Default reusable role sets:
  - [x] MANAGEMENT
  - [x] EXECUTION
  - [x] CONTROL
- [x] Default role definitions:
  - [x] LEADER
  - [x] DEPUTY
  - [x] SENIOR
  - [x] EXECUTOR
  - [x] ASSISTANT
  - [x] REVIEWER
  - [x] OBSERVER
- [x] `RoleCatalogBootstrapService` exists
- [x] Default role catalog initialization for new workspaces is present in current code

## 1.2 Important coexistence / legacy state

- [x] Legacy/intermediate `GroupRole` still exists
- [x] Existing `GroupPermissionGrant` flow still works through group roles
- [x] Existing GroupEditor still edits `GroupRole`
- [x] Existing member-role assignment in GroupEditor still uses legacy group role IDs
- [x] Existing role-set UI still lives inside GroupEditor
- [x] Separate `WorkspaceRole` security-role model also exists and must not be confused with functional `RoleDefinition`

**INVARIANT**

Do not merge these concepts:

- Workspace security role / workspace permission
- functional RoleDefinition
- contextual RoleAssignment
- legacy GroupRole

The new model must replace legacy functional-role usage incrementally, without breaking workspace authorization.

---

# 2. Role refactor — immediate remaining work

## 2.1 Verify database/migration state first

- [ ] Run/verify V7 on the current existing DB
- [ ] Verify fresh DB bootstrap through all Flyway migrations
- [ ] Verify default RoleSets/RoleDefinitions are created exactly once
- [ ] Verify creation of a new Workspace initializes defaults
- [ ] Verify existing workspaces receive/may receive default catalog safely
- [ ] Check duplicate-name/system-code behavior
- [ ] Check delete/deactivate behavior for RoleSet/RoleDefinition

Do not call this compile-/migration-tested until it is actually run.

## 2.2 Legacy data migration

- [x] Define mapping from existing `GroupRole` to new `RoleDefinition` via `legacy_group_role_id`
- [x] Preserve existing role names/descriptions in V8 bridge migration
- [x] Preserve existing role-set membership in V8 bridge migration
- [x] Preserve existing member-role assignments in V8 bridge migration
- [x] Preserve existing permission grants via V9 bridge migration
- [ ] Decide which migrated roles become:
  - [ ] PRIVATE
  - [ ] SHARED
- [x] Define owner context for migrated group-local roles as PRIVATE + MEMBER_GROUP
- [x] Create V8 migration bridge without deleting legacy tables
- [x] Add temporary dual-write bridge for legacy GroupRole/member-role edits
- [ ] Smoke-test V8 + dual-write against current real DB
- [ ] Remove legacy only in a later separate migration

## 2.3 Role catalog service/API

- [x] Add/finish `RoleDefinitionService`
- [x] Add RoleDefinition list API
- [x] Add create API
- [x] Add update API
- [x] Add deactivate/delete semantics
- [x] Support role set association
- [x] Support `PRIVATE / SHARED` in backend model
- [x] Support owner context
- [x] Protect system-default RoleDefinitions/RoleSets from edit/delete
- [x] Add localization/fallback-name handling for predefined roles in frontend
- [ ] Smoke-test RoleDefinition/RoleSet catalog API locally

## 2.4 RoleSetBinding

- [ ] Add service for `RoleSetBinding`
- [ ] Bind reusable RoleSet to Workspace/Group/Task context
- [ ] Unbind safely
- [ ] Prevent duplicate bindings
- [ ] Define visibility rules for bound sets
- [ ] Binding must NOT auto-assign roles to actors

## 2.5 RoleAssignment

- [ ] Add RoleAssignment service
- [ ] MEMBER actor assignment
- [ ] GROUP actor assignment where valid
- [ ] Context-aware assignment
- [ ] Active/inactive or validity semantics
- [ ] Prevent duplicate active assignment
- [ ] Validate role is visible/available in target context
- [ ] Assignment in child context must not grant parent access

## 2.6 Scope evaluator / context hierarchy

- [ ] Implement/commonize `ScopeEvaluator` / `ContextHierarchyService`
- [ ] CURRENT scope
- [ ] SUBTREE scope
- [ ] descendant allow
- [ ] parent deny
- [ ] sibling deny
- [ ] multiple-parent group graph support
- [ ] visited-set/cycle-safe traversal
- [ ] hidden parent metadata protection

---

# 3. Permissions integration

## 3.1 Functional role vs security permission

- [x] Add `RolePermissionGrant` targeting new RoleDefinition
- [x] Add `RolePermissionScope = CURRENT | SUBTREE`
- [x] Add temporary dual-write for legacy GroupPermissionGrant create/delete
- [ ] Smoke-test V9 + permission dual-write against current real DB
- [ ] Do not automatically grant permissions just because a custom functional role exists
- [ ] Keep explicit grants/presets
- [ ] Preserve workspace-global permissions separately
- [ ] Preserve group/task scoped permissions

## 3.2 Authorization acceptance tests

- [ ] Child-context role → parent: DENY
- [ ] SUBTREE grant → descendant: ALLOW
- [ ] SUBTREE grant → sibling: DENY
- [ ] PRIVATE role → global catalog: HIDDEN
- [ ] SHARED role → owner context + descendants: VISIBLE
- [ ] SHARED role → ancestors/siblings: HIDDEN
- [ ] readable child with hidden parent does not leak parent metadata
- [ ] seeing parent alone does not imply access to child
- [ ] local custom role cannot cause privilege escalation

---

# 4. Role UI refactor

## 4.1 Workspace-level catalog

- [x] Add Workspace settings section **“Ролі та набори”**
- [x] List predefined/shared RoleSets
- [x] List RoleDefinitions grouped by RoleSet
- [x] Create/edit custom SHARED RoleSet where authorized
- [x] Create/edit custom SHARED RoleDefinition where authorized
- [x] Show predefined roles translated to current interface language via stable systemCode
- [x] Manage role descriptions
- [ ] Manage localization metadata for custom shared roles
- [ ] Manage permission presets/grants where allowed
- [x] Clear distinction between system/default and shared custom roles in Workspace settings
- [ ] Local PRIVATE role UI stays for contextual Group/Task editor
- [ ] Prepare catalog to be seeded by Workspace/activity templates (Family/Office/Business/etc.)

## 4.2 Simplify GroupEditor

Current state: GroupEditor still contains role-set manager, role editor and permission editor.

Target:

- [ ] Remove global catalog administration from GroupEditor
- [ ] Keep contextual role assignment only
- [ ] Select/bind available shared RoleSets for group context
- [ ] Assign available standard/shared roles to members
- [ ] Allow local PRIVATE role creation where authorized
- [ ] Local PRIVATE role remains visible only in owner context / explicitly allowed descendants
- [ ] Local role creation cannot grant permissions outside creator delegation scope
- [ ] Support scenario: create group → add available members → assign senior/leader → add group-specific custom role
- [ ] Keep local permission configuration only where model explicitly permits it
- [ ] Compact grouped multi-select by RoleSet
- [ ] Avoid redundant `onChanged()+reloadAux` requests

## 4.3 Task role UI

- [ ] Reuse contextual role assignment model on Task Card where appropriate
- [ ] ADMIN/OBSERVER/EXECUTOR participant semantics remain separate from functional roles
- [ ] Role assignment must not silently change current executor

---

# 5. Group lifecycle follow-up

After role refactor is stable:

- [ ] Expose `MemberGroupType = ORGANIZATIONAL | TEAM` fully in API
- [ ] Expose type in UI
- [ ] Expose `validFrom`
- [ ] Expose `validUntil`
- [ ] Define effective-date behavior
- [ ] Expired temporary team → archive/deactivate, not physical delete
- [ ] Verify multiple-parent composition
- [ ] Verify team-local leadership does not leak into source departments

---

# 6. Then return to Task domain

## 6.1 Assignment / eligibility

- [ ] Finish Actor model use for MEMBER/GROUP
- [ ] `assignedActor`
- [ ] `preferredActor`
- [ ] `claimedByActor`
- [ ] `currentExecutor`
- [ ] role-based eligibility ANY/ALL
- [ ] SINGLE_MEMBER
- [ ] GROUP_SHARED
- [ ] OPEN_GROUP
- [ ] OPEN_WORKSPACE
- [ ] PREFERRED_MEMBER
- [ ] fix RELEASED instance/member/list semantics
- [ ] verify `TaskInstanceResponse` exposes required assignment fields

## 6.2 Requirements

- [ ] `ALL_SATISFIED`
- [ ] `MINIMUM_SATISFIED`
- [ ] `requiredCount`
- [ ] `alwaysRequired`
- [ ] same-occurrence recurring dependency
- [ ] generic requirement foundation

## 6.3 Calendar / future tasks

- [ ] day/week/month/period
- [ ] future task visibility
- [ ] visibility != availability != eligibility
- [ ] reminder != deadline
- [ ] defer/decline
- [ ] extension/review
- [ ] overdue later

---

# 7. Rich reward/task mechanics after core task model

- [ ] Quality rating
- [ ] FIXED
- [ ] QUALITY_PERCENTAGE
- [ ] BASE_PLUS_QUALITY_BONUS
- [ ] MILESTONE
- [ ] TaskStep/TaskMilestone
- [ ] PAY_IMMEDIATELY
- [ ] PAY_ON_TASK_COMPLETION
- [ ] configurable stars
- [ ] RewardGroup
- [ ] random/manual selection semantics
- [ ] task reputation reward/penalty
- [ ] claimable task bank
- [ ] TaskRequest / child task requests

---

# 8. Reputation domain

Concept recovered and logged; implementation is later.

- [ ] Reputation transaction/history model
- [ ] task reputation delta
- [ ] behavior reputation delta
- [ ] manual award/penalty
- [ ] scoped permission to award reputation
- [ ] group/admin allocation limits
- [ ] decay/reset policy
- [ ] excused-day interaction
- [ ] reward/catalog minimum reputation condition
- [ ] reputation is checked, NOT spent on purchase

---

# 9. Purchase / Reward eligibility

- [ ] Generic condition engine reusable beyond tasks
- [ ] minimum reputation
- [ ] required task/task-set completion
- [ ] M-of-N conditions
- [ ] no serious BehaviorEvent within period
- [ ] streak requirement
- [ ] learning/achievement requirement
- [ ] point price remains separate from qualification conditions
- [ ] locked-item progress explanation in UI
- [ ] optional Goal/target feature later

---

# 10. Later domains

## Behavior
- [ ] BehaviorRule
- [ ] BehaviorEvent
- [ ] BehaviorDailyReview
- [ ] rewards/penalties
- [ ] reputation integration
- [ ] self-report approval
- [ ] severity model

## Streaks / achievements
- [ ] streak calculation
- [ ] reset/freeze
- [ ] excused days
- [ ] rewards
- [ ] achievements

## Learning Bank
- [ ] LearningActivity/Question/Attempt
- [ ] Practice/Challenge
- [ ] generators
- [ ] automatic checking
- [ ] difficulty/adaptive difficulty
- [ ] anti-farming
- [ ] reading/book/chapter milestones
- [ ] learning plans

## Device/App control
- [ ] app price/time packages
- [ ] schedules/limits
- [ ] partial-use policy
- [ ] Android enforcement
- [ ] Windows agent
- [ ] iOS later

## Templates
- [ ] WorkspaceTemplate foundation
- [ ] built-in activity templates such as Family / Office-Small Team / Business / Software Development / Household
- [ ] declarative seed of RoleSets/RoleDefinitions/bindings
- [ ] declarative seed of groups/work hierarchy/permissions/economy
- [ ] predefined role localization through systemCode
- [ ] full wizard later

---

# 11. Immediate next action

**Continue role/context refactor before starting a new major domain.**

Recommended next sequence:

1. smoke-test V9/V10 + new Role Catalog settings UI;
2. fix/simplify GroupEditor to contextual assignment + local PRIVATE roles;
3. finish RoleAssignment/RoleSetBinding services and APIs;
4. implement ScopeEvaluator + authorization tests;
5. switch authorization off legacy GroupRole;
6. only then proceed to group lifecycle and Task assignment/requirements.

---

# 12. Safety rule

Before each substantial implementation batch:

- inspect current Git source;
- check `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`;
- check `FAMILYPOINTS_CONCEPT_CHECKLIST.md`;
- do not implement a `RECOVER` item by guessing;
- update this checklist as items become verified/implemented.
