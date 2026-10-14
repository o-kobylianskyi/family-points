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

## Android local role follow-up — 2026-10-10

- [x] Propose first unused localized role name and stop renaming after manual edit
- [x] Warn on duplicate predefined permission profiles and reject duplicate names
- [x] Apply selected permission preset on role save with explicit role ID resolution
- [ ] Support modified-name suffix for manual advanced permission divergence
- [ ] Test multi-request role save failures, role IDs and permission sync
- [ ] Port group balance controls
- [ ] Run Android compile/runtime validation

## Android group balances — 2026-10-10

- [x] Show all server-provided group balances in GroupEditor by point type
- [ ] Confirm backend DTO for POST /member-groups/{groupId}/points and group delegation rights
- [ ] Add secure group point adjustment form after DTO verification
- [ ] Verify Android compile and group UI on emulator

## Android Group Points transactions — 2026-10-10

- [x] Verify backend GroupPointOperationRequest and PointTransactionType
- [x] Enforce MANAGE_POINTS/ADMIN_OVERRIDE and workspace scope on group points controller endpoint
- [x] Add Android credit/debit form for existing group point types
- [x] Reject zero/invalid amount in Android and display insufficient balance warning
- [ ] Consider backend overdraft policy and atomic balance validation
- [ ] Compile backend with Maven and Android with Gradle, deploy secured backend
- [ ] Runtime-test role permissions and credit/debit API; verify ledger and balance refresh

## 2026-10-11 — Android Groups UI polish
- [x] Compact group cards with localized member and subgroup counters
- [x] Expand/collapse tree and indent nested groups
- [x] Suppress zero balances in group overview; retain full balance list in editor
- [x] Translate permission groups, role profiles, scope selection and member types
- [ ] Compile Kotlin and verify layout on actual device/emulator, especially deep group nesting

## 2026-10-11 — Android Settings
- [x] Replace Settings placeholder with native screen
- [x] Allow own password change with basic validation
- [x] Load accounts (for managers), create account, update login status and username, reset password
- [x] Localized new controls in UK/DE/EN/RU
- [ ] Port role catalog/role sets editor from React
- [ ] Compile Kotlin and smoke-test Settings API and 204 password responses
- [ ] Improve account form failure handling and permission visibility

## 2026-10-11 — Android Settings role catalog
- [x] Read workspace role sets and definitions
- [x] Create/edit/delete custom sets and non-system shared roles
- [x] Support role-to-set assignment and localized action labels
- [x] Mount role catalog within native Settings
- [ ] Translate predefined system role names from systemCode
- [ ] Confirm role CRUD API response compatibility and permissions
- [ ] Verify Android Kotlin compile and runtime on emulator

## 2026-10-11 — Localized custom currency declensions
- [x] Store per-point-type forms one/few/many for UK/RU/EN/DE (Flyway V18)
- [x] Secure read/update API for name forms
- [x] Native Android Settings editor with quantity preview
- [x] Shared Android quantity formatter for custom forms and POINTS fallback
- [x] Apply configured forms to Android reward catalog and request prices
- [ ] Populate recommended defaults for built-in COPPER/SILVER/GOLD, and expose forms in React Settings
- [ ] Use formatter consistently in balances, transactions, tasks, groups and other currency displays
- [ ] Build/test Flyway migration, backend API, Android Settings + reward prices

## 2026-10-11 — Currency UI audit, Android + React
- [x] Inventory screens/components showing currency counts in both clients
- [x] Shared UI currency formatting helper for React; Android counterpart exists
- [x] Web Settings currency form editor + API; Rewards/Tasks/TaskDetails/Group/Dashboard/Points displays
- [x] Android Dashboard/Points basic POINTS summary + TaskDetails; Rewards custom forms
- [ ] Android custom-form loading in all screens (not just Rewards); groups/history/negotiation/task-list/editor
- [ ] Web point history and remaining forms; full localization and currency defaults
- [ ] Backend V18 migration tests; Kotlin compile, JS build and live regression checks

