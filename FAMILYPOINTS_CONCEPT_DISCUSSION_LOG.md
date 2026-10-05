# FamilyPoints — Concept & Mechanics Discussion Log

> **Purpose:** persistent project memory for product mechanics, domain concepts, alternatives, rejected ideas, and decisions discussed outside code.
>
> This file is intentionally broader than a roadmap or technical specification. It exists so that important product reasoning does not disappear when a chat/session ends.
>
> **Rule:** do not silently rewrite old decisions. Prefer appending a dated entry that marks an earlier idea as `SUPERSEDED`, `REJECTED`, or `REFINED`.

Last reconstructed/updated: **2026-10-04**

---

## 1. Status vocabulary

Use these labels consistently:

- **IMPLEMENTED** — confirmed in the current Git branch/source.
- **PARTIAL** — implementation exists but semantics/API/UI are incomplete.
- **AGREED** — concept/mechanics were explicitly agreed but not yet implemented.
- **DISCUSSED** — explored, but no final decision is reliably known.
- **RECOVER** — we know the topic was discussed earlier, but exact details were lost and must not be invented.
- **SUPERSEDED** — earlier model replaced by a later model.
- **REJECTED** — consciously not chosen.
- **QUESTION** — unresolved design decision.

Source of truth hierarchy:
1. Current GitHub source for what is actually implemented.
2. This log for historical product/domain reasoning.
3. Current technical specification / concept documents for consolidated design.
4. Chat memory only as a recovery source — never as the sole durable record.

---

## 2. Core product direction

### 2026-10-04 — reconstructed baseline

**AGREED**

FamilyPoints is a family-focused system that combines:

- tasks and recurring responsibilities;
- points/economy;
- rewards and penalties;
- behavior tracking;
- learning/practice;
- application/device access control;
- groups, roles and scoped authorization.

The architecture should also remain capable of evolving into a broader Team/Office Manager without rewriting the core domains.

**AGREED**

Keep these conceptual planes separate:

1. **Workspace / organizational graph** — members, groups, teams.
2. **Work hierarchy** — `WorkNode` with `GROUP/TASK`.
3. **Authorization context** — permissions and roles with explicit scope.

Do not derive authorization merely from hierarchy membership.

---

## 3. Tasks: definition, occurrence, execution

### 2026-10-04 — reconstructed

**AGREED**

- `TaskDefinition` describes the reusable/recurring definition.
- `TaskInstance` represents a concrete occurrence/execution.
- Assignment, eligibility, preferred executor, claim, current executor, participation, and delegation are separate concepts.
- `claim != delegation`.
- Parent/child work hierarchy does not itself grant access.

**AGREED**

Assignment policy family:

- `SINGLE_MEMBER`
- `GROUP_SHARED`
- `OPEN_GROUP`
- `OPEN_WORKSPACE`
- `PREFERRED_MEMBER`

**AGREED**

Distinguish:

- `assignedActor`
- `preferredActor`
- `claimedByActor`
- `currentExecutor`
- responsible/coordinator actor where required

**DISCUSSED / NEEDS RECONCILIATION**

Older lifecycle concepts included:

