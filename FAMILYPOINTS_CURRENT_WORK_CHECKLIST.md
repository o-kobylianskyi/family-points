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

- [x] Remove RoleSet administration from normal GroupEditor flow
- [x] Keep contextual/local role assignment in GroupEditor
- [x] Allow local role creation without requiring a RoleSet
- [x] Add simple permission profiles for local roles
- [x] Hide raw permission matrix behind “Advanced permissions”
- [x] Add local role delete flow with cleanup
- [ ] Assign shared/system RoleDefinitions directly in GroupEditor later if real usage requires it
- [ ] Enforce creator delegation ceiling for local advanced permissions when new authorization model is switched on
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

1. smoke-test simplified local-role editor and local role deletion;
2. stop role UX work unless real usage exposes a concrete problem;
3. finish RoleAssignment/RoleSetBinding services and APIs;
4. implement ScopeEvaluator + authorization tests;
5. switch authorization off legacy GroupRole;
6. then proceed to group lifecycle and Task assignment/requirements.

---

# 12. Safety rule

Before each substantial implementation batch:

- inspect current Git source;
- check `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`;
- check `FAMILYPOINTS_CONCEPT_CHECKLIST.md`;
- do not implement a `RECOVER` item by guessing;
- update this checklist as items become verified/implemented.


## Automatic naming / duplicate UX
- [x] Auto-name new local roles from permission profile
- [x] Append numeric suffix for generated role-name collisions
- [x] Use “modified” suffix when advanced permissions diverge from the base profile
- [x] Stop auto-renaming after manual role-name edit
- [x] Reject duplicate local role names within the group
- [x] Warn when another local role has identical permission+scope set
- [x] Auto-name new tasks with localized “Task/Завдання + first free number”
- [x] Reject duplicate task titles in the current task catalog


## Global-role assignment correction
- [x] Stop creating/using legacy GroupRole copies for new predefined-role usage
- [x] Expose shared Workspace RoleDefinitions as automatically available in every group
- [x] Use RoleAssignment for member ↔ global role ↔ group context
- [x] Keep RoleSetBinding/reference semantics; do not clone RoleDefinitions into groups
- [x] Create PRIVATE RoleDefinition only for genuinely custom group roles
- [x] Update GroupEditor role picker to show global predefined roles by default
- [x] Rename normal UX to “Create custom role”
- [x] Reuse V8-migrated legacy assignments and switch current group response to RoleAssignment
- [x] Switch task required-role eligibility to RoleDefinition
- [x] Switch scoped task authorization to RoleAssignment + RolePermissionGrant
- [x] Add V11 migration for task-role bridge + system role permission presets
- [ ] Smoke-test V11 migration and group/task flows locally


## Task participant UI simplification
- [x] Remove duplicate assignment controls from CreateTaskModal
- [x] Keep Participants as the visible actor source
- [x] Restrict ADMIN and OBSERVER to MEMBER actors
- [x] Allow combined MEMBER + GROUP executors
- [x] Require at least one executor before save
- [x] Add PARTICIPANTS assignment policy for explicit executor sets
- [x] Eligibility for PARTICIPANTS uses EXECUTOR TaskParticipants
- [ ] Smoke-test combined executor task creation and claim visibility
- [ ] Design true joint/multi-actor TaskInstance execution only if required by real usage


## Reputation — task slice
- [x] Add dedicated reputation ledger
- [x] Add task reward reputation
- [x] Add task penalty reputation
- [x] Snapshot reputation values on TaskInstance
- [x] Award reputation on task completion
- [x] Deduct reputation on missed mandatory task
- [x] Add reputation fields to Create/Edit Task UI
- [x] Expose reputation values in task DTOs
- [ ] Add reputation balance/history UI
- [ ] Add manual reputation bonus/penalty with permission budgets
- [ ] Add behavior reputation sources
- [ ] Add reputation decay/reset policy
- [ ] Add reward eligibility minimum-reputation conditions


## Quick task economy presets
- [x] Default new task to SIMPLE economy
- [x] Add SIMPLE / NORMAL / HARD / CUSTOM presets
- [x] Add common-value dropdowns for points
- [x] Add common-value dropdowns for reputation
- [x] Add “Other…” manual amount entry
- [x] Use step 10 for custom point values
- [x] Use step 1 for custom reputation values
- [x] Switch preset to CUSTOM after manual value change
- [ ] Smoke-test create/edit task economy presets locally


## Rewards v1
- [x] Add reward catalog persistence
- [x] Add reward purchases and REWARD_PURCHASE ledger source
- [x] Allow zero-price conditional rewards
- [x] Add many-to-many reward categories
- [x] Add DIRECT / REQUEST / DIRECT_OR_REQUEST acquisition modes
- [x] Add free-form and catalog-based RewardRequest
- [x] Allow parent approval to override price / point type / minimum reputation
- [x] Add BEFORE_REWARD / AFTER_REWARD requirements
- [x] Implement TASK_COMPLETED + TODAY for preconditions
- [x] Implement SINCE_REWARD for advance task obligations
- [x] Create RewardObligation after advance reward grant
- [x] Auto-complete task-backed obligations after required task completion
- [x] Add NONE / WARN_ONLY / ALL_REWARDS / CATEGORIES / SPECIFIC_REWARDS blocking
- [x] Add category blocking selection
- [x] Add manually selected reward blocking
- [x] Add initial /rewards catalog + requests + management UI
- [x] Show open advance obligations
- [ ] Implement Behavior module and NO_NEGATIVE_BEHAVIOR evaluation
- [ ] Add availability schedule for recurring reward access
- [ ] Add daily / weekly purchase limits
- [ ] Add generalized RewardGroup and RequirementGroup semantics
- [ ] Add full redeem / cancel / refund / approval lifecycle
- [ ] Add Goal conversion for large reward requests
- [ ] Integrate time rewards with app/screen-time control
- [ ] Complete rewards UI i18n
- [ ] Run backend/frontend smoke test locally


