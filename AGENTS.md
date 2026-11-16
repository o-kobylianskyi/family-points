# FamilyPoints — mandatory rules for every project chat/agent

## MANDATORY ALL-TRACK CI EVIDENCE LOOKUP — 2026-11-16
- Read [CI_EVIDENCE_LOOKUP_PROTOCOL_20261116.md](docs/coordination/CI_EVIDENCE_LOOKUP_PROTOCOL_20261116.md) before deciding CI PASS/FAIL or claiming GitHub Actions unavailable. Designer independently verified historic full four-component `push` run `43934619348` on SHA `3c0cf014`; Android P0 `43893026715` 98/98 on SHA `09da723`.
- GitHub connector commit-run lookup may **only include PR-triggered runs**; empty array or empty combined statuses is *not* evidence that a `push` Actions run did not exist. Use repository branch-run listing / existing run URL and `fetch_workflow_run_jobs` with exact ID, then inspect actual executed steps. If unavailable report LOOKUP_BLOCKED, not FAIL or fabricated PASS.
- A job displayed green with its Maven/Android/Web/Compose test steps **SKIPPED** is **SCOPED**, not a full release PASS. Record candidate SHA separately from latest HEAD, run ID, event, all four component step results, CI gate type and any QA/Release holds. Historical green never automatically applies to current HEAD.
- Release must publish real push-run evidence; other tracks consume it. No `gh` requirement for the user, no routine PROD probes. CI green does not approve BKL-002 reset/restore or real V49 onboarding.

## BKL-002 P0 — Backend/QA parallel execution (user decision 2026-11-16)
- Backend owns developer-side compile and fast affected regression tests, but **hands off substantial/full integration and acceptance test packets to independent QA** rather than repeatedly requesting their execution from the operator.
- Backend commits bounded tested changes with SHA, source/test instructions, expected/negative cases; then continues the next safe independent development package while QA tests on isolated PostgreSQL and reports consolidated failures. Review QA findings each pickup; fix P0 data loss, auth, tenant or financial correctness defects immediately.
- QA owns the full test queue, exact-SHA evidence, PASS/FAIL/SKIP and retest. **Neither chat automatically executes work because another chat created a GitHub request.**
- No parallel-work exception allows BKL-002 `VERIFIED` promotion, destructive Reset/Restore execute or release without independent QA/Release gate. HTTP 423 remains until all required safety constraints pass.
- Canonical handoff: [BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md](docs/coordination/requests/BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md).

## CRITICAL CORRECTION — HARD CONTEXT LIMIT WARNING FAILED (user evidence 2026-11-16)
- A Web role chat reached ChatGPT's **maximum conversation length with no 50/75/85% warning**. The requested percentage warning plan is **not reliable** because chats are not given exact usage/remaining context counters; a hard cap can prevent any final handoff. **Do not present milestone alerts as a working guarantee.**
- **Mandatory alternative for ALL tracks (Architecture, Backend, Web, Android, QA, Designer, Release):** after **every substantial work batch** or changes in committed SHA/tests/blockers, update a **self-contained rolling continuation file** in `docs/coordination/handoffs/<TRACK>_LATEST.md` (first create it if absent) and commit it. Each user-visible completion report should link the handoff and include its latest commit SHA. A role journal alone is not a substitute for the rolling entry.
- At beginning of a newly started role chat, read and update `<TRACK>_LATEST.md`. If chat is approaching a practical length, recommend fresh chat **early, even without percentage**. Continue to issue percentage milestone notices **only if an explicitly stated defensible estimate is available**, never claim exact context capacity.
- If no new code and this is only casual chat, a full handoff update isn't necessary; for technical work, do not postpone preparation until 75% or 85%. A generated handoff in GitHub survives even an abrupt block on a chat's next message.
- If unable to commit, give a complete inline continuation snapshot immediately and state that GitHub persistence failed. Do not assume other chats receive updates automatically.
- Recovery exemplar: [Web hard-limit handoff 2026-11-16](docs/coordination/handoffs/20261116_WEB_CONTINUE_AFTER_LIMIT.md).

## MANDATORY FOR ALL FAMILYPOINTS CHATS — CONTEXT CAPACITY WARNINGS AND EARLY HANDOFF (user decision 2026-11-16)

**Applies to:** Architecture/Planning, Backend, Web, Android, QA/Testing, Release/DevOps, Designer and every future role/project chat using this repository. This is an operational rule, NOT a product feature, and takes priority over routine discussion convenience.

- **Warning milestones:** notify the user at estimated context utilization **50%, 75%, 85%, 90%, 95%, then every further 5 percentage points while possible**. Do not wait until the model can no longer generate a handoff.
- **Honesty about measurement:** chats/agents generally have **no authoritative token-usage percentage, remaining-context counter, or reliable cross-turn real-time measurement**. Never display invented exact usage percentages or assert a threshold was crossed without supporting evidence. If only a heuristic is possible, explicitly label it "**орієнтовна оцінка, не системний лічильник**", and prefer an early warning over false precision. If estimation is not credible, proactively warn that context is long and prepare a handoff without percentages.
- **When to check:** review capacity risk **at each meaningful user turn and before a large code/file/log/tool batch**. A chat cannot monitor silently or notify between user turns without a supported scheduled mechanism; do not promise automatic background alerts or messages to other chats.
- **At 50% (estimated):** brief warning; ensure current decisions, task ownership, source SHA and blockers are in durable repository docs.
- **At 75% (estimated):** warn; create/update **a real continuation handoff draft** for the current track under `docs/coordination/handoffs/` (or a track journal + HANDOFF_TEMPLATE if directory is unavailable). Include tested vs untested, latest GitHub SHA, outstanding tasks and references; link it to the user.
- **At 85% (estimated):** **finalize and commit** the self-contained handoff, verify the exact file/commit is available on `main`, and proactively recommend starting a fresh conversation. Do not postpone this until 95%.
- **At 90%, 95% and every further 5% (estimated):** short repeated warning only while this chat remains active; make sure handoff has been refreshed with any new work. If a safe handoff is not yet committed, prioritize it **immediately over optional work**.
- **Immediately on danger signs**, independent of percentage (loss of older context, repeated truncation or summaries, very long source dumps, inability to retrieve recent history): produce and commit handoff at the next possible turn; say if the handoff could not be committed.
- **Handoff must contain:** exact track and intended new-chat name, repo/branch and verified SHA/date, goal and current priorities, completed vs partially implemented vs only planned work, changed files/commits, commands and real PASS/FAIL/SKIP evidence, CI run IDs, security/release holds, issues/ADR/IMP links, current blockers, next safe actions, and a ready-to-paste **new-chat opening message**. No credentials, private family data, or tokens.
- **A new chat must begin by reading** `AGENTS.md`, this handoff and current GitHub HEAD, and reconcile drift since saved SHA. Creating a file **does not notify or automatically initialize other existing chats**: user or each role chat must explicitly retrieve the shared rule at their next pickup.
- Do not turn rough conversation length or model token estimates into claimed percentages. The milestones are **requested advisory triggers**, not a guaranteed automatic alerting service.

[Handoff template](docs/coordination/HANDOFF_TEMPLATE.md) · [Coordination hub](docs/coordination/README.md)

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
