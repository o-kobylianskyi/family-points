# ADR-CAL-001 — Unified task and reward calendar + reward acquisition windows

**Date:** 2026-10-22  
**Status:** Product requirement AGREED; implementation contract PROPOSED for Backend/Web/Android review. No code/test/deployment claim.  
**Coordination:** IMP-0006 · XTR-20261022-P-01/02/03  
**Scope:** Calendar projection, reward *acquisition* availability; not a new points economy or screen-time engine.

## Product goal
Show the same authorized family task and reward schedule in Web and native Android, with day, week, month and arbitrary date-range views. A reward can be acquired only at permitted dates/times **in addition to**, not instead of, prerequisite tasks, reputation, balance, approval, cooldown, quota, post-obligation and blocking rules. Existing rewards have no schedule restriction until one is configured.

Examples: (a) cartoons Mon–Fri 16:00–18:00 only after homework + clean room, one purchase/day; (b) one-time movie ticket on 2026-11-05 between 18:00 and 20:00; (c) any-day, any-time reward (legacy behavior).

## Evidence from existing code (reviewed 2026-10-22, `feature/task-soft-delete`)
- TaskDefinition: ONCE/DAILY/WEEKLY/MONTHLY/CUSTOM, start/end dates, dueTime, assignment and reward/penalty fields.
- TaskScheduleSlot: stable slotKey, scheduledTime, enabled; V25 migration. TaskInstance: uniqueness by definition/member/date/slot and dueTime snapshot; V26/V32 migrations.
- TaskService.getMemberSchedule currently reads **one member on one date**, projecting virtual PLANNED and overriding with persisted instances; creation only on actual action/deadline. Web/Android consume this model.
- Current TaskScheduleSlot.scheduledTime becomes an occurrence's **dueTime/deadline**, not a guaranteed start time. Keep this invariant; do not relabel it as an execution start without a separate contract.
- RewardDefinition/RewardService have BEFORE/AFTER requirements, acquisition modes DIRECT/REQUEST/DIRECT_OR_REQUEST, approval, reputation, cooldown, per-member/workspace count limits, duration-kind, purchases and ledger; no persisted availability schedule. Both `purchaseReward` and `purchaseRequest` must enforce any new time gate.
- RewardPurchase is acquired/charged and marked READY; actual redeem/use lifecycle is separate and currently incomplete. No screen-time activation is implied.
- Workspace presently has no persisted IANA timezone; task deadlines and reward quota boundaries currently use JVM `LocalDateTime.now()`/`LocalDate.now()`. This is a **known compatibility gap**, not already solved.

## Core domain invariants
1. **Calendar view != persistence:** Build bounded, read-only projections for future TaskDefinitions/slots and reward time windows. Never pre-generate hundreds of future TaskInstances, RewardPurchases or ledger movements.
2. **TaskDefinition != TaskInstance:** Persistent historical instances/status/reward/penalty/audit win over projected planned occurrences; preserve stable existing `definitionId:scheduledDate:slotKey` identifiers and disabled-slot history.
3. **Visibility != availability != eligibility != permission:** A visible future task can be unavailable to start; an upcoming reward can be displayed but cannot be purchased; only authorized actors can view/work on a member's data.
4. **Reward timing != task conditions:** ScheduleOpenAt(now,workspaceZone) AND all normal entitlement, requirement, balance, cooldown, quota, blocking and approval checks. A schedule never bypasses mandatory tasks or grants points.
5. **Receipt != utilization:** V1 time windows gate initiating reward acquisition (purchase, including approved request). They do not by themselves force actual use to finish within the window. A separate future entitlement/use-time policy is needed for Android Device Owner or reservation use.
6. **Historical integrity:** Schedule edits change future eligibility/projections, not paid purchases, completed tasks, audit records or dueTime snapshots of materialized occurrences. No silent historical backfill.
7. **Server authority:** Backend alone makes real-time gate and time-zone decisions. Android offline calendar is cached/read-only, not permission to purchase locally.
8. **Workspace isolation:** All schedules/windows/purchases/tasks scoped by workspace (including nested ID access), no exposure to unrelated families.
9. **Time boundaries:** Intervals are half-open `[start,end)` (e.g. available 16:00:00, unavailable 18:00:00); date ranges include both configured dates. Clock/timezone must be injectable for deterministic tests.

