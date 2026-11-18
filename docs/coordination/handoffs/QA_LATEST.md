# QA — LATEST / ACTIVE ASSIGNMENT (2026-11-18)

**READ FIRST:** [ARCHITECT_DISPATCH_LATEST.md](../ARCHITECT_DISPATCH_LATEST.md).
**CURRENT TASK:** [QA_ACTIVE_ASSIGNMENT.md](QA_ACTIVE_ASSIGNMENT.md).

The user starts or continues QA with only `++`; QA reads current GitHub assignments and works autonomously. Current phase is **parallel nonblocking baseline audit**, then WAIT for B `READY_FOR_QA`, then one pinned non-skipped full-package verification and one aggregated defect list. Old QA rolling handoffs and independent historical CI results are retained in Git history/Issue #15, but must **not** override these current dispatch rules.

When a real browser/phone/auth test truly requires the user, issue a **separate `OPERATOR ACTION REQUIRED`** packet and notify Architecture; do not mix with ordinary `++` assignments.

**Safety:** `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, **BETA HOLD**.
