# FamilyPoints — mandatory rules for every project chat/agent

**Start here:** [docs/coordination/README.md](docs/coordination/README.md). These rules apply to Architecture / Planning, Backend, Web, Android and Release / DevOps.

## At the beginning of EVERY chat or substantial work batch
1. Check the actual remote repository branch and HEAD SHA. Do not assume `main` is current: as of 2026-10-22 the active development/integration branch was `feature/task-soft-delete`.
2. Read `docs/coordination/PROJECT_STATE.md`, `IMPACT_REGISTER.md`, `ARCHITECTURE_DECISIONS.md`, and your journal in `docs/coordination/logs/`.
3. Review commits since the SHA in your previous handoff, including work by other tracks. Read applicable root development/concept/deployment logs, checklists and actual code.
4. Explicitly review incoming **OPEN/ACK NEEDED/BLOCKED** impact records. Acknowledging means impact reviewed, not implemented or tested.
5. Never infer runtime, test or deployment status from a Git commit or chat memory alone.

## Before changing shared behavior
Record cross-track impact **before** introducing incompatible API/DTO, database/Flyway, workspace/permissions, auth, error semantics, offline sync, URL/deployment or image storage changes. Obtain consumer review or record a blocker; Architecture arbitrates cross-domain choices. Release must approve publication independently.

## After each work batch
Append a dated, evidenced record to your track journal (what changed, files/commit SHA, tests actually run, affected tracks, risks, next steps). Update impact records, shared state, and relevant root checklist; update existing historical journals for major milestones/operational changes. Commit documentation with source changes or as a separate docs commit.

## Safety/concurrency
- Preserve all historical logs; append, don't erase. Inspect HEAD before writes and resolve conflicts without force pushing.
- Prefer topic branches/PRs; coordinate edits to shared files.
- Do NOT put tokens, passwords, signing keys, private infrastructure configuration, family/child data or DB dumps in Git.
- Committed != tested != deployed. Release requires CI, compatible consumers, backup/rollback, tested signed APK and explicit gate.

[Protocol](docs/coordination/CHANGE_PROTOCOL.md) · [Handoff template](docs/coordination/HANDOFF_TEMPLATE.md)