## Task reward negotiation
- [x] Add TaskRewardRequest persistence and lifecycle
- [x] Scope reward negotiation to TaskInstance
- [x] Allow executor to request point/reputation changes
- [x] Allow executor to request a catalog reward
- [x] Allow executor to request a free-form reward
- [x] Allow parent/manager to edit requested terms before approval
- [x] Make approved negotiation replace normal TaskInstance reward
- [x] Create zero-price RewardRequest for approved non-point reward after task completion
- [x] Add executor “Запросити іншу винагороду” UI
- [x] Add parent management queue for pending reward negotiations
- [ ] Add request-status badge to task cards
- [ ] Add counter-offer / negotiation conversation history
- [ ] Add notifications
- [ ] Add WAIT_FOR_DECISION task-start policy
- [ ] Add negotiation audit events
- [ ] Smoke-test Task Reward Negotiation locally


## Time-based rewards
- [x] Add STANDARD / TIME_BASED RewardKind
- [x] Add optional catalog defaultDurationMinutes
- [x] Snapshot duration on RewardRequest and RewardPurchase
- [x] Add requested / approved duration to TaskRewardRequest
- [x] Add duration selector to task reward negotiation
- [x] Default negotiated duration to “Будь-який”
- [x] Prefill negotiation point/reputation fields from current TaskInstance snapshot
- [x] Add reward kind and typical duration to reward creation UI
- [ ] Add duration editing to all free-form reward request flows
- [ ] Integrate purchased time rewards with app/screen-time timers
- [ ] Smoke-test V17 and time-based reward flow locally


## Android app
- [x] Add native Android project under /android
- [x] Add INTERNET permission
- [x] Add Retrofit / OkHttp networking foundation
- [x] Add native Compose Login screen
- [x] Authenticate against /auth/login
- [x] Persist JWT access token locally
- [x] Add minimal authenticated Home screen and logout
- [x] Add native Dashboard screen matching web data flow
- [ ] Load /api/me after login and restore
- [x] Add native navigation shell
- [x] Add native Tasks list screen (my/open/management)
- [x] Add native Task Details screen (execution/actions/delegation/subtasks/participants/history)
- [x] Add native Create Task screen (participants/recurrence/economy/subtasks)
- [x] Add native Task Reward Negotiation screen (points/reputation/catalog/time/custom request)
- [x] Add native Rewards screen (catalog/requests/manage/conditions/obligations/purchases)
- [ ] Add balances/dashboard
- [ ] Add Android app-control/time-control layer

- [x] Add native Points screen (members/balance/history/manual operations)

## Android Members — 2026-10-10 initial native port

- [x] Add Members API/repository and native navigation destination
- [x] Members/Groups mobile tabs and shared AppHeader
- [x] Load members, workspace roles and member groups
- [x] Permission-gated add/edit/delete member dialog and confirmation
- [x] Create group and render nested group tree with cycle guard
- [ ] Port full GroupEditor: group edit, composition, role assignments, balances, permissions, lifecycle
- [ ] Validate Members API DTO shape and permission edge cases against running backend
- [ ] Verify Kotlin compile and Android runtime locally (not run by assistant)

## Android Members / GroupEditor follow-up — 2026-10-10

- [x] Add group rename/description/showInNavigation editing UI
- [x] Add/remove group members and child groups
- [x] Assign existing available group roles to members
- [ ] Implement creating/editing/deleting PRIVATE group roles and permission profiles/grants
- [ ] Implement group points operations and full i18n of system role names
- [ ] Check cycle validation and group assignment permissions against backend
- [ ] Compile and runtime smoke-test new GroupEditor on emulator

## Android GroupEditor — local roles follow-up

- [x] Implement create/edit/delete dialog for PRIVATE local roles, with duplicate title check
- [x] Preserve shared/system role read-only behavior
- [ ] Port role permission presets NONE/EXECUTOR/SENIOR/LEADER/CONTROL and advanced grants
- [ ] Port automatic role naming and equivalent-permission warnings
- [ ] Port group balance operations
- [ ] Compile and smoke-test Android Members/GroupEditor locally

## Android GroupEditor permission profiles — 2026-10-10

- [x] Add role permission grant fetch/create/delete API
- [x] Expose NONE/EXECUTOR/SENIOR/LEADER/CONTROL profiles
- [x] Add advanced permission checklist and GROUP/GROUP_SUBTREE scope choice
- [ ] Finish creation workflow applying selected profile immediately to new role
- [ ] Auto-name local roles and show duplicate-permissions warning
- [ ] Complete group balances UI
- [ ] Compile and smoke-test role permissions on emulator