## Web Settings currency editor layout
- [x] Separate currency editor layout from password settings grid
- [x] Localize system currency selector labels
- [x] Show persisted forms and distinguish them from unsaved built-in examples
- [x] Responsive field arrangement and quantity preview
- [ ] npm build and browser smoke test; verify GET and PUT of persisted forms

## 2026-10-11 — Stabilization and Android app-control
- [x] Review build configuration, migration version uniqueness and selected high-risk backend/Kotlin paths
- [ ] Obtain verified backend Maven test/compile output, frontend npm build output, Android Gradle compile output
- [ ] Finish custom currency forms propagation on Android (Tasks, Groups, Points history, negotiation)
- [ ] Inspect other UI translations / layout defects and report remaining cases by screen
- [x] Add permission-gated, read-only Android UsageStats data access foundation
- [ ] Android Usage Access settings handoff and app-usage inventory UI
- [ ] Design server device-to-member binding and app-time purchase policy
- [ ] Determine permissible kiosk/device-owner/accessibility approaches before claiming app lock or force-close ability

## Device management implementation (phase 1+)
- [x] V19 schema for managed devices, app inventory, per-app policies, temporary child sessions and parent override audit
- [ ] Maven/Flyway migration test on PostgreSQL
- [ ] Device registration API with ownership, workspace validation and server-side permissions
- [ ] Android device ID enrollment and installed-app inventory upload
- [ ] Web device/app catalog with category, visibility, access mode and pricing
- [ ] Temporary child session start/end endpoints with parent authorization, idempotency and cross-workspace validation
- [ ] Protected local parent PIN, timeout, rate limiting and override recovery
- [ ] Standard/Device Owner capability negotiation and local policy enforcement
- [ ] Incoming call screening, allowlist and approval requests; outbound restrictions need separate feasibility test
- [ ] YouTube/browser content rules (depending on achievable enforcement level)

## Offline-first child device (2026-10-11)
- [x] Isolate balance loading from history timeout in Android PointsRepository (commit 87bfa95)
- [ ] Room local database: device/user scoped snapshots for balances, point types + inflections, reward catalog + prices + requirements, app policies, allowed contacts, active time entitlements, synchronization state
- [ ] Encrypted-at-rest local sensitive data using Android Keystore-backed keys, device binding and careful logout/account-switch behavior
- [ ] Server-issued offline spending allowance/reservations (budget + expiry + signed device binding); **do not** treat last-seen server balance as automatically spendable across devices
- [ ] Durable idempotent offline action queue with UUID client operation id, append-only local ledger, retry/reconciliation and transaction uniqueness server-side
- [ ] Freshness/stale indicators, last successful sync and conservative enforcement when policy/allowance expires
- [ ] Test airplane mode, concurrent phone/tablet usage, revoked rewards, clock changes, reboot during active time and reconnect after long offline intervals
- [ ] Cache read-path for balance/reward catalog with UI showing explicitly offline/stale values

- [x] Wire persisted display-only balance cache into Android repository and UI with offline status, user fallback, and disabled offline operations
- [ ] Verify Kotlin compilation and emulator online/airplane-mode navigation after first successful balance sync
- [ ] Implement offline session hydration and offline reward catalog/policy cache

## Android shared server reachability
- [x] Replace connectivity-only Settings logic with shared API backend status
- [x] Add central OkHttp status propagation, bounded backend probe, retries and global banner
- [x] Preserve active session on transient gateway/unreachable refresh failures
- [x] Repair Settings syntax introduced in offline mode changes
- [ ] Verify :app:compileDebugKotlin and emulator offline->online recovery
- [ ] Test server unavailable while Wi-Fi remains connected; verify Settings controls hide and return
- [ ] Separate per-screen transient operation errors from global backend outages in remaining screens

## 2026-10-11 — Offline cache coverage audit
- [x] Rewards: split essential catalog GET from seven-request aggregate; save server catalog immediately
- [x] Rewards: show cached catalog before/during refresh, prevent offline purchases/requests
- [x] Dashboard: date-scoped offline read-only snapshot
- [x] Tasks (My tab): date and account-scoped read-only offline snapshot; disable mutations
- [ ] Run Android Kotlin compile and online-then-airplane-mode smoke tests on emulator
- [ ] Encrypt local rewards/tasks/dashboard snapshots and enforce per-account cache retention/logout policy
- [ ] Offline local PIN authorization so snapshots can be opened after process restart/token expiry
- [ ] Cache point type declensions, members/groups, role catalog and policy rules
- [ ] Cache reward detail forms where applicable; task management/details still online-only
- [ ] Verify all requests are refreshed after server reconnect and stale data never allows mutations

