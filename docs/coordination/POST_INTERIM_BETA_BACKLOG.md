# FamilyPoints — AFTER_INTERIM_BETA backlog

**Feature-freeze decision: 2026-10-24.** The current goal is to stabilize and publish an interim BETA using work already underway. This file is **not a task list for immediate development**.

## Parked feature initiatives

| ID | Idea | Existing specification | Status |
|---|---|---|---|
| BKL-001 | Ready-made family templates, starter bundles and importable task/group/reward structures | [ADR-TEMPLATES-001](../architecture/ADR-TEMPLATES-001.md), IMP-0008; XTR-20261024-P-01..04 | AFTER_INTERIM_BETA |
| BKL-002 | Administrator family data reset with password confirmation, selective keep/clear checkboxes and restart initial setup | [ADR-FAMILY-RESET-001](../architecture/ADR-FAMILY-RESET-001.md), IMP-0009; XTR-20261024-P-05..08 | AFTER_INTERIM_BETA |
| BKL-003 | Adult or scoped group administrator records actual child reward receipt, factual delivery/start/end time and corrects duration/price/status with audit | RewardRequest/RewardPurchase lifecycle; existing READY/REDEEMED and ledger | AFTER_INTERIM_BETA |
| BKL-004 | Private task templates: save existing task, instantiate and assign quickly; child accepts or authorized adult records acceptance | ADR-TEMPLATES-001; current TaskDefinition/TaskInstance/Claim/Complete | AFTER_INTERIM_BETA |
| BKL-005 | Adult-controlled task execution history corrections: backdate acceptance/start/completion, re-evaluate points, correct approvals and erroneous child operations | TaskInstance history, scoped group permissions and points/reputation ledger | AFTER_INTERIM_BETA |

| BKL-006 | Personal reward templates and ready-made reward presets: reuse price, type, time/window constraints, stock/usage limits, approval and eligibility, edit before activation | Existing RewardDefinition/availability and BKL-001 family starter packs | AFTER_INTERIM_BETA |
| BKL-007 | Restore and maintain standard family currencies POINTS/COPPER/SILVER/GOLD for existing workspaces; provide idempotent onboarding defaults, configurable visibility without erasing balances; suggest + persist localized singular/few/many forms by currency and language across Web and Android | Existing point types, CurrencyFormsSettings, CurrencyFormsSection, /point-name-forms | BETA_REGRESSION_TRIAGE + FOLLOWUP |

### Currency regression and forms acceptance
- User reports that previously present GOLD/SILVER/COPPER are missing and only POINTS remains. This is a *potential regression of preexisting configuration*, not evidence that currencies were intentionally removed. Confirm whether the three point types are missing in database/API for this workspace, disabled, or hidden by Web/Android UI; compare fresh workspace vs existing and role/permission effects. **Do not insert duplicates or mutate existing balances.**
- Restore the four standard selectable types POINTS, COPPER, SILVER, GOLD for workspaces where expected, respecting intentional per-family disabling and legacy accounts. Correct backend seeding/onboarding or client filtering as root cause warrants.
- Currency quantity forms must be automatically prefilled for known built-ins, in UK/RU/EN/DE, following numeric plural rules, while allowing admin override; **user must not manually populate 12 fields per currency**. Examples uk: 1 бал, 2 бали, 5 балів; 1 мідна монета, 2 мідні монети, 5 мідних монет; 1 срібна монета, 2 срібні монети, 5 срібних монет; 1 золота монета, 2 золоті монети, 5 золотих монет. Handle 11–14, 21, 22, 25; retain configured custom forms; do not silently overwrite existing overrides.
- Current Web `CurrencyFormsSettings.jsx` already supplies **suggested but unsaved** defaults for all four built-ins; `formatCurrencyAmount.js` has built-in display fallback **only for POINTS**; Android `CurrencyFormsSection.kt` appears to initialize fields only from saved rows. Close this discrepancy in coordinated Backend/Web/Android package, then test API, actual user-visible forms and rendering rather than assuming suggestion means automatic persistence.
- Missing builtin currencies and wrong numeric labels are BETA stabilization defects if reported from installed build; automated default language-form population can be delivered alongside the fix without broadening into unrelated currency systems.

### Detailed acceptance notes for BKL-003 and BKL-005

**BKL-003 — Reward delivery by an authorized adult.** Child submits request, parent approves, purchase/debit happens once, then actual fulfillment is separately acknowledged by the child or a parent / group administrator with delegated rights scoped to that child. Record `occurredAt` (actual receipt/start/end), `recordedAt` (time entered), actor, recipient, reason, and original vs new values. Permit backdated factual delivery and correcting duration, receipt status and price. Never equate approval with delivery. Any adjustment of purchased price creates a compensating ledger entry rather than rewriting historic transactions. Guard approval/fulfillment and purchase against duplication, cross-family leaks and expired/unauthorized role scopes.

**BKL-005 — Parent correction of task history.** On a specific TaskInstance/task occurrence, an authorized family parent or delegated group manager can (1) mark accepted/in-progress on child's behalf with actual past timestamp; (2) record or amend actual start/end/completion time; (3) mark completed/approved/rejected or undo incorrect action with required reason; (4) re-evaluate earned points/reputation and create delta adjustment(s) rather than mutate/delete history; (5) review all adjustments as actor/action/time-old→new, with explicit disclosure to the child. Preserve immutable `recordedAt` and separately editable, auditable `occurredAt`. Check recurring tasks, group/shared assignments, already paid task points, streaks, time limits, approval rules and refund/recalculation policies; prevent duplicate payout, self-approval loopholes, cross-workspace access and member/group manager privilege escalation. UI should support correction from child operation history, not just editing a TaskDefinition master record. No actual child event should be silently manufactured as child-authored; display 'recorded by parent on behalf of child'.

**Release freeze:** Existing approved-reward purchase failure, logout, incorrect task completion, duplicate debit or offline synchronization defects remain present-BETA bugs, irrespective of deferred adult correction functionality. This backlog records future product features only. Do not start new cross-track XTR/IMP or source changes before the interim BETA is released and verified or the user explicitly reprioritizes.



## Intake rules
1. Minor bugs/regressions/UX adjustments to **current** behavior belong to existing stabilization work, not this deferred backlog.
2. New substantial capabilities, systemic refactors and new global initiatives go here with a short description, affected tracks and referenced prior design; **do not create new XTR/IMP or start implementation**.
3. Urgent data loss, security, tenant isolation, auth and release-blocking faults are allowed release blockers. Explain any scope impact to the user instead of silently expanding the feature list.
4. Review and prioritize parked features **after** interim BETA has actually been released and verified, or when the user expressly changes the freeze.
5. Maintain independent CI, backup/restore, release qualification, Android signed install and BETA smoke tests. No deployment is implied by this backlog.

### New ideas awaiting later triage
Append `BKL-00N — Title; value; impacted track(s); dependencies; status AFTER_INTERIM_BETA`.
