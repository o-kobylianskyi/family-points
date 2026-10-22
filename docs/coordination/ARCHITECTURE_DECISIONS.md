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

## ADR-UX-CAL-001 — Agreed FamilyPoints calendar composition (2026-10-22, AGREED)
The user selected an iPhone-style family agenda reference and explicitly approved the proposed FamilyPoints adaptation. **Today/Home** is a daily agenda: three context-aware summary tiles, Today/Tomorrow, chronological timeline for TASK deadlines and REWARD_WINDOW opportunities, authorized member avatars, separate no-time tasks and accessible bottom navigation. The **Full Calendar** is a separate day/week/month/range entry, with member/type filters. Child/parent visibility and available actions follow server permissions. Navigation default: Home / Calendar / Tasks / Rewards / Family, Settings through profile without removing existing functionality. Preserve approved Soft Family light/dark palettes, native Kotlin/Compose and React implementations; do not copy fictional Inbox/events or iOS chrome. Source and acceptance: `docs/FAMILYPOINTS_CALENDAR_UI_REFERENCE.md`. Implementation, API contract and device acceptance are still pending (ADR-CAL-001, IMP-0006, XTR-20261022-P-02/03).

## ADR-MEDIA-001 — Workspace-owned media and PostgreSQL binary storage (2026-10-23, AGREED by Architecture; CONSUMER ACK PENDING)
Account-global identity avatars are separate/deferred. V1 family COVER/ICON, workspace member AVATAR, people MemberGroup ICON, WorkNode(GROUP) category ICON, and TaskDefinition ICON override are private workspace-scoped bindings; task override falls back to nearest task group then curated default. Store normalized image bytes (BYTEA variants) and metadata in PostgreSQL, with safe upload/re-encode/EXIF stripping, scoped read/assignment rights, catalog defaults, optimistic updates, delayed garbage collection and DB backup/restore. Web's 11 reference artwork assets remain a separate static-image delivery task; they do not require the upload API. Exact schema, API shape, quota and Release capacity review pending. Full decision: [ADR-MEDIA-001](../architecture/ADR-MEDIA-001.md), IMP-0007, XTR-20261022-B-01 / XTR-20261022-W-01. No Backend implementation claim.

## Earlier product decisions to respect (historical; validate code state)
- One global identity can join multiple isolated family/work workspaces, with scoped memberships/roles. See `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` and concept journal.
- Native Android client via Kotlin/Compose, not WebView; clients share Backend business semantics, not one UI codebase.
- Soft Family visual baseline chosen for Web; adapt to Android; future family cover/member photo requires deliberate Backend storage/authorization design.
- DEV/BETA/deployment separation, safe Flyway upgrades, no private family data copying, no changes to unrelated 1C/BAS/VPN forwarding.

**Next ADRs required:** offline queue, replay/idempotency/conflict contract; API compatibility window and workspace switch semantics. Media lifecycle is specified in ADR-MEDIA-001; consumer/Release reviews remain pending.