- `AVAILABLE`
- `IN_PROGRESS`
- `WAITING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `EXPIRED`
- `CANCELLED`

The current implementation later introduced/uses states such as `PAUSED` and `RELEASED`. Do not restore old states blindly; reconcile with current code before changes.

**AGREED**

Task visibility, availability, eligibility and execution permission are distinct:

- a future task may be visible in calendar;
- it may not yet be available;
- an actor may see it but not be eligible;
- an eligible actor may still lack management rights.

---

## 4. Recurrence, calendar, future tasks

**AGREED**

Recurrence:

- `ONCE`
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `CUSTOM` reserved for future extension.

Rules:

- no silent historical backfill;
- monthly day 31 means literal day 31;
- historical `ONCE` should not silently create a pending occurrence;
- recurring tasks starting in the past should not generate the entire missing history unless an explicit policy says so.

**AGREED**

Calendar/product views should support:

- day;
- week;
- month;
- arbitrary period;
- visible future tasks.

**AGREED**

Reminder is separate from deadline.

**DISCUSSED**

Future mechanisms:

- defer;
- decline;
- extension request;
- review/approval;
- overdue handling.

---

## 5. Requirements / dependencies / conditions

**AGREED**

Requirements are broader than a simple task-dependency table.

`RequirementGroup` supports:

- `ALL_SATISFIED`
- `MINIMUM_SATISFIED`

and may use:

- `requiredCount`
- per-item `alwaysRequired=true`

Examples:

- 1 of 3 alternatives;
- 2 of 5 alternatives;
- mandatory conditions plus selectable alternatives.

**AGREED**

Recurring dependency must match the relevant occurrence/date, not merely any historical completion.

**DISCUSSED / AGREED DIRECTION**

Generic requirement types may eventually include:

- another task completed;
- learning activity completed;
- balance/savings condition;
- behavior condition;
- achievement;
- other domain-specific conditions.

**INVARIANT**

Do not mix:

- mandatory task;
- eligibility;
- prerequisite/requirement.

---

## 6. Quality, approval and reward calculation

### Recovered mechanics

**AGREED**

Reward policies:

- `FIXED`
- `QUALITY_PERCENTAGE`
- `BASE_PLUS_QUALITY_BONUS`
- `MILESTONE`

A task may have a quality rating that **does not affect payment at all**.

**AGREED**

Configurable quality / star scale. Recovered example:

- 5★ → 100%
- 4★ → 70%
- 3★ → 50%
- 2★ → 20%
- 1★ → 0%

This mapping is an example/configuration, not a universal hard-coded law.

**AGREED**

Approval policy may support:

- `AUTO`
- `PARENT_REQUIRED`

**INVARIANT**

Do not conflate:

- quality rating;
- deadline;
- reputation;
- behavior rating.

These are different signals.

---

## 7. Milestones / savepoints / partial rewards

**AGREED**

Tasks may contain `TaskStep` / `TaskMilestone`.

Milestones/savepoints can support partial reward settlement.

Reward timing modes discussed:

- `PAY_IMMEDIATELY`
- `PAY_ON_TASK_COMPLETION`

**QUESTION**

Before implementation, define precisely:

- whether failed final completion can claw back already-paid milestone rewards;
- whether milestone quality is independent from final task quality;
- whether milestone completion can be reverted;
- idempotency rules for partial settlement.

---

## 8. Reward groups and reward selection

**AGREED**

`RewardGroup` modes:

- `ALL`
- `CHOOSE_ONE`
- `CHOOSE_MULTIPLE`

Reward types should be extensible, including:

- `POINTS`
- `GOLD`
- `MONEY`
- `DEVICE_TIME`
- future custom/resource types.

**AGREED**

Once a random/selected reward result has been resolved, the result must be persisted and reused. It must not re-randomize during subsequent reads or settlement retries.

**RECOVER / DISCUSSED**

Selection actor concepts recovered from earlier discussion:

- `CHILD`
- `PARENT`
- `SYSTEM`

Selection method concepts:

- `MANUAL`
- `RANDOM`
- `RANDOM_FILL`

Additional recovered concepts:

- `randomEligible`
- weighted selection / `weight`

Exact final schema was not reliably preserved. Do not treat the enum names above as final database/API contracts until revalidated.

---

## 9. Reputation

### 2026-10-04 — mechanics recovered with user

**AGREED**

Reputation is a separate long-term balance/metric. It is **not**:

- task quality stars;
- `BehaviorDailyReview`;
- the spendable points balance.

### Earning and losing reputation

**AGREED**

A task may define not only point reward/penalty but also a separate reputation delta:

- reputation earned for successful completion;
- reputation lost for failure/poor outcome where configured.

Task points and task reputation are therefore two independent settlement dimensions.

**AGREED**

Behavior can also affect reputation:

- good behavior may grant reputation;
- bad behavior may deduct reputation.

The exact mapping remains configurable and must not collapse Behavior into the Reputation domain.

### Manual reputation awards

**AGREED**

An authorized administrator/group administrator may manually award reputation as a bonus, analogous to manual point awards, but only when the actor has the explicit permission to do so.

**DESIGN / QUESTION**

Manual reputation distribution should support configurable limits so a delegated group administrator cannot mint unlimited reputation.

Possible configuration level discussed:

- per actor/role;
- per group/context;
- per period (daily/weekly/monthly);
- maximum amount per operation;
- total reputation allocation/budget available to a group.

Exact limit model is not yet fixed.

### Reputation as an access threshold

**AGREED**

Rewards, purchases, bonuses, games, privileges or other catalog items may require a minimum reputation threshold in addition to their point price.

Example:

- bicycle price: 1000 points;
- bicycle reputation requirement: 1000 reputation;
- member must satisfy both conditions before the reward is available.

The reputation threshold is a **qualification/access requirement**, not necessarily a spend. Unless a specific reward says otherwise, buying an item should spend points while reputation remains as the member's standing.

This allows expensive/important rewards to require sustained good participation over time rather than only accumulated points.

### Decay / reset

**DISCUSSED — IMPORTANT**

Reputation should potentially be non-permanent so that old good history cannot indefinitely unlock every high-trust reward.

Mechanics considered:

- reputation decay over time;
- reputation loss when the member remains inactive / does not participate;
- periodic reset;
- configurable decay windows/rates;
- exceptions/excused periods so sickness/vacation/etc. do not unfairly reduce reputation.

Example product intent: a child who wants a high-value reward such as a bicycle may need roughly a month of consistently good behavior and task effort to maintain/build the required reputation.

**QUESTION**

Before implementation decide:

- continuous decay vs scheduled decay;
- inactivity-only decay vs universal decay;
- whether reputation can reach zero but never negative, or negative reputation is allowed;
- workspace/group-specific decay policy;
- grace period;
- interaction with excused days;
- whether some reputation types/categories are non-decaying;
- whether a seasonal/manual reset is supported.

### Reputation ledger/history

**AGREED DIRECTION**

Reputation changes should be auditable, similarly to points, with source/reason and actor:

- TASK;
- BEHAVIOR;
- MANUAL_BONUS;
- MANUAL_PENALTY;
- DECAY;
- ADJUSTMENT;
- other future source types.

A dedicated reputation transaction/history model is preferable to storing only the current scalar balance.

### Invariant

Do not conflate:

- spendable points;
- reputation standing;
- task quality;
- behavior daily score.

A reward may require both points and reputation, but they represent different things.

---

## 10. Behavior module

**AGREED**

Behavior is a separate domain, not a special type of Task.

Core concepts:

- `BehaviorRule`
- `BehaviorEvent`
- `BehaviorDailyReview`

`BehaviorRule` may be:

- `POSITIVE`
- `NEGATIVE`

with configurable default amount.

Examples previously discussed:

- +10 for helping;
- −30 for fighting.

A concrete behavior event may create a reward/penalty ledger transaction.

**AGREED**

Daily behavior score is separate from task quality and reputation.

Recovered example bonus mapping:

- 90–100% → +30
- 80–89% → +20
- 70–79% → +10
- <70% → 0

This is an example/configuration, not a mandatory fixed rule.

**AGREED DIRECTION**

Negative concrete events may penalize. Automatically penalizing solely because the aggregate daily score is low was not recommended.

**RECOVER / DISCUSSED**

A child-created/self-reported `BehaviorEvent` may require approval, e.g. a `PENDING_APPROVAL` stage.

Exact final workflow needs confirmation before implementation.

---

## 11. Streaks and achievements

**AGREED / DISCUSSED**

Streaks were explicitly part of the concept.

Potential sources include:

- repeated task completion;
- learning practice;
- positive behavior;
- daily/weekly consistency.

**AGREED DIRECTION**

Streaks/achievements should not be implemented as fake tasks merely to reuse the Task module.

**QUESTION**

Need final rules for:

- streak reset;
- excused days;
- freeze/protection;
- reward thresholds;
- whether streaks are per-rule, per-domain or generic.

---

## 12. Exceptions / excused days

**PARTIAL / RECOVER**

The system already has the idea of exception periods/snapshots in task mechanics, but the full business semantics of **excused days** were discussed more broadly and are not fully recovered.

Potential use cases that must be decided before implementation:

- sickness;
- vacation;
- family event;
- technical impossibility;
- parent-granted exception.

**QUESTION**

An excused day may need to affect separately:

- recurrence expectation;
- streak continuity;
- penalties;
- quality/reputation calculations;
- learning daily limits.

Do not assume one global behavior for all domains.

---

## 13. Learning Bank

**AGREED**

Learning is a separate domain using the shared economy/ledger.

Core concepts:

- `LearningActivity`
- `Question`
- `Attempt`

Categories discussed:

- mathematics;
- reading;
- languages;
- logic.

Question types:

- `NUMBER_INPUT`
- `SINGLE_CHOICE`
- `MULTIPLE_CHOICE`
- `TRUE_FALSE`

Difficulty:

- `EASY`
- `NORMAL`
- `HARD`
- future adaptive difficulty.

Modes:

- `PRACTICE`
- `CHALLENGE`

**AGREED**

Anti-farming controls may include:

- `maxAttemptsPerDay`
- `maxRewardPerDay`
- `cooldown`
- `repeatRewardPolicy`

**AGREED**

Other discussed concepts:

- generators;
- automatic answer checking where possible;
- reading comprehension;
- book/chapter milestones;
- learning plans.

---

## 14. Claimable task bank and Task Requests

**AGREED**

Support both:

- assigned work;
- claimable/open task bank.

Open task policies still respect eligibility and authorization.

`OPEN_GROUP` / `OPEN_WORKSPACE` do **not** imply that every eligible executor may manage/edit the task.

**AGREED / DISCUSSED**

`TaskRequest` is a separate scenario for proposing/requesting a task.

A child may request:

- a new task;
- work from a bank;
- approval for an activity.

Exact request workflow/statuses still require consolidation before implementation.

---

## 15. Group execution and coordination

**AGREED**

Groups are actors, not merely lists of members.

A task assigned to a group may still be claimed/executed by a concrete member.

**AGREED**

Role-based eligibility can filter a target group using match semantics:

- `ANY`
- `ALL`

**DISCUSSED**

Separate from executor:

- responsible coordinator;
- preferred assignee;
- participants;
- delegated actor.

**RECOVER / DISCUSSED**

Earlier discussion included concepts such as:

- responsibility transfer;
- delegated bonus/penalty;
- completion bonus when all requirements are satisfied.

Do not implement these until exact semantics are reconstructed.

---

## 16. Delegation vs subtask / subcontract

**INVARIANT**

Delegation is not the same as:

- claim;
- assignment;
- responsibility transfer;
- subcontract;
- child/subtask.

Delegation must preserve original assignment/history.

Funding/subcontract mechanics belong to later task change/funding domains.

---

## 17. Economy / ledger

**IMPLEMENTED BASE**

Point ledger/economy exists and task settlement is designed to be idempotent.

**AGREED**

Sources may include:

- task reward;
- milestone reward;
- behavior;
- learning;
- manual award;
- manual penalty;
- future funding/subcontract mechanics.

**AGREED**

Manual penalty is a first-class family scenario, not merely a negative task reward.

**LATER**

Funding ledger after a generic Task Change Request mechanism.

---

## 18. Device and application control

**AGREED**

Core concepts:

- program/app price in points;
- duration packages;
- daily/weekly limits;
- schedules;
- per-app/per-device/per-category rules;
- always-allowed/free apps;
- automatic stop/block when paid or allowed time ends.

Partial use policy discussed:

- auto;
- manual;
- ignore;
- unused remainder policy including “do not count/save remainder”.

Platform direction:

1. Android first;
2. Windows agent next;
3. iOS later.

Server remains source of truth; clients enforce locally according to `DeviceCapabilities` and sync.

---

## 19. Roles / permissions / actor model

**AGREED**

Avoid hard-coded business authorization such as:

`if PARENT ... else CHILD ...`

Use generic actor/role/permission mechanisms.

Functional role is not the same as security permission.

Current target direction includes:

- `RoleDefinition`
- `RoleAssignment`
- `RoleSet`
- `RoleSetBinding`
- `RolePermissionGrant`

Visibility:

- `PRIVATE`
- `SHARED`

Scope:

- `CURRENT`
- `SUBTREE`

Scope flows downward only.

No role/permission escalation upward merely because an actor has access in a child context.

---

## 20. Concepts that must never be silently merged

Keep these separate unless a future explicit decision says otherwise:

- Task quality vs BehaviorDailyReview vs Reputation.
- Assignment vs eligibility vs claim vs current execution vs delegation.
- Organizational graph vs WorkNode hierarchy.
- Functional role vs security permission.
- Deadline vs reminder.
- Task dependency vs eligibility.
- Task vs Behavior event.
- Task vs Learning activity.
- Delegation vs subcontract/subtask.
- Visibility vs availability vs eligibility vs manage permission.

---

## 21. Recovery queue

These topics are known to have incomplete historical recovery:

- exact Reputation decay/reset algorithm and manual-allocation limit model;
- exact Reward selection actor/method schema;
- weighted/random-fill reward semantics;
- excused-day rules and their effect on streaks/reputation/penalties;
- streak reset/freeze/reward rules;
- child-created BehaviorEvent approval workflow;
- responsibility transfer semantics;
- delegated bonus/penalty semantics;
- completion bonus semantics;
- old `HABIT` concept and whether it was superseded by Recurrence + Behavior;
- exact TaskRequest workflow/statuses.

Do not close these items by assumption.

---

## 22. Append-only discussion entry template

When a meaningful product/domain discussion happens, append an entry here before the session ends.

```md
### YYYY-MM-DD — <topic>

