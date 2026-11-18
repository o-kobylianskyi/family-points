# FamilyPoints — CURRENT ARCHITECT DISPATCH (read FIRST on every `++`)

**Effective:** 2026-11-18 18:26 CET. **Authoritative control for live workstream activation and assignments.** Owner communicates primarily with Architecture; owner starts role chats by sending only `++`. GitHub is the durable coordination queue. The user need not copy technical assignments into chats.

## Mandatory stream bootstrap and every subsequent `++`
1. Read this file from `main` **at the start of every active turn**. Then read only your current role assignment file below, Issue [#15](https://github.com/o-kobylianskyi/family-points/issues/15) for the latest architect directive, and relevant source/CI. **Do not ask owner to paste tasks.**
2. Perform the **largest safe substantive portion** of the currently assigned work autonomously. `++` = continue the latest active assignment; not permission for production data deletion, deployment, reset, destructive backup retention, or release.
3. Record actual changed files, commit SHA, targeted test evidence, incomplete/blocked tasks, and a short handoff **in your role assignment or an append-only linked handoff**, plus concise Issue #15 comment for cross-stream blocker/READY_FOR_QA. No docs-only loop if implementation is expected.
4. **Streams cannot wake/activate each other or work in background**. They hand off asynchronously in GitHub; **Architecture** tells owner which chat to start/continue/close. No stream should assume a GitHub comment itself starts a role.
5. When blocked by another stream: post one precise blocker with file/contract/SHA, stop only the dependent portion, work on independent subitems; do **not** ask owner to coordinate, and do not silently weaken security.
6. The only owner-visible manual-action instruction is a **separate, explicit operator request**. See protocol below.

## CURRENT stream status and routing
| Stream | Owner instruction | Canonical work file | Condition |
|---|---|---|---|
| Architecture | ACTIVE: sole coordinator/acceptance router | **this file** | Coordinates dependency gates and calls activation |
| Backend B | **ACTIVATE / CONTINUE** with `++` | [BACKEND_B_ACTIVE_ASSIGNMENT.md](handoffs/BACKEND_B_ACTIVE_ASSIGNMENT.md) | Deliver B-INT-02 big implementation; no micro gates |
| QA | **ACTIVATE / CONTINUE** with `++` for baseline audit, then WAIT until B READY_FOR_QA | [QA_ACTIVE_ASSIGNMENT.md](handoffs/QA_ACTIVE_ASSIGNMENT.md) | Parallel nonblocking baseline; full gate after B handoff |
| Backend A | CLOSED | historical only | All mandatory tasks inherited by B |
| Backend C | CLOSED | historical only | All mandatory tasks inherited by B |
| Backend D | CLOSED | historical only | All mandatory tasks inherited by B |
| Web / Android / Designer / Release | WAIT; use existing active chats only if explicitly activated by Architecture | historical role files | No idle `++` calls required |

## Step and dependency graph
**Stage 01 (parallel NOW):** Backend B implements B-INT-02 as ONE large autonomous package; QA independently reviews current CI/known failures and prepares consolidated acceptance cases. QA does **not** stop B or require a full CI per subcommit.

**Gate A — Backend B READY_FOR_QA**: one coherent reviewed working tree/main SHA including complete agreed functionality, targeted compile/PG tests, supported/blocked categories and migration/backward-compatibility report. Only this state activates QA's full-package test phase. A code-committed partial archive is not READY_FOR_QA.

**Stage 02 (QA):** one pinned non-skipped exact-SHA Java 21/Postgres18 + Web/Android/Compose + full attestation run; disposable source-db-loss E2E with production security implementation path (never real family). Output **one aggregated defect report** by severity, reproduction, first stack, ownership.

**Stage 03 (Backend B):** one large corrective pass for all reproducible defects; focused checks, then Stage 02 exact SHA rerun. Independent fixes can be committed separately, not accepted as one unsafe monolith.

**Gate B — Release eligibility:** only after QA independently accepts full end-to-end security/finance/fail-closed evidence, Architecture instructs Release and (if needed) Web/Android real device acceptance. **No auto-production unlock**.

## Operator-only protocol — ALWAYS separate from ordinary updates
When genuine human action is needed, author a dedicated note with:
- `OPERATOR ACTION REQUIRED` and clear issue ID;
- why automation/synthetic tests cannot establish evidence;
- whether it is disposable DEV-only or touches live assets (live writes need distinct explicit approval);
- exact step-by-step instructions, expected result, rollback/safety, data the operator must return;
- **which stream** receives the response (normally QA) and what Architecture will do on receipt.
Do NOT blend manual test instructions into normal task reports; do NOT ask operator to activate streams just to forward tool output. Never use a real family for destructive testing absent explicit separate approval.

## Safety / evidence contract
Current production Restore `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, **BETA HOLD**. No live family mutation, deployment, signed APK publication or destructive backup-retention purge authorized. Latest historically verified all-four baseline in earlier QA-068: #44290621478 @`89b0f8e1`, Maven 749/749; more recent historical v2 implementation and QA fixes are **not** automatically covered. Before stating a new full PASS, inspect actual non-skipped jobs and exact SHA, not a green badge.

**Architecture updates only one current dispatcher and role work file when stage/owner changes.** Avoid stale LATEST pointers: this file supersedes older A/B/C/D splitting language and older micro-package/full-ci-every-commit instructions.
