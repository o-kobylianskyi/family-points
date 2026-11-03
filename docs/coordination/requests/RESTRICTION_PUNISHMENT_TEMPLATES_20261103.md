# BKL-REWARD-LOCKS — Future punishment templates and conditional restrictions (Issue #16 extension)

**Date:** 2026-11-03. **Product source:** user requests configurable punishments, templates, blocks for reward groups (cartoons, games, all gadgets), unlocking by time or completed tasks. **Priority:** P2 / POST-BETA; no impact on current BKL-002 P0 release gate. **Related:** GitHub Issue #16, BKL-008/009/010, family economy penalties.

## Architecture placement
Use a separate **Restriction / Consequence** domain (parent-initiated policy), linked to penalty transaction or task incident when applicable. Do **not** model restrictions only as negative point transactions: a reward lock has duration/condition, eligibility and immutable actor audit independent of balances. A penalty may create zero, one or several restriction instances, without forcing deduction of points.

## Template and instance semantics
- `ConsequenceTemplate`: id, workspaceId (and optionally curated global catalog), localized title/description, target type (REWARD, REWARD_CATEGORY, REWARD_GROUP, DEVICE_SCOPE), conditions, target members/eligibility defaults, severity label informational only, version, active flag; admin can create/edit/duplicate/delete own templates. Editing template must NOT retroactively rewrite active instances.
- `RestrictionInstance`: createdBy, targetMember(s), reward identifiers or category tree, optional trigger penaltyId/reason, effectiveAt, state ACTIVE/EXPIRED/COMPLETED/REVOKED, startDataEpoch, unlockCondition immutable snapshot, completedAt/revokedAt, authority audit. Multiple matching restrictions combine as **deny if ANY active restriction**.
- **Target examples**: all cartoon rewards; game/time rewards; all eligible device-related rewards; one specific reward; a user-defined reward category/group. Use existing RewardCategory when sufficient. `DEVICE_SCOPE` is a **policy representation**, not proof Android can enforce an OS app lock.
- Unlock policy: `AT_DATETIME` (workspace timezone, DST-safe server Instant); `MANUAL_PARENT`; `TASK_REQUIREMENT` with specifically bound TaskInstances/occurrences and ANY/ALL; optionally `OR` / `AND` composition (e.g. wait 24h OR complete two tasks; wait 24h AND finish tasks). For MVP only one mode per instance or explicitly supported compositions; arbitrary recursive DSL deferred.
- A template may specify default 1 day / 3 days / 1 week, but parent reviews the exact activation and can adjust deadlines/required tasks. Zero/unbounded durations not silently inferred.

## Control and enforcement
- Restriction is **server-authoritative** in all reward mutation paths: request/purchase/approve/redeem/start, with clear restrictions as 403/409 and remaining time / required task summary; **no points debited** if blocked; write check and debit in same transactional fence.
- Active restricted reward remains **visible** to the child with badge, explanation, condition and countdown, rather than vanishing. Private parent note stays hidden.
- Time expiry checked against server UTC instant; on-demand evaluation plus scheduled cleanup; no reliance solely on background jobs. When tasks are reviewed/revoked, condition evaluator must recalculate safely. Distinguish completion from mere task submission.
- Offline Android: if restriction state is unknown/stale, cannot queue a new reward purchase or start an app reward as confirmed. Existing offline point operations remain separate.
- Device control: regular Android cannot promise whole-device or individual-app hard blocking. Actual OS-level enforcement requires BKL-010 (Device Owner / managed device), explicit device provisioning/capability and QA; otherwise policy blocks FamilyPoints reward entitlement but not physically launching cartoons/games/apps.

## Integration and retention
- Penalty / negative points UI may offer 'Add consequence' from a saved template as an independent optional action. Parent can issue restriction with no point penalty. Avoid automatic punitive escalation, stacking surprises, restrictions to essential communications, emergency calling, accessibility needs or core family contact; device controls must preserve safety exemptions.
- Distinguish restriction from the 30-second cooldown, availability window, reward usage quota and BKL-008 time countdown. New restrictions do not revoke already earned/delivered rewards or refund expenses unless a separate approved policy exists.
- Reward History: show issued/expired/revoked restriction events in a separate moderation/consequences timeline, not as purchase or redemption. Reason/actor access scoped.
- BKL-002 backup/reset/restore: include restriction templates and instances as explicit independently selectable categories with dependency-aware FK graph; FULL clears family-local instances/templates, security audit retained; SELECTIVE default should be **RETAIN** until matrix explicitly reviewed, to avoid unexpected unblock of children's limitations. After restore, active restrictions re-evaluate due times and completion states without changing their semantic identities.

## UI and QA
- Web/Android parent view: 'Consequences / restrictions' with template library, create from template, choose child(ren), reward group(s), unlock mode, due time or exact tasks, preview and explicit confirm; active restrictions list with edit/revoke (audited).
- Child view: visible limited rewards, why blocked, remaining time and tasks to complete, no privileged actions.
- Four locales UA/RU/DE/EN; gentle neutral presentation, avoid shame badges or punitive illustrations.
- QA: category descendants, overlapping restrictions, deadline in family timezone/DST, task ALL/ANY, approval rollback, refund absence, denial on server concurrent purchase, child ACL, two-family account, offline stale, no unintended unlock after category rename or task edit, backup/restore, tenant isolation.

## Phase order
1. Future architecture contracts, reusable templates and family-scoped domain.
2. Backend restrict/policy engine + server enforcement + tests.
3. Web & Android UX with read-only state and manager creation.
4. Optional BKL-010 managed device enforcement in separate phase.

**No live family restrictions are created or enforced by this document.**
