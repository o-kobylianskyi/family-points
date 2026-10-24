# FamilyPoints — mandatory rules for every project chat/agent

## TEMPORARY MANDATORY PRIORITY — INTERIM BETA FEATURE FREEZE (user decision 2026-10-24)
- **Goal:** stabilize and release the CURRENT FamilyPoints functionality as an interim BETA before any new global product initiatives.
- **Continue:** in-progress accepted Backend PR #4 calendar and PR #5 media as feasible for a safely qualified release, existing Web/Android integration, auth/offline/data integrity, regression fixes, testing, migrations, backups, signed APK and BETA smoke. Release decides what is actually ready; never equate committed, CI-passing, or planned with deployed.
- **Allowed:** small UI corrections and bugs in existing behavior. Urgent security, tenant isolation, data loss or release-blocking failures remain in scope even if complex; document and communicate scope implications.
- **STOP creating new global tasks:** new modules, large features and broad refactors are recorded in [POST_INTERIM_BETA_BACKLOG](docs/coordination/POST_INTERIM_BETA_BACKLOG.md) only; no new cross-track XTR/IMP or implementation unless user explicitly reprioritizes.
- **Explicitly PARKED:** Family Templates and initial setup expansion (IMP-0008 / XTR-20261024-P-01..04); selective Family Reset (IMP-0009 / XTR-20261024-P-05..08). Their prior READY/PLANNED labels are overridden by **AFTER_INTERIM_BETA**.
- This freeze overrides earlier broad roadmap priorities, not safety and independent Release approval. Review it at the start of all five chat tracks.


## Sixth coordination chat — Testing / QA (2026-10-24)
**Owner:** dedicated user-facing chat `Тестування і задачі для тестування`, operating under the existing interim-BETA scope freeze. QA coordinates **manual and acceptance testing of existing functionality only**; it does not create new global feature XTR/IMP or claim implementation/deployment. Canonical source is `origin/main`; feature branches may be tested separately but must be labeled UNMERGED and not available in BETA unless independently verified. QA always compares latest backend/web/android/release logs, current PRs, CI and actual deployed version when known.
**Files:** [QA log](docs/coordination/logs/qa.md), [live manual test board](docs/coordination/QA_TEST_BOARD.md), [test case catalog](docs/coordination/QA_TEST_CASES.md). On every pickup refresh evidence and classify cases READY_TO_TEST / BLOCKED / PASSED / FAILED / NEEDS_RETEST / NOT_VERIFIED. Any success needs environment/version/device and tester evidence. Failed cases route to existing Backend/Web/Android/Release track logs with reproduction, not a new global project.

**Start here:** [docs/coordination/README.md](docs/coordination/README.md). These rules apply to Architecture / Planning, Backend, Web, Android and Release / DevOps.

## At the beginning of EVERY chat or substantial work batch
1. **Canonical project branch is `main`.** Fetch `origin/main` and review its real SHA before every work batch. Do not use former `feature/task-soft-delete` or a track branch as the shared source of truth.
2. Read `docs/coordination/PROJECT_STATE.md`, `IMPACT_REGISTER.md`, `ARCHITECTURE_DECISIONS.md`, and your journal in `docs/coordination/logs/`.
3. Review commits since the SHA in your previous handoff, including work by other tracks. Read applicable root development/concept/deployment logs, checklists and actual code.
4. Explicitly review incoming **OPEN/ACK NEEDED/BLOCKED** impact records. Acknowledging means impact reviewed, not implemented or tested.
5. Never infer runtime, test or deployment status from a Git commit or chat memory alone.

## Main-only integration policy (ADR-COORD-005, 2026-10-23)
- The whole FamilyPoints monorepo — Java/Backend, Web/React, Android/Compose, architecture docs and DevOps — has exactly one accepted integration branch: **`main`** (also the GitHub default).
- Unfinished features can use temporary topic branches and PRs, but PRs target `main`. After review/test, integrate promptly into `main` and refresh all chat checkouts from `origin/main`. Do not keep a second permanent integration branch.
- Before any local branch switch or merge, preserve unique local commits and uncommitted/untracked/important ignored files, especially the Android branch reported to contain 18 local-only commits. **Never use `reset --hard`, `clean -fd`, force-push, or delete unique work** as a quick fix. Follow [safe branch consolidation](docs/coordination/MAIN_BRANCH_MIGRATION.md).
- `main` commits are source integration, not DEV/BETA/PROD deployment approval. Signed APK and Release tag gates remain independent.

## Local planning and cross-track task routing
- **Planning is decentralized:** every track may analyze, design, prioritize and plan tasks within its own responsibility. Do not require Architecture approval for routine track-local plans.
- If work is outside this track, **create a written technical specification for the owning track** instead of silently implementing its code. If work spans tracks, split it into clear owner-specific deliverables and dependencies.
- Save cross-track requests as separate `docs/coordination/requests/XTR-YYYYMMDD-<origin>-NN.md` files using [the request template](docs/coordination/requests/TASK_TEMPLATE.md). Include context, current branch/SHA, required behavior, API/contracts where relevant, acceptance criteria, dependencies, tests, status and impacted tracks. Give the user a readable handoff/link for the target chat.
- The receiving chat must retrieve the request from GitHub, review/acknowledge it or document a blocker, own its implementation and record evidence/commit in its track log. **Chats do not notify one another automatically.**
- Architecture is consulted for shared invariants/contracts, unclear ownership, conflicts and strategic trade-offs only; Release retains independent deployment approval. An `XTR` task does not replace the mandatory `IMP-XXXX` impact record for shared/breaking contracts.

## Before changing shared behavior
Record cross-track impact **before** introducing incompatible API/DTO, database/Flyway, workspace/permissions, auth, error semantics, offline sync, URL/deployment or image storage changes. Obtain consumer review or record a blocker; Architecture arbitrates cross-domain choices. Release must approve publication independently.

## After each work batch
Append a dated, evidenced record to your track journal (what changed, files/commit SHA, tests actually run, affected tracks, risks, next steps). Update impact records, shared state, and relevant root checklist; update existing historical journals for major milestones/operational changes. Commit documentation with source changes or as a separate docs commit.

## Safety/concurrency
- Preserve all historical logs; append, don't erase. Inspect HEAD before writes and resolve conflicts without force pushing.
- Pull/review `origin/main` before accepting work. Temporary topic branches/PRs target `main`; coordinate edits to shared files.
- Do NOT put tokens, passwords, signing keys, private infrastructure configuration, family/child data or DB dumps in Git.
- Committed != tested != deployed. Release requires CI, compatible consumers, backup/rollback, tested signed APK and explicit gate.

[Protocol](docs/coordination/CHANGE_PROTOCOL.md) · [Handoff template](docs/coordination/HANDOFF_TEMPLATE.md)

## Development branch preference — approved 2026-10-23
- Routine work by Web, Backend, Android, Architecture and Release goes directly to `main`. Do not create separate track branches by default.
- Temporary branches are exceptional, for risky changes, experimental work, coordinated migrations or unfinished incompatible contracts. Document the reason and target `main` for integration.
- Check HEAD and protect local unpublished commits before changes. Preserve approved Home dashboard visuals and manually edited assets without explicit user authorization to alter them.
- Existing contract reviews, tests and Release approval still apply. GitHub changes alone do not migrate the user's local Android checkout.