## Reward availability model — proposed v1
- `ALWAYS`: default on every pre-existing/new reward; existing customers unchanged.
- `ONE_TIME`: one explicit instant range (`startsAt`, `endsAt`, ISO-8601 offset or UTC storage); end strictly later than start. A one-time date with no time restriction can use explicit all-day local boundaries resolved in workspace zone.
- `RECURRING_WEEKLY`: zero or more recurring local time windows with (stable `windowId`, ISO weekdays 1–7, `localStartTime`, `localEndTime`, optional inclusive `effectiveFromDate`/`effectiveToDate`, `enabled`). Selecting all seven days yields a daily schedule; more than one window may exist per day.
- V1 reject same-day `endTime <= startTime` and overlapping/duplicate windows for the same day; cross-midnight, exclusions/holidays, seasonal/complex RRULE and per-member overrides are postponed rather than guessed. No window means a disabled/restricted recurring schedule, **not** fallback ALWAYS.
- RewardDefinition owns schedule; each window belongs to one reward's workspace. Make dates, weekdays and times explicit; never infer locale from browser or server.
- Store workspace IANA `ZoneId` (e.g. `Europe/Berlin`) and use it to resolve local schedule windows and next opening. Design migration + onboarding/default policy explicitly; existing rewards and task deadline/quota behavior must not silently shift because DB timestamps currently lack timezone. Track timezone migration as a compatibility gate.
- Implement a pure evaluation service that yields `scheduleOpenNow`, `nextOpenAt`/next eligible window (if determinable), `availableUntil`, structured availability reasons and user-safe flags. In the reward catalog, distinguish `scheduleOpenNow` from `eligibleToPurchaseNow` (other requirements may still fail). Avoid leaking private task/behavior details to unauthorized viewers.
- No timer, cron, precomputation, auto-charge or auto-purchase needed just for schedule opening. Recompute on a read, and revalidate **inside the purchase transaction** against server clock.
- For catalog-based reward requests, allow submitting/reviewing a request ahead of an opening unless otherwise blocked. `READY_TO_PURCHASE` means prerequisites/approval are ready; final purchase is additionally time-gated and can temporarily be unavailable without mutating request state. Free-form reward requests without a catalog reward do not implicitly inherit a catalog schedule.
- Retain existing per-member/family quotas and cooldown; define day/week calculations based on the workspace timezone (Mon–Sun ISO weeks), with historical timestamp migration and backward-compatibility verification. Do not reinterpret old purchases silently.
- Reward `durationMinutes` remains separate from allowed *acquisition* window. If a reward must be consumed within a window, that will be a separate use-time entitlement contract.

## Task calendar / service API — proposed v1
- Preserve existing member/day `GET /workspaces/{workspaceId}/tasks/members/{memberId}/schedule?date=...` and task creation/edit/start semantics.
- New bounded calendar projection, e.g. `GET /workspaces/{workspaceId}/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD&memberId=...`. `from,to` inclusive, `from <= to`, cap e.g. 62 days in v1; paginate or bound result sizes as needed. Filters: `TASK`, `REWARD`, `ALL` and authorized member(s). Optimize batch queries rather than one endpoint request per day.
- Return discriminated events: `kind=TASK|REWARD_WINDOW`, `key`, `date`, `startsAt`/local display time, `endsAt`/dueAt as meaningful per kind, title, authorized member/workspace context, status and action capability flags. Task event reuses canonical occurrenceKey and `instanceId?; status=PLANNED|<actual>`; task `scheduledTime` reflects deadline, not start. Reward event key derived from reward+local date+window ID and is **virtual** (not a purchase ID).
- Display a reward event for future allowed windows even when the member is currently not eligible due to incomplete prerequisites; do not fabricate availability or store fictional reward instances. Limit disclosure of member financial/requirement details to authorized viewers.
- Potential separate `GET /workspaces/{workspaceId}/rewards/{rewardId}/availability` and `PUT /workspaces/{workspaceId}/rewards/{rewardId}/availability` (manager only), with schedule DTO and computed next opening. **Keep old `PUT /rewards/{id}` backward compatible**: older clients omitting the new field must not reset/erase an already stored availability schedule. Use a distinct endpoint or explicit PATCH semantics.
- Preserve existing TaskScheduleSlot `slotKey`; future changes to planned start vs deadline must be additional fields and not silently change deadline/penalty behavior.