## Offline password login (2026-10-11)
- [x] Add encrypted per-username profile and salted password verifier
- [x] Allow normal login button to work offline for enrolled accounts
- [x] Preserve enrollment across manual logout; require password every session
- [x] Add 7-day offline credential validity and rate limiting after wrong passwords
- [x] Differentiate offline access errors from online incorrect credentials
- [x] Prevent offline authenticated identity from reusing old JWT; require online sign-in after reconnect
- [x] Guard PointsScreen network requests when offline identity has no JWT
- [ ] Verify Android compilation and 6-step emulator test (online enrollment -> logout -> server down -> offline login -> cached screens -> server reconnect)
- [ ] Add offline account chooser, manage/remove saved accounts and locally settable parent PIN
- [ ] Encrypt remaining reward/task/dashboard/points caches and define device-removal policy

## 2026-10-11 — Backend reconnect regression
- [x] Make health check identify actual FamilyPoints backend (new /api/client-health)
- [x] Prevent false ONLINE state from generic HTTP 200/401/403
- [x] Preserve offline banner during unsuccessful manual Retry
- [x] Add manual refresh on Rewards, Tasks, Dashboard
- [x] Auto refresh opened screens every 30 seconds while backend confirmed online
- [x] Differentiate failed catalog refresh from true offline catalog in Rewards
- [ ] Run backend Maven tests / Android :app:compileDebugKotlin
- [ ] Verify /api/client-health accessible without credentials via actual Android BASE_URL host
- [ ] Emulator: backend down -> stale cache -> backend up -> indicator clears -> new Web task appears on Android within 30 s
- [ ] Verify expired JWT displays login instead of silently showing stale data as synced
- [ ] Protect background refresh from rapid status toggles and duplicate API calls if needed

- [x] Explain why an offline-only authenticated session needs fresh online login before synchronizing

## Offline participants/points (2026-10-11)
- [x] Cache participants and last confirmed balances by workspace/viewer
- [x] Show cached member names, roles, balances offline on Members and Points screens
- [x] Durable UUID-based pending offline earnings and penalties, visibly separate from confirmed balance
- [x] Flyway V20 unique operation IDs and secure idempotent backend replay endpoint
- [x] Require MANAGE_POINTS for ordinary manual point operations as well as replay
- [x] Reconcile pending operations only after real online auth; refresh confirmed balances
- [ ] Run backend migration + Maven tests and Android :app:compileDebugKotlin
- [ ] Test online cache -> offline 2 members -> queue earn and penalty -> online reauth -> repeated retry -> exactly-once server ledger
- [ ] Test server denies expired/revoked MANAGE_POINTS; pending entries must remain visible
- [ ] Encrypt local member/balance/operation stores before production and define removal/retention policy
- [ ] Improve rejected-operation management UI (edit/cancel, last failure, retry)

- [x] Provide manual retry button for offline earning/penalty queue with clear rejection status
- [ ] Verify security on actual device (parent online, child offline, invalidated permissions) and compile client/backend

## Currency-specific offline point adjustments
- [x] Offer actual cached/backend point type selector for manual earn/penalty/spend
- [x] Store currency ID/code/name in offline pending operation and display pending deltas per currency
- [x] Migrate Android SQLite queue v1 -> v2 without dropping existing pending operations
- [x] Backend idempotent replay validates selected currency and keeps old nullable payload support
- [x] Server typed manual point operation for online Earn/Penalty/Spend
- [ ] Build Java backend and Android Kotlin app; run Flyway/JPA startup and functional tests
- [ ] Test online selected currency and offline selected currency replay (other than POINTS)
- [ ] Test legacy pending items from v1 SQLite (NULL pointTypeId) sync safely
- [ ] Cache/display balances for each currency, not just default points

