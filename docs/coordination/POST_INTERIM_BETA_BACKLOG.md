# FamilyPoints — AFTER_INTERIM_BETA backlog

**Feature-freeze decision: 2026-10-24.** The current goal is to stabilize and publish an interim BETA using work already underway. This file is **not a task list for immediate development**.

## Parked feature initiatives

| ID | Idea | Existing specification | Status |
|---|---|---|---|
| BKL-001 | Ready-made family templates, starter bundles and importable task/group/reward structures | [ADR-TEMPLATES-001](../architecture/ADR-TEMPLATES-001.md), IMP-0008; XTR-20261024-P-01..04 | AFTER_INTERIM_BETA |
| BKL-002 | Administrator family data reset with password confirmation, selective keep/clear checkboxes and restart initial setup | [ADR-FAMILY-RESET-001](../architecture/ADR-FAMILY-RESET-001.md), IMP-0009; XTR-20261024-P-05..08 | AFTER_INTERIM_BETA |

## Intake rules
1. Minor bugs/regressions/UX adjustments to **current** behavior belong to existing stabilization work, not this deferred backlog.
2. New substantial capabilities, systemic refactors and new global initiatives go here with a short description, affected tracks and referenced prior design; **do not create new XTR/IMP or start implementation**.
3. Urgent data loss, security, tenant isolation, auth and release-blocking faults are allowed release blockers. Explain any scope impact to the user instead of silently expanding the feature list.
4. Review and prioritize parked features **after** interim BETA has actually been released and verified, or when the user expressly changes the freeze.
5. Maintain independent CI, backup/restore, release qualification, Android signed install and BETA smoke tests. No deployment is implied by this backlog.

### New ideas awaiting later triage
Append `BKL-00N — Title; value; impacted track(s); dependencies; status AFTER_INTERIM_BETA`.