## UI guidelines (not final styling)
- Common calendar with month/week/day/range, category/member filters, task statuses and reward windows; click/tap opens existing task details or reward details/editor.
- Reward create/edit: mode `always / one-time / repeating`, select date range and one/multiple windows, localized weekday selection and clear `family timezone` indicator. Preview next open/close and why acquisition is disabled (date/time vs tasks/points/approval).
- Child sees visible upcoming reward and accurate next availability, no actionable Buy when outside window; server refusal remains authoritative and localized.
- Web and Android use their already approved Soft Family light/dark design; do not block ongoing styling work or require shared UI code.
- Android offline projection is labelled stale/offline and cannot acquire/purchase/approve on speculative cached local time.

## Migration, verification, delivery order
1. Backend: approve DTO semantics with consumers, Flyway additive reward calendar schema / workspace timezone strategy, schedule evaluator, transactional direct/request purchase enforcement, bounded calendar endpoint, tests.
2. Web: after current styling pass, calendar/range/filter UI and reward availability editor; consume shared API without guessing server eligibility.
3. Android: after current styling pass, native Compose calendar and reward schedule display/edit subject to rights, read-only cached offline, no speculative purchase.
4. Release: migration/rollback, exact CI+client compatibility, two-family isolation, time-zone/DST and duplicate purchase tests before tagged BETA; **no deploy implied by plan**.

### Required tests
- Legacy reward still acquirable 24/7 and old client update does not erase a schedule.
- ONE_TIME, multiple weekly windows, exact edges, disabled/all-disabled windows, past/future dates, invalid/overlapping rules, next opening and DST start/end for Europe/Berlin.
- Purchase outside time rejected **both** direct and approved-request paths (including concurrent devices), with no purchase/point deduction/ledger/post-obligation; at opening, normal prerequisites, quota, cooldown, auth and point checks remain unchanged.
- Materialized past task slot status wins over virtual plan; no future write side effects; no accidental removal of historical slot or duplicate reward/penalty.
- Unauthorized cross-family member/calendar/reward ID access refused; group/open task visibility respects existing partial execution support and real permissions.
- Month view response size bounded; Web/Android light/dark, localization, reload, phone width; Android offline explicitly non-authoritative.
- Ensure Flyway upgrade and existing LocalDateTime timestamp semantics are reviewed before enforcing a new workspace timezone.

**Open questions to resolve within track reviews, not implicit changes:** timezone default/migration for existing workspaces; whether/when to add end-time use control for time rewards; overnight/repeating monthly exceptions; notification rules; task planned start separate from due deadline.

## User-selected calendar layout reference (2026-10-22)
The user selected an external iPhone-style family agenda mockup as the **composition baseline**, not a replacement for Soft Family light/dark themes. Daily dashboard, today/tomorrow switch, three contextual tiles, chronological vertical timeline, avatar stacks, no-time chores and fixed navigation should be adapted for task deadlines and reward acquisition windows. See [Calendar UI visual reference](../FAMILYPOINTS_CALENDAR_UI_REFERENCE.md). Backend ownership and DTO semantics remain unchanged by this visual decision.

## UX alignment approval (2026-10-22)
The user explicitly APPROVED the daily agenda + separate full calendar recommendations in `docs/FAMILYPOINTS_CALENDAR_UI_REFERENCE.md`. This confirms product composition and role-aware information hierarchy (three summary cards, chronological timeline, task/reward distinction, no-time tasks, native/mobile navigation, approved Soft Family themes). Backend DTO, tenant rights, timezone migration and purchase checks remain **implementation proposals** pending track-level review, not automatically approved by UX agreement.