## Points quick edit and reward spending
- [x] Default amount 20, +/-10 buttons and saved last successful amount per currency/operation
- [x] Android spend dialog: optional reward selector; reward price/currency are fixed and shown
- [x] Parent can create true reward purchase for child from Points without duplicate manual deduction
- [x] Backend checks reward eligibility and permission to manage rewards
- [x] Online-only reward purchasing; manual spend is separate and unchanged
- [ ] Maven compile/test RewardController, Android :app:compileDebugKotlin
- [ ] Verify parent with points permission but without reward permission sees manual spend only
- [ ] Verify direct reward purchases create linked purchase and REWARD_PURCHASE point transaction exactly once
- [ ] Implement idempotent retries for parent-on-behalf reward purchase operations

## 2026-10-12 — Contabo Beta deployment
- [x] Reserve Contabo VPS for independent always-on family Beta; local laptop stays development environment
- [x] Prepare React production same-origin HTTPS API routing and explicit VITE_API_URL override
- [x] Prepare Android distinct debug/release API addresses and optional release endpoint override
- [x] Disable Android cloud backup of app-private data
- [x] Add step-by-step initial deployment and read-only audit notes in deploy/beta/README.md
- [ ] Inspect real Contabo RAM/disk/listening ports/Nginx and Apache over SSH without changing them
- [ ] Confirm Beta DNS, TLS certificate and ingress routing without breaking WireGuard/DEV
- [ ] Provision PostgreSQL Beta role/database, backups and isolated Java 21 systemd backend
- [ ] Deploy locally built React dist via Nginx and backend jar; verify HTTPS APIs
- [ ] Secure and sign Android release APK for stable future updates; test on parent phone
- [ ] Compile Maven/Gradle/npm and run migration/offline/synchronization smoke tests

## 2026-10-12 — Actual Contabo pre-deployment audit
- [x] Verified 8 GiB VPS with >7 GiB available RAM and ~94 GiB free disk
- [x] Verified active nginx port 80/8443 and active WireGuard UDP 51820
- [x] Verified public port 443 is available for Beta, but blocked by UFW until explicitly opened
- [x] Verified Docker and PostgreSQL not yet installed on VPS
- [x] Verified UFW contains routed allowlist for 1C external 8080 -> laptop Apache; preserve UFW/DNAT
- [x] Add docker compose Beta assets, environment template and Nginx HTTPS 443 config
- [x] Discover release-critical missing Flyway V1; require restored DEV DB with migration history or compatible baseline snapshot
- [ ] Back up UFW, Nginx and iptables NAT files to root-only path before Docker installation
- [ ] Check actual NAT rules and apt Docker/Compose package candidates
- [ ] Install Docker and verify the existing 1C forwarding and DEV still work
- [ ] Obtain sanitized/current DEV PostgreSQL dump and restore to Beta isolated DB
- [ ] Build/release Java JAR and React dist; obtain Beta TLS cert and bring Nginx 443 online

## 2026-10-12 — Clean Beta + multi-family onboarding decision
- [x] Decision: NO development data/account/transaction copy into Beta
- [x] Document tenant separation, registration, member wizard and optional task/reward templates in docs/MULTI_FAMILY_ONBOARDING_PLAN.md
- [x] Update deployment guidance to require empty-data schema and explicit workspace security fixes
- [ ] Fix critical cross-tenant access paths (WorkspaceController, EconomyController, other controllers and nested IDs)
- [ ] Create and verify clean database bootstrap (V1 pre-V2 schema through V20, or controlled schema-only V20 baseline)
- [ ] Invite-only first-family registration: initial admin + default roles, point type, settings (atomic)
- [ ] First-run wizard with participants and permissions; resumable
- [ ] Shared versioned template catalog for optional tasks/rewards, editable per-workspace copies, idempotent import
- [ ] Tests with two unrelated families and cross-workspace API security
- [ ] Only after security and bootstrap validated: Contabo Beta release and parent phone

