# FamilyPoints — Concept Preservation Checklist

> Run this checklist after any substantial product/domain discussion and before ending a long design session.
>
> Goal: no important FamilyPoints mechanic should exist only in chat history.

Last updated: **2026-10-04**

---

## A. Preserve the discussion

- [ ] Was a new domain/mechanic discussed?
- [ ] Was an existing mechanic materially changed?
- [ ] Were alternatives compared?
- [ ] Was anything rejected or superseded?
- [ ] Were new invariants introduced?
- [ ] Were unresolved questions identified?
- [ ] Has the result been appended to `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`?
- [ ] Are uncertain recovered details marked `RECOVER` instead of guessed?

## B. Status discipline

For each affected concept, verify one status is explicit:

- [ ] `IMPLEMENTED`
- [ ] `PARTIAL`
- [ ] `AGREED`
- [ ] `DISCUSSED`
- [ ] `RECOVER`
- [ ] `SUPERSEDED`
- [ ] `REJECTED`
- [ ] `QUESTION`

Do not call something implemented merely because it was designed.

## C. Cross-domain separation

Check that the discussion did not accidentally merge concepts that FamilyPoints keeps separate:

- [ ] TaskDefinition vs TaskInstance
- [ ] assignment vs eligibility
- [ ] claim vs execution
- [ ] delegation vs assignment
- [ ] delegation vs subcontract/subtask
- [ ] participant role vs current executor
- [ ] task quality vs reputation
- [ ] reputation vs points
- [ ] reputation vs BehaviorDailyReview
- [ ] BehaviorEvent vs Task
- [ ] LearningActivity vs Task
- [ ] requirement/prerequisite vs eligibility
- [ ] reminder vs deadline
- [ ] visibility vs availability vs eligibility vs management permission
- [ ] organizational graph vs WorkNode hierarchy
- [ ] functional role vs security permission

## D. Task mechanics

When Task behavior changes, check:

- [ ] definition vs occurrence semantics
- [ ] recurrence
- [ ] historical backfill policy
- [ ] future visibility/calendar behavior
- [ ] assignment policy
- [ ] eligibility
- [ ] preferred actor
- [ ] claim/release
- [ ] delegation
- [ ] participants/admin/observer
- [ ] requirements/dependencies
- [ ] alternative requirements (1 of N / M of N)
- [ ] milestones/savepoints
- [ ] approval
- [ ] quality
- [ ] rewards/penalties
- [ ] reputation reward/penalty
- [ ] reminders/deadlines
- [ ] change requests
- [ ] exception/excused periods
- [ ] lifecycle/status impact

## E. Reward/economy mechanics

- [ ] `FIXED`
- [ ] `QUALITY_PERCENTAGE`
- [ ] `BASE_PLUS_QUALITY_BONUS`
- [ ] `MILESTONE`
- [ ] `PAY_IMMEDIATELY` vs `PAY_ON_TASK_COMPLETION`
- [ ] RewardGroup `ALL/CHOOSE_ONE/CHOOSE_MULTIPLE`
- [ ] manual/random/random-fill selection semantics
- [ ] selected/random result persistence
- [ ] points settlement remains idempotent
- [ ] manual award/penalty authorization
- [ ] reward access thresholds
- [ ] multiple reward/resource types

## F. Reputation mechanics

- [ ] task reputation reward/penalty
- [ ] behavior reputation reward/penalty
- [ ] manual reputation bonus/penalty
- [ ] permission to award reputation
- [ ] delegated/group-admin allocation limits
- [ ] reputation transaction/history audit
- [ ] minimum reputation requirements for rewards/purchases/bonuses/games
- [ ] whether reputation is spent or only checked as threshold
- [ ] decay policy
- [ ] inactivity decay
- [ ] reset policy
- [ ] grace period
- [ ] minimum/negative reputation policy
- [ ] exception/excused-day interaction
- [ ] group/workspace-specific policy

## G. Behavior / Learning / Streaks

- [ ] Behavior remains separate from Tasks
- [ ] positive/negative BehaviorEvent
- [ ] approval workflow for self-reported events
- [ ] BehaviorDailyReview
- [ ] behavior rewards/penalties
- [ ] streak rules
- [ ] streak freeze/excused-day semantics
- [ ] achievements
- [ ] Learning Bank Practice/Challenge
- [ ] question/checking mechanism
- [ ] difficulty/adaptive difficulty
- [ ] anti-farming limits/cooldown/repeat reward
- [ ] reading/book/chapter milestones
- [ ] learning plans

## H. Authorization and groups

- [ ] current Git authorization model inspected before changing signatures
- [ ] no permission flows upward accidentally
- [ ] subtree scope does not leak to siblings
- [ ] hidden parent metadata stays hidden
- [ ] group actor vs member actor semantics preserved
- [ ] role-based eligibility ANY/ALL considered
- [ ] group admin rights remain scoped
- [ ] any new manual award action has explicit permission
- [ ] any delegated allocation has explicit limit/budget where necessary

## I. Implementation impact

Before coding an agreed mechanic:

- [ ] inspect current GitHub branch/source
- [ ] inspect current migrations
- [ ] identify backend entity/service/API impact
- [ ] identify frontend impact
- [ ] identify DB/Flyway impact
- [ ] identify authorization impact
- [ ] identify migration/backward-compatibility impact
- [ ] identify tests/acceptance criteria
- [ ] check whether existing behavior is being superseded
- [ ] update discussion log before or alongside implementation

## J. End-of-session safety check

Before ending a long FamilyPoints discussion:

- [ ] Did anything important remain only in chat?
- [ ] Were examples mistaken for hard-coded rules?
- [ ] Were unresolved details clearly marked?
- [ ] Was any old concept replaced? If yes, was it marked `SUPERSEDED`?
- [ ] Are the latest decisions in Git?
- [ ] Can a new chat understand the decision without needing the old conversation?

If any answer above is **no**, the session is not safely preserved.
