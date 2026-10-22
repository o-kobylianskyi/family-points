# FamilyPoints — architectural / coordination decisions

Preserve older domain mechanics and discussions in `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`. A decision is intent, **not proof of implementation**. To change a decision, append a dated SUPERSEDES record.

## ADR-COORD-001 — Five work tracks (2026-10-22, AGREED)
Separate Architecture/Planning, Backend, Web, Android, Release/DevOps into five development chats. GitHub is the canonical persistent interchange; a chat does not automatically receive another chat's context. Each chat checks real Git HEAD and shared documentation on startup and writes its journal at the end.

## ADR-COORD-002 — Mandatory impact register (2026-10-22, AGREED)
A track proposing/changing another track's API, auth, data, UX conventions or release contract must register impact before implementation and obtain consumer review/ACK (or explicitly record block). Architecture arbitrates shared behavior; Release independently validates and controls deployment.

## ADR-COORD-003 — Preserve historical project logs (2026-10-22, AGREED)
Retain all three existing large history journals and both root checklists. Split routine future log entries into five topic journals, not five forks of the historical record.

## ADR-COORD-004 — Decentralized planning and cross-track technical specifications (2026-10-22, AGREED)
Every development chat is allowed and expected to plan its own work within its direction. Architecture / Planning is reserved for shared design, invariants, conflicts and inter-track prioritization rather than approving every task. A chat encountering work owned by a different direction must create a persistent, actionable cross-track technical specification (`XTR-*`) addressed to the appropriate receiving chat; split multi-owner work into linked specs. The receiving direction reviews/acknowledges, executes, tests, and records the outcome. The request lives in Git under `docs/coordination/requests/`; chat histories are not synchronized and no auto-delivery is implied. Contract-impacting changes additionally follow IMP/ADR reviews; independent Release approval remains mandatory.

## ADR-CAL-001 — Unified task/reward calendar and reward acquisition windows (2026-10-22, PROPOSED CONTRACT; GOAL AGREED)
Unified calendar presents read-only task occurrences and reward acquisition windows. Rewards may allow acquisition ALWAYS, in ONE_TIME date/time window, or in RECURRING_WEEKLY date/time windows. Time availability is an **additional** gate, not a replacement for task prerequisites, balances, reputation, approvals, cooldown, quota and blockers. Backend enforces a workspace-timezone-aware check in both direct and request purchase paths. Materialized task history and reward purchase history remain unchanged, server authoritative; no future DB bulk generation. Calendar appearance/UI is independent per Web/Compose. Full proposed compatibility and migration contract: [ADR-CAL-001](../architecture/ADR-CAL-001.md), IMP-0006, XTR-20261022-P-01/02/03. Open: timezone migration/default, separate future reward use-time policy and task start/deadline semantics. No implementation claimed.

## Earlier product decisions to respect (historical; validate code state)
- One global identity can join multiple isolated family/work workspaces, with scoped memberships/roles. See `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` and concept journal.
- Native Android client via Kotlin/Compose, not WebView; clients share Backend business semantics, not one UI codebase.
- Soft Family visual baseline chosen for Web; adapt to Android; future family cover/member photo requires deliberate Backend storage/authorization design.
- DEV/BETA/deployment separation, safe Flyway upgrades, no private family data copying, no changes to unrelated 1C/BAS/VPN forwarding.

**Next ADRs required:** media asset lifecycle/access rights; offline queue, replay/idempotency/conflict contract; API compatibility window and workspace switch semantics.