## 2026-10-12 — Tenant isolation milestone 1, source committed (tests pending)
- [x] Root GET /workspaces exposes own family only; foreign GET/PUT denied, POST and DELETE temporarily disabled
- [x] Shared cross-family guard in Spring Security chain after JWT authentication
- [x] MANAGE_WORKSPACE, MANAGE_MEMBERS and MANAGE_ROLES workspace-scoped predicates
- [x] Economy and workspace role configuration protected from unprivileged family members
- [x] Group mutations/permissions protected; scoped role delegation needs explicit future support
- [x] Review paths for role sets/definitions, points, exception periods, exchanges
- [x] Unit tests for filter and workspace root behavior written
- [ ] Run Maven `-Dtest=WorkspaceIsolationFilterTest,WorkspaceControllerTest test` and verify context boot
- [ ] Integration test two distinct tenants across nested resource IDs and request payload references
- [ ] Implement clean PostgreSQL baseline from empty DB
- [ ] Implement transactional, invite-only family registration and parent setup wizard

## 2026-10-12 — Reproducible clean Flyway V1 baseline
- [x] Confirm Git annotated tag v0.3-workspace predates introduction of Flyway V2 and contains the legacy mapped schema
- [x] Add scripts/db/generate-v1-baseline.ps1: isolated Docker PostgreSQL 17, temporary historic Git worktree, Hibernate DDL create, pg_dump schema-only, second isolated V1-V20 Flyway + Hibernate validation
- [x] Update db/migration/README and deploy/beta/README to forbid copying current DEV data and require generated/validated V1
- [x] Remove hardcoded username "oleh" from clean Beta login page
- [ ] **User-run script** on laptop with Java 21 and Docker Desktop; capture PASS or validation error
- [ ] Review generated V1 SQL; commit to feature branch only after V1..V20 and JPA validation pass
- [ ] Implement invite-only family administrator registration and setup UI


## 2026-10-13 — Joining another family/workspace (AGREED; TODO)
- [ ] Public non-enumerable family/workgroup join identifier -> authenticated pending request -> authorized manager approval/decline
- [ ] Secure expiring/revocable workspace invitation links with configurable recipient, role and uses
- [ ] Preserve invite context across registration, Google OIDC redirect, email verification and login
- [ ] Authenticated invitation review, accept/decline and transactional idempotent membership creation
- [ ] Negative tests: expired/revoked/replayed invites, wrong user, races, workspace isolation
- [ ] DEV acceptance before Contabo BETA deployment

## 2026-10-13 — Join-code API incremental implementation
- [x] Commit V27 tables for invitations, join codes, join requests, redemptions and pending invite sessions
- [x] Commit join-code generator/request/rejection service and permission-scoped controller (fe23c79)
- [ ] Compile and migrate V27 on isolated local test PostgreSQL before running against existing DEV DB
- [ ] Add rate limits, expiration/approval handling, and a safe manager workflow
- [ ] Implement invitation token exchange and continuity through password/Google registration
- [ ] Integration tests (workspace A/B, user with multiple memberships, invite replay)
- [ ] DEV acceptance, then separately promote pinned commit to Contabo BETA

## 2026-10-13 — Join request manager approval increment
- [x] Transactional approval: row lock, workspace-scoped role validation, account duplicate check, member/membership insert, request review audit (2c822d7)
- [x] Manager permission guarded approval API (0f5b051)
- [ ] Test V27 migration, Java compile, approve/reject races, cross-workspace refusal, multi-workspace JWT switching on DEV
- [ ] Invitation links and context persistence through password + Google signup
- [ ] Guard public endpoint against automated join-code guessing and request flooding

## 2026-10-13 — Invite context increment
- [x] Server-side pending invitation handle storage and resumed preview (852a0bd)
- [x] Authenticated DEV API context begin/resume (30da43d)
- [ ] Browser-bound, anti-CSRF invitation preservation for unauthenticated signup and Google OIDC
- [ ] Invitation acceptance and atomic membership / use-limit / role controls
- [ ] Compile/test V27 migration and API against DEV test database
- [ ] Full multitenant negative tests, abuse throttling and BETA promotion after DEV acceptance

