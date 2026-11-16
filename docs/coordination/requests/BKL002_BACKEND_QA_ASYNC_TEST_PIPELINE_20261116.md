# BKL-002 — Parallel Backend implementation and independent QA validation

**Decision:** approved by project owner 2026-11-16. **Status:** READY_FOR_PICKUP. **Tracks:** Backend (producer, remediation), QA/Testing (test owner), Architecture (contracts), Release (final gate). **Priority:** P0. **Baseline:** GitHub main as of 2026-11-16; always re-fetch real SHA at pickup. **Scope:** BKL-002 and applicable cross-track development/test handoffs.

## Operating principle

Backend **must not wait for user-executed full QA bundles** before developing the next independent bounded P0 package. Send committed, self-contained test cases and executable instructions to QA and continue feature implementation in parallel. QA owns test queue, environments, consolidated executions, independent result interpretation and failure reports. Backend reviews new QA reports on pickup, repairs critical defects, adds regression coverage, and continues its roadmap. Keep commit/tested/accepted distinctions explicit.

### Backend responsibilities (cannot delegate)
1. Before handing off, ensure code compiles and run **developer-side fast checks** proportional to change (affected unit/compile tests, deterministic negative tests). For schema/FK/security/destructive implementation changes, run relevant local Testcontainers integration tests when feasible; a broken or uncompiled packet is NOT ready for QA.
2. Commit implementation, tests/fixtures and bounded contract; record SHA, affected classes/migrations, exact command and real PASS/FAIL/SKIP or NOT_RUN. If local execution impossible, report NOT_RUN and blocker; do not mark as verified.
3. Write QA packet: expected outcome, synthetic/disposable environment and dataset, commands/configuration (without credentials), positive and negative cases, tenant isolation/rollback/financial invariants, idempotency, known blockers, test IDs, severity and links. **Batch packages** rather than asking the user to execute every small test.
4. Mark packet `READY_FOR_QA` in a durable entry or QA board, alert via issue comment; then **continue implementing the next safe independent package**. No fictitious automatic other-chat execution.
5. Inspect QA findings at each work pickup. On FAIL P0 (tenant isolation, unauthorized destructive access, data loss, financial corruption, proof mismatch), prioritize immediate fix, create regression test and hand back SHA. Lesser failures are grouped into repair batch; do not blindly block unrelated work.
6. Never call initial source-only / local-unit / targeted-test PASS an integrated or release-gate PASS.

### QA responsibilities
1. Pick up new `READY_FOR_QA` packets from GitHub/board; triage and **independently** run full integration/regression and edge-case suites on pinned SHA where an authorized isolated environment is available. QA can ask operator for truly external secrets/infrastructure if needed; avoid repeated interactive test requests for every Backend commit.
2. Maintain queue statuses: `READY_FOR_QA`, `IN_QA`, `PASS`, `FAIL`, `BLOCKED_ENV`, `NEEDS_RETEST`, `STALE_SHA`. Include exact SHA, environment, commands, results, CI links. `BLOCKED_ENV` never counts as success.
3. Report failures in one consolidated evidence-rich batch to existing Backend issue #15 and coordination record; distinguish source/test-fixture defect vs product defect. Failed tests do not disappear from acceptance if Backend proceeds with other work.
4. Re-test patches plus neighboring authorization, FK, ledger, epoch/idempotency regressions; coordinate Release on only fully accepted scope.

### Nonnegotiable stop gates
- Backend may proceed **in parallel** with QA for **independent, non-destructive implementation**, but it must STOP unsafe activation/promotion, destructive/reset/restore execute, or any BETA publication until QA and Release signoff.
- If a failing P0 test proves actual lost data, unauthorized cross-tenant action, ledger corruption or broken atomicity, it **blocks merging or enabling the affected unsafe functionality**, not just publication; stop that change and fix it promptly.
- `VERIFIED` archive requires real supported-category disposable replay, integrity/FK/ledger proof and durable evidence, never a synthetic flag. Real workspaces/Test/Test2/PROD must not be destructively tested without explicit authorization.
- `reset/execute` stays HTTP 423 until owner/fresh reauth, 30-day protection, complete dependency closure, audit, atomic receipts, isolated tested backup/restore, Web/Android compatibility and release gates pass.
- Full manual four-component same-SHA CI and browser/authenticated E2E remain final Release checks; normal selective CI green can skip heavy jobs.

## Suggested immediate work split
- **Backend continues:** financial/ledger/task/reward recovery closure and remap, production-safe reusable native restore, VERIFIED state machine, owner fresh reauth, export/import security, reset executor.
- **QA works asynchronously:** replay of delivered V51/V52 staging/lease and cross-DB tests, verify 10/10 known operator suite and negative cases, maintain 25-case PostgreSQL and browser suites, run disposable restore as implementation arrives.
- **Release:** no deploy; prepare tagged exact-SHA gate and safeguards after QA signoff.

### One-line QA packet schema
`BKL002-QA-YYYYMMDD-NN | source SHA | scope/categories | env | test command | expected PASS and negative cases | developer preflight result | QA status | evidence URL | Backend remediation SHA`

## Acceptance
- [ ] Backend acknowledges and logs first QA packet with commit/test evidence.
- [ ] QA acknowledges, adds test IDs and posts independent PASS/FAIL/BLOCKED_ENV.
- [ ] Backend keeps coding non-dependent work instead of waiting for operator command; any FAIL tracked and remediated.
- [ ] No unsafe release/VERIFIED/Execute claim absent independent evidence.

**Communication:** GitHub is the durable queue; individual role chats have to pick up this document explicitly or during their normal read of AGENTS.md. Writing this document does not send real-time messages to the chats.
