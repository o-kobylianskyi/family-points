# FamilyPoints — cross-track change protocol

## Release-scope filter — Interim BETA freeze (2026-10-24)
Before proposing an XTR/IMP or starting implementation, classify the work: (A) **in-scope existing release stabilization** (including contract fix needed to close already underway PR), (B) **small bug or visual correction** without meaningful added product scope, or (C) **new major/global feature**. Proceed only on A/B. Route C to [POST_INTERIM_BETA_BACKLOG.md](POST_INTERIM_BETA_BACKLOG.md) with no new global XTR/IMP, PR, migration or implementation. User must explicitly reopen the next phase after verified interim BETA or approve an exception. Urgent actual data-loss/tenant-security/auth/release blockers remain in scope; document the exception. Existing template and family-reset XTR-P-01..08 dated 2026-10-24 are AFTER_INTERIM_BETA irrespective of older status. Release owns tag/DB backup/restore/handset/HTTPS gates and user release authorization.


## Testing / QA track (sixth chat)
QA consults all five development journals and creates a prioritized **test queue** under `QA_TEST_BOARD.md`, not new feature assignments. Every test must state environment (Web/Android/Backend/BETA), release SHA, prerequisites, exact manual steps, expected results and evidence. On failure, report to owning development track, mark FAIL and retest after an identifiable fix is actually deployed. No claim of successful tests without verified results. Keep new product ideas after interim BETA.

## Beginning a new chat/session
1. Fetch `origin/main` and verify its current HEAD. `feature/task-soft-delete` is a historical former integration branch; no longer use it as authoritative.
2. Read `AGENTS.md`, this directory's README, PROJECT_STATE, IMPACT_REGISTER, decisions and your track log; read applicable historical logs and source.
3. Filter impacts targeting your track. Record reviewed SHA and **ACK / BLOCKED / NOT_APPLICABLE** in the track log and impact entry; ACK only means reviewed.
4. Inspect source/DTO/DB/CI and intervening commits from **all** tracks before implementing changes.

## Decentralized planning and routing a task to another track
1. **Plan where the work arises:** Backend, Web, Android, Release and Architecture each independently scope, design, sequence and estimate their own work. Escalate to Architecture only for common domain invariants, ownership disputes, shared contracts or strategic trade-offs.
2. **Check ownership:** if the necessary implementation falls outside the current track, do not make an unreviewed cross-track change. Create a separate actionable `XTR-YYYYMMDD-<origin>-NN` specification under `docs/coordination/requests/` using `TASK_TEMPLATE.md`. Assign one accountable receiving track per task. Split multi-track projects into linked dependent XTRs.
3. **Specify sufficiently to execute:** problem, user-visible result, expected behavior, in/out-of-scope, existing source/branch/SHA, owned files/modules if known, DTO/API/data/security concerns, dependencies, acceptance tests, risks, and handoff/status. Distinguish proven current behavior from proposals. Do not prescribe another track's internal implementation unnecessarily.
4. **Publish + communicate:** commit the request in Git, link its exact path/ID/commit in the origin journal and user-visible reply. The target chat does not receive automatic notifications: the user may paste the link into the target chat, or the target must review pending requests addressed to it when starting work.
5. **Receive + close:** target reads Git at current HEAD, responds by updating request status (ACKNOWLEDGED / NEEDS_CLARIFICATION / BLOCKED, then IN_PROGRESS / DONE), logs reviewed SHA and the change/test evidence in its own journal, and links the implementing commit. Source track verifies the accepted behavior and closes its dependency as appropriate. ACK means scope accepted, not code completed.
6. **Contract changes require both mechanisms:** use XTR for the task handoff and `IMP-XXXX` for any externally affecting API, schema, auth, offline sync or runtime contract; required consumer ACKs, compatibility/rollback and Release gate still apply. Architecture can arbitrate conflicts but is not a universal bottleneck.

## Proposed impacting change
Before altering a shared contract, create `IMP-XXXX` in IMPACT_REGISTER with producer/date/old→new behavior/status, breaking or compatible classification, affected consumers, link/commit/ADR, required consumer actions, test plan, release gate. Architecture resolves disputes and maintains cross-domain priorities. No silent breaking changes.

Affected contracts include REST paths/payloads/errors, JWT and workspace membership, Flyway migrations, task/economy business rules, offline queue/idempotency, image/cover/avatar endpoints and permissions, translated enums, base URLs, deep links, environment flags, Android package version/signature, deployment/rollback.

## Implementing and integrating
Accept changes into the single canonical `main` branch. Concurrent or unverified work may use temporary `feature/backend-*`, `feature/web-*`, `feature/android-*`, `chore/release-*` branches, with PRs always **targeting `main`**. Confirm consumer reviews and CI, merge ready work, then refresh all local branches. Before branch switch, preserve unpushed commits and local work according to [MAIN_BRANCH_MIGRATION](MAIN_BRANCH_MIGRATION.md). Never force push or reset local work to resolve conflicts.

Producer records commit SHA and real test evidence; every consumer records review/required migration in own log and acknowledges the impact; impact remains OPEN if any mandatory action or integration test is missing. Backward-compatible API migration should precede removing old payloads.

## Release gate
Release independently verifies tagged SHA, CI and tests, DB migration safety, 2-workspace authorization, Web and Android compatibility, secrets/environment setup, backups/rollback, signed APK upgrades, smoke tests and actual public endpoints. A source commit is not a deployed BETA. Preserve DEV/WireGuard/Contabo's unrelated 1C/BAS routing and existing user data.

## End of work / next chat
Update track log, impact register, shared PROJECT_STATE and checklist as relevant; commit and provide branch/SHA + [handoff](HANDOFF_TEMPLATE.md). Never pretend another chat has already received the update: the next session must fetch it.

## Default development workflow (approved 2026-10-23)
For routine scoped changes all five tracks work directly on `main`, refreshing current HEAD and checking overlapping edits first. Separate branches are exceptional and require a documented reason such as risky experiments, incompatible shared contracts or migrations; any PR targets `main`. The earlier permission to use topic branches is not a requirement to create them. Preserve local unpublished commits before switching branches or merging. The user-approved Home dashboard images, layout and hover styling are frozen unless the user explicitly requests changes. Direct commits still require tests, contract review, security checks and independent Release approval.