## 2026-10-13 — DEV multi-family milestone
- [x] List current account's active workspace memberships (`GET /auth/workspaces`)
- [x] Issue selected workspace JWT after membership authorization (`POST /auth/workspaces/{workspaceId}/select`)
- [x] Add second-family creation service and role-checked opt-in route, default disabled
- [ ] Compile/backend unit tests, PostgreSQL Flyway V27 and two-tenant HTTP isolation tests
- [ ] Review all role-scoped routes for use of selected workspace context, not legacy primary member
- [ ] Web/Android family switch UI; fully global identity and Google OIDC; invitation continuation

## 2026-10-13 — Web onboarding and consolidated validation
- [x] React signup page and API client with error/validation state
- [x] Route and login entry point, disabled by default through Vite env flag
- [x] Confirm existing React family selector/backend routes; do not duplicate
- [x] Consolidated Java + React build script; optional Maven tests
- [ ] Run build locally and verify Flyway V27 on isolated PostgreSQL
- [ ] Complete abuse protection and account signup race handling before enabling registration
- [ ] Add browser-safe invitation continuation and Google OIDC, then full end-to-end tests

## 2026-10-13 — Consolidated test gate
- [x] Review invitation and workspace session test coverage; adjust invite JdbcTemplate mock (b25b87c)
- [x] Single `-BackendOnly -WithTests` DEV checkpoint with V29 Flyway inventory requirement (066e7a8)
- [ ] ACTUALLY RUN Maven compilation/tests on local checkout
- [ ] Validate V27, V28, V29 against isolated PostgreSQL with Flyway
- [ ] Verify two-family signed JWT and malicious cross-tenant access
- [ ] Only after the above: onboarding registration and Google identity integration

## 2026-10-13 — Isolated PostgreSQL test gate
- [x] Add Testcontainers migration V27–V29 verification
- [x] Add cross-workspace DB role integrity checks (member + invitation)
- [x] Add family-less global identity persistence and provider-subject uniqueness tests
- [ ] Rerun full Maven test suite with Docker Desktop available; inspect surefire-reports for failures
- [ ] Confirm clean PASS before enabling new registration or Google auth
- [ ] Run invite/join HTTP API multi-family functional tests in disposable DB

## 2026-10-13 — Join/invitation integration follow-through
- [x] Detect/reject duplicate pending join request (HTTP 409)
- [x] Add disposable database tests of join application and manager approval
- [x] Add disposable database tests of invite context and one-use redemption
- [ ] Run full Maven Testcontainers suite on laptop; resolve actual failures
- [ ] Add HTTP security/manager-authorization checks to join/invite controller suite
- [ ] Preserve BETA isolation until complete test gate

## 2026-10-13 — Join/invite safety gate
- [x] Fix invalid MockMvc JSON test content
- [x] Add disposable PostgreSQL invitation revoke-after-context test
- [x] Add disposable PostgreSQL join rejection and deny-later-approval test
- [ ] Run `scripts/dev/check-onboarding.ps1 -BackendOnly -WithTests` locally
- [ ] Investigate any test failure before changing Flyway/production code
- [ ] Add concurrency/idempotency tests only after complete suite passes

## 2026-10-13 — Invitation/join development phase closed for now
- [x] Join codes, manager review/approve/reject, expiring/revocable invitations, pending invitation context, workspace-local membership and permission isolation implemented in DEV branch.
- [x] Unit and Testcontainers integration scenarios added for happy path, repeat use, revoke/reject, role isolation, manager HTTP access.
- [ ] Full suite after latest commits NOT CONFIRMED. Keep as final release gate, not an ongoing feature development thread.
- [ ] Concurrency edge cases (simultaneous approve/invite acceptance, revoke vs accept) need explicit test and robust conflict handling before public rollout.
- [ ] Email-bound invitations and rate limiting remain out of public-release scope until identity/login work completes.
- [ ] User has local uncommitted FamilyPointsApplicationTests.java edits. Preserve them in stash before git pull; compare after update, do not blindly pop.
- [ ] No Contabo deployment, registration/public invite enablement until integration tests and security review pass.
**Next main development:** account identity lifecycle, password registration, account without family, Google OAuth subject binding, workspace selection; UI updates only to verify completed workflows.

