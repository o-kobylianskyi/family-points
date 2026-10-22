# FamilyPoints — architectural / coordination decisions

Preserve older domain mechanics and discussions in `FAMILYPOINTS_CONCEPT_DISCUSSION_LOG.md`. A decision is intent, **not proof of implementation**. To change a decision, append a dated SUPERSEDES record.

## ADR-COORD-001 — Five work tracks (2026-10-22, AGREED)
Separate Architecture/Planning, Backend, Web, Android, Release/DevOps into five development chats. GitHub is the canonical persistent interchange; a chat does not automatically receive another chat's context. Each chat checks real Git HEAD and shared documentation on startup and writes its journal at the end.

## ADR-COORD-002 — Mandatory impact register (2026-10-22, AGREED)
A track proposing/changing another track's API, auth, data, UX conventions or release contract must register impact before implementation and obtain consumer review/ACK (or explicitly record block). Architecture arbitrates shared behavior; Release independently validates and controls deployment.

## ADR-COORD-003 — Preserve historical project logs (2026-10-22, AGREED)
Retain all three existing large history journals and both root checklists. Split routine future log entries into five topic journals, not five forks of the historical record.

## Earlier product decisions to respect (historical; validate code state)
- One global identity can join multiple isolated family/work workspaces, with scoped memberships/roles. See `docs/MULTI_FAMILY_ONBOARDING_PLAN.md` and concept journal.
- Native Android client via Kotlin/Compose, not WebView; clients share Backend business semantics, not one UI codebase.
- Soft Family visual baseline chosen for Web; adapt to Android; future family cover/member photo requires deliberate Backend storage/authorization design.
- DEV/BETA/deployment separation, safe Flyway upgrades, no private family data copying, no changes to unrelated 1C/BAS/VPN forwarding.

**Next ADRs required:** media asset lifecycle/access rights; offline queue, replay/idempotency/conflict contract; API compatibility window and workspace switch semantics.