**Status:** AGREED | DISCUSSED | RECOVER | SUPERSEDED | REJECTED | QUESTION

**Context**
Why the topic came up.

**Decision / current understanding**
- ...

**Alternatives considered**
- ...

**Invariants / things not to mix**
- ...

**Open questions**
- ...

**Implementation impact**
- backend:
- frontend:
- DB/migration:
- tests:

**Supersedes / superseded by**
- ...
```

---

## 23. Maintenance rule

Any time a discussion changes mechanics, product behavior, domain boundaries, statuses, reward formulas, permission semantics, or lifecycle:

1. update this log;
2. update the project checklist;
3. only then consider the topic safely preserved.

A chat summary/handoff is **not** a substitute for this file.


---

## 24. Purchase / reward eligibility requirements

### 2026-10-04 — conditions for buying rewards with points

**AGREED**

Buying a reward/item is not determined by point balance alone.

A catalog item/reward may have:

1. a **price** — spendable resources that are consumed on purchase, e.g. points;
2. **eligibility requirements** — conditions that must be satisfied but are not necessarily consumed.

Example:

- bicycle price: 1000 points;
- minimum reputation: 1000;
- complete a defined set of tasks;
- no serious behavior incidents during the required period.

If all requirements are satisfied, the item becomes purchasable.

### Reputation is not spent

**INVARIANT**

Reputation used as a purchase requirement is a threshold/qualification, not a currency spend.

For example:

- before purchase: 1200 points, 1100 reputation;
- bicycle costs 1000 points and requires 1000 reputation;
- after purchase: 200 points, 1100 reputation.

Reputation changes only through its own reputation mechanics (task/behavior/manual/decay/etc.), not merely because a purchase was made.

### Generic requirement engine

**AGREED DIRECTION**

Purchase eligibility should reuse a generic requirement/condition engine rather than adding bicycle-specific or reward-specific fields.

Useful requirement types may include:

- `MIN_REPUTATION` — reputation >= configured value;
- `TASK_COMPLETED` — a specific task completed;
- `TASK_COUNT` — N qualifying tasks completed;
- `TASK_GROUP_COMPLETED` — all / minimum number from a defined task set;
- `NO_SERIOUS_BEHAVIOR_EVENT` — no event above configured severity within a period;
- `BEHAVIOR_SCORE_AT_LEAST` — daily/period behavior score above threshold;
- `STREAK_AT_LEAST` — maintain a configured streak;
- `LEARNING_ACTIVITY_COMPLETED`;
- `ACHIEVEMENT_UNLOCKED`;
- `MIN_POINT_BALANCE` where useful;
- future generic conditions.

The exact enum/schema names are not fixed yet.

### Requirement composition

**AGREED**

Requirements should support the same compositional ideas already discussed for Tasks:

- `ALL_SATISFIED`;
- `MINIMUM_SATISFIED`;
- `requiredCount`;
- `alwaysRequired`.

This allows reward rules such as:

> To buy the bicycle:
> - always: reputation >= 1000;
> - always: no serious misconduct in the last 30 days;
> - complete at least 3 of these 5 target tasks;
> - have at least 1000 spendable points at checkout.

### Time windows

**AGREED DIRECTION**

Behavior/task requirements may be evaluated over an explicit time window, for example:

- last 7 days;
- last 30 days;
- current month;
- since a configured start date.

This prevents a one-time historical achievement from satisfying a requirement that is intended to measure recent effort.

### Serious behavior incidents

**DESIGN / QUESTION**

A requirement such as “no serious misconduct” should be based on explicit Behavior severity/category rather than arbitrary point/reputation loss.

Possible semantics:

- block if a `BehaviorEvent` with severity `SERIOUS` or `CRITICAL` exists in the evaluation window;
- optionally restart a clean-period timer after such an event;
- excused/cancelled/reversed events should not count.

Exact severity model and clean-period semantics remain to be defined.

### Purchase evaluation and audit

**AGREED DIRECTION**

At checkout the server should evaluate all requirements atomically enough to avoid stale eligibility:

1. evaluate current requirements;
2. verify sufficient spendable points/resources;
3. create purchase/redemption record;
4. deduct points/resources;
5. preserve the evaluated requirement result/snapshot for audit where useful.

A failed requirement must block the purchase without spending points.

### UX implication

**AGREED DIRECTION**

The UI should explain *why* a reward is locked.

Example:

- ✅ 1000/1000 reputation;
- ✅ no serious incidents for 30 days;
- ❌ completed 2/3 required target tasks;
- ✅ 1350/1000 points.

This turns the reward into a visible long-term goal rather than a mysterious locked item.


---

## 25. Role catalog administration, activity templates and local contextual roles

### 2026-10-04 — recovered role administration mechanics

**AGREED**

The system must support a reusable catalog of predefined role sets and roles tailored to the type of Workspace/activity.

Examples of activity/template families discussed:

- Family;
- Office / Small Team;
- Business;
- Software Development / project work;
- Household;
- future custom templates.

A template may seed an initial role catalog appropriate to the activity instead of forcing every Workspace to build roles from scratch.

### Predefined role localization

**AGREED**

Predefined roles/role sets use stable `systemCode` values as business keys and must be displayed in the current UI language.

The localized visible name is not the business identifier.

Examples:

- LEADER
- DEPUTY
- SENIOR
- EXECUTOR
- ASSISTANT
- REVIEWER
- OBSERVER

The same predefined role should therefore appear translated according to interface language while remaining the same domain role.

### Workspace-level role administration

**AGREED**

Workspace settings must contain a dedicated role administration area, conceptually **“Ролі та набори”**, where an authorized user can:

- view predefined/shared RoleSets;
- view RoleDefinitions grouped by RoleSet;
- create custom SHARED RoleSets;
- create/edit custom SHARED RoleDefinitions;
- configure descriptions/localization metadata where applicable;
- configure permission presets/grants where authorized;
- bind reusable RoleSets to contexts.

Global/shared catalog administration should not be hidden inside a particular GroupEditor.

### Reusable RoleSets

**AGREED**

A RoleSet/RoleDefinition can be reusable and available in multiple contexts.

`RoleSetBinding` makes a reusable set available in a Workspace/Group/Task context but does **not** assign roles to actors automatically.

Examples:

- a common Management set can be available in several groups;
- the same EXECUTOR/REVIEWER definitions can be reused in different project groups;
- binding is catalog/context configuration, assignment is a separate action.

### Local/private contextual roles

**AGREED**

A user who is allowed to create/manage a specific context may create a local functional role directly inside that context.

Examples:

- user creates group **“Група для прибирання”**;
- adds the members available to that group;
- assigns a standard role such as LEADER/SENIOR/EXECUTOR;
- creates an additional custom role specific to cleaning work;
- the custom role is visible only in that group/context (and, if explicitly configured with subtree semantics, in allowed descendants), not in the general Workspace catalog.

Such a role is `PRIVATE` by default.

It must not appear as a general reusable role for unrelated groups/tasks.

### Local role creation and privilege safety

**INVARIANT**

Being allowed to create a local functional role does not grant the creator permission to invent new security privileges.

A local role may:

- organize responsibilities;
- be assigned to actors in the current context;
- participate in eligibility/assignment logic;
- receive only permission grants the creator is authorized to delegate.

It may not:

- become Workspace-global automatically;
- expose itself in unrelated/sibling contexts;
- escalate rights above the creator's delegation scope.

### Contextual administration pattern

**AGREED**

Use this separation:

**Workspace settings / role catalog**
- administer shared/predefined role sets and roles;
- translations/system codes;
- shared presets;
- catalog-wide availability.

**Group/Task context**
- choose/bind available shared role sets;
- assign available roles to members/actors;
- create context-local PRIVATE roles where permitted;
- configure only rights that may legally be delegated in that context.

This is the target UI/authorization model for the role refactor.

### Templates and role catalog

**AGREED DIRECTION**

WorkspaceTemplate should be able to seed, at minimum:

- default RoleSets;
- predefined RoleDefinitions;
- initial bindings;
- group structure;
- WorkNode structure;
- permission presets;
- economy/default settings.

The large template wizard remains later work, but the role model must remain compatible with this foundation now.


---

## 26. Simplified role administration UX

### 2026-10-04 — stop exposing RBAC complexity in ordinary group editing

**AGREED**

The technical role/permission backend remains flexible, but ordinary users should not have to administer raw RBAC details for routine work.

The normal GroupEditor experience is simplified to:

- local roles for the current group;
- simple permission profiles;
- optional advanced permission editing only when needed.

**AGREED**

RoleSet administration is removed from the normal GroupEditor flow.

Shared/system RoleSets and shared RoleDefinitions belong in:

- Workspace settings;
- role/template administration;
- later WorkspaceTemplate/activity presets.

**AGREED**

Local roles:

- belong only to the current group/context;
- do not require a RoleSet;
- can be created, edited, assigned and deleted directly in the group;
- may use a simple predefined permission profile.

Initial permission profiles:

- NONE — no additional permissions;
- EXECUTOR;
- SENIOR;
- LEADER / group leader;
- CONTROL / controller;
- CUSTOM — explicit advanced configuration.

The profile is a UI convenience, not a new security primitive. It resolves to ordinary permission grants/scopes underneath.

**AGREED**

Advanced raw permissions remain available behind an explicit “Advanced permissions” control.

**INVARIANT**

Do not force users through:

RoleSet → Role → individual permission checkboxes → scope → assignment

for simple scenarios.

The common scenario should remain:

create group → add members → create/choose role → assign role.

**STATUS**

This is considered sufficient for the current development stage. Further role refinements should be driven by real usage rather than additional speculative complexity.


---

## 27. Automatic naming and duplicate detection

### 2026-10-04 — UX naming convention

**AGREED**

For newly created local roles, the UI should propose a meaningful name automatically from the selected permission profile.

Examples:

- SENIOR → localized “Старший” / equivalent current UI language;
- LEADER → localized “Керівник групи”;
- if the same generated name already exists, append the first free numeric suffix: `Старший 2`, `Старший 3`, etc.;
- when advanced permissions diverge from the selected profile and the user has not manually renamed the role, propose `<profile> (модифіковано)` with numeric suffix if needed;
- once the user manually edits the name, automatic naming must stop overwriting it.

**AGREED**

Duplicate role names in the same group are rejected case-insensitively when entered manually.

**AGREED**

If another local role already has an identical effective permission + scope set, show a warning naming that role. This warning does not block save, because two semantically different roles may intentionally share the same permissions.

**AGREED**

New task definitions receive an automatic localized name using the first free number:

- `Завдання 1`
- `Завдання 2`
- etc.

Manual task titles remain allowed. A manually entered title identical to another existing TaskDefinition title is rejected.


---

## 28. Global predefined roles are referenced, not copied into groups

### 2026-10-04 — correction to group role mechanics

**AGREED / ARCHITECTURAL CORRECTION**

Predefined/shared roles must exist only once per Workspace as reusable `RoleDefinition` records.

Creating a MemberGroup must **not** create duplicate role rows such as LEADER, SENIOR, EXECUTOR, REVIEWER, etc.

Instead:

- the group automatically has access to the Workspace's default/shared role catalog;
- standard RoleSets/RoleDefinitions are referenced from the group context;
- assigning a standard role to a member in a group creates only a `RoleAssignment`;
- the assignment context is `MEMBER_GROUP + groupId`;
- no duplicate `GroupRole` or duplicate `RoleDefinition` is created.

Example:

`SENIOR` exists once in the Workspace.
Assigning SENIOR to Oleh in Group A and Group B creates two RoleAssignments pointing to the same RoleDefinition.

**AGREED**

A new PRIVATE RoleDefinition is created only for genuinely group-specific/custom semantics, e.g. “Відповідальний за інвентар”.

Custom/local role creation is therefore an exception path, not the normal path.

**AGREED**

Normal group UX should be:

1. create group;
2. predefined roles are immediately available;
3. add members;
4. assign existing global roles;
5. only use “Create custom role” if no existing role fits.

**INVARIANT**

RoleSet availability/binding does not clone RoleDefinitions.

RoleDefinition = reusable catalog entity.
RoleAssignment = contextual use of that role by an actor.
RoleSetBinding = contextual availability of a set.


---

## 29. Task actor model simplified to participant lists

### 2026-10-05 — remove duplicate assignment controls

**AGREED**

The ordinary task editor should not expose both assignment-policy fields and participant lists.

Remove from the normal UI:

- assignment type;
- assigned member;
- responsible member;
- preferred member / target group selection as primary assignment controls.

Use the Participants section as the visible source of task actors.

**AGREED**

Task participant constraints:

- ADMIN: MEMBER only;
- OBSERVER: MEMBER only;
- EXECUTOR: MEMBER and/or GROUP;
- executor selection may be combined: multiple members, multiple groups, or groups + individual members.

The task author remains an ADMIN automatically.

**AGREED**

Before save, validate the minimum viable task:

- non-empty title;
- valid schedule/start date;
- valid recurrence-specific values;
- at least one executor;
- reward/penalty values consistent with point types;
- ADMIN/OBSERVER actor types restricted to MEMBER.

**IMPLEMENTED**

A compatibility AssignmentPolicy.PARTICIPANTS represents explicit executor lists without widening eligibility to the whole Workspace.

If there is exactly one MEMBER executor, SINGLE_MEMBER may still be used internally so the existing concrete TaskInstance generation lifecycle continues to work.

For combined executor sets, eligibility comes from EXECUTOR TaskParticipants.

**OPEN**

The current TaskInstance lifecycle still represents one concrete executor after claim. If FamilyPoints later needs true simultaneous/shared completion by multiple executor actors, instance-level joint participation semantics must be implemented separately rather than inferred from the definition-level participant list.