## 2026-10-13 — Global registration next gate
- [x] Account-only registration service and disabled-by-default endpoint
- [x] Initial service tests for account without family
- [ ] Permit account-only endpoint anonymously in SecurityConfig while preserving feature gate (write temporarily blocked)
- [ ] Enable authenticated family-less account to create its first family without preexisting workspace authority
- [ ] Add Spring MVC/JWT lifecycle tests and check duplicate username races
- [ ] Do not enable public registration, Google/OIDC or BETA deployment until safety gates pass

## 2026-10-13 — First family implementation gate
- [x] Feature-gated POST /api/onboarding/first-family for authenticated global accounts
- [x] Reject accounts with existing family and serialize parallel first-family requests
- [x] Initial unit tests for successful first family / existing family conflict
- [ ] Run Maven unit and PostgreSQL integration tests; verify full identity lifecycle
- [ ] Add real API-level onboarding tests and address any failures
- [ ] Leave public signup and first-family flags disabled until release review

## 2026-10-13 — Global account switcher fixes
- [x] Audit existing /auth/workspaces selection and scoped JWT; avoid parallel implementation
- [x] Fix /api/me/workspaces on account with no families
- [x] Adapt selected-workspace unit test and add empty-list regression
- [ ] Run local Maven tests and full Testcontainers gate after safe git pull
- [ ] Complete signup -> first-family -> workspace-selection HTTP integration coverage
- [ ] Implement provider-subject-verified Google identity binding, not email-only linking

## 2026-10-13 — Google identity milestone
- [x] Add Google ID-token signature, issuer, expiry, audience and azp validation.
- [x] Resolve existing linked account exclusively using provider+subject, never email.
- [x] Feature-gate POST /auth/google disabled by default; avoid assigning a workspace automatically.
- [x] Add initial controller tests.
- [ ] Execute Maven build/tests and resolve any compilation/integration errors.
- [ ] Implement authenticated, ownership-confirmed Google account linking with uniqueness-conflict behavior and tests.
- [ ] Implement optional Google account creation with secure registration/onboarding integration only when ready.
- [ ] Never enable Google or deploy BETA until full verification.

## 2026-10-13 — Google account link integration
- [x] Signed-in and current-password-confirmed Google linking controller
- [x] Shared token verifier for Google login and linking
- [x] Subject-based account resolution; no email matching
- [x] Unit tests for linking and resolution failure paths
- [ ] Run full backend Maven tests on DEV and fix any failures
- [ ] Add provider verifier tests and duplicate/conflict integration tests before enabling
- [ ] Google front-end consent/onboarding flow and public activation deferred

## 2026-10-14 — Web/Android onboarding
- [x] Web: global account registration screen, explicit workspace chooser and first-family screen
- [x] Web: avoid /api/me on global login before selecting a workspace
- [x] Web: verify scoped JWT before replacing active workspace session
- [x] Android: typed registration and first-family API endpoints
- [ ] Build React and smoke-test older family member sessions
- [ ] Build Android and implement Compose account registration / initial family chooser flow
- [ ] Add end-to-end browser tests for both zero and multiple workspaces
- [ ] Public flags stay disabled, no Contabo changes

## 2026-10-14 — Android account and workspace onboarding
- [x] Global login returns workspace choice state instead of treating missing /api/me as failed credentials
- [x] Restore global JWT to chooser, without allowing unselected family feature access
- [x] Compose chooser for existing families and first-family form
- [x] Android online account registration mode and API callback
- [ ] Run ./gradlew :app:assembleDebug and fix Kotlin compile errors
- [ ] Test single family, zero families, two families, revoked workspace, offline login, and first-family flags disabled on emulator/phone
- [ ] No Google credential UI and no BETA deployment until backend feature tests and credentials configured

## 2026-10-14 — Onboarding stabilization
- [x] Prevent Android workspace-selection network failure from silent offline fallback
- [x] Suppress expected Android workspace-navigation exception error message
- [x] Apply Web session idle/refresh checks for workspace-less account
- [ ] Run Android Gradle assembleDebug and Web npm run build locally
- [ ] Test /auth/login -> workspace choice -> /api/me with real DEV backend
- [ ] Only then enable DEV registration and first-family flags; never enable in BETA by default

## 2026-10-14 — Additional family creation
- [x] Web header button accessible with one family
- [x] Form validates nonblank family and owner, sends authenticated API request
- [x] Permission gate in UI; backend authorization unchanged
- [x] Select and verify newly created family after creation
- [ ] Confirm npm run build on local DEV
- [ ] Explicitly enable familypoints.additional-family.enabled only in DEV if testing creation
- [ ] Manually verify creating and switching families without sharing task/points data

## 2026-10-14 — Additional-family DEV smoke
- [x] Diagnosed 503 feature flag on local DEV screenshot
- [x] Added explicit local-only Spring Boot launcher; default flag remains false
- [ ] Restart DEV backend with appropriate DEV credentials, not BETA credentials
- [ ] Create test family, verify auto-switch and member/task/points isolation

## 2026-10-14 — Web header
- [x] Family selector + create button aligned left
- [x] Language, member name, logout aligned right
- [x] Family selection style matches language selection
- [ ] Verify npm run build and mobile header visually

## 2026-10-14 — Web buttons / chooser
- [x] Logout separate at bottom-right of chooser
- [x] Compact neutral shared button styling for ordinary actions
- [ ] Run frontend npm run build and visually check forms, destructive actions, mobile layout

## 2026-10-14 — Registration closure (large package; still pending verification)
- [x] Additive email/verification-token schema V30 preserving pre-existing users
- [x] New email/password signup blocked until email confirmation
- [x] One-time verification, resend throttle, email-based password recovery and Web routes
- [x] Google Web button plus backend verified Google email/subject registration (no email auto-link)
- [x] Android signup email field and API payload
- [x] README for SMTP and Google Cloud OAuth configuration
- [ ] Maven clean test / Vite build / Android assembleDebug on local DEV
- [ ] Complete Google native Android Credential Manager (not implemented yet)
- [ ] Configure verified SMTP sender and Google OAuth client IDs; manually test real emails and Google login
- [ ] Add abuse/IP throttling, transaction/outbox behavior, duplicate-race and end-to-end HTTP integration tests
- [ ] Keep flags OFF in Contabo BETA, preserve DEV database and legacy accounts

## 2026-10-14 — Shared email account model
- [x] Nonunique email via additive V31 (V30 preserved)
- [x] Username remains unique, tokens belong to account ID
- [x] Password and Google signup allow email reuse without identity auto-link
- [x] Recovery/resend require username plus email; Web and unit tests adapted
- [ ] Run Maven clean test, confirm V30→V31 Flyway migration on disposable PostgreSQL, then Web build
- [ ] Decide parent-managed child account ownership and safeguards before public registration

## 2026-10-14 — Password minimum 8
- [x] Align registration, reset, legacy family signup controllers and services to 8–128 characters
- [x] Align Web registration and reset forms and public configuration docs
- [x] Add 7-vs-8 character boundary tests
- [ ] Run local Maven test and Web production build; Android smoke test
- [ ] SMTP sender and Google OAuth client IDs still require external provisioning; native Android Google credential flow outstanding

## 2026-10-14 — Auth external provider gate
- [x] Verify local Maven, Vite, Android baseline builds (user confirmed before new hardening changes)
- [x] Reject insecure non-local mail verification link base URL
- [x] Add initial mail token tests and read-only local SMTP/Google config preflight
- [ ] Re-run Maven tests after new mail token tests
- [ ] Choose/configure SMTP provider, verify sender domain SPF DKIM DMARC and test mail delivery
- [ ] Create Google Cloud Web client and configure authorized JavaScript origins / ID token audience
- [ ] Native Google credential manager Android flow and release security checks pending

## 2026-10-14 — Consolidated service configuration
- [x] Central Git-tracked DEV service settings template
- [x] Gitignored local override for secrets; optional Spring import
- [x] README including IntelliJ working directory and environment precedence
- [ ] User copies template, populates local values, restarts DEV and verifies first family creation
- [ ] Never upload SMTP/JWT/database secrets to GitHub
