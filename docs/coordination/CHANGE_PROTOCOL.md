# FamilyPoints — cross-track change protocol

## Beginning a new chat/session
1. Verify repository branch and current remote HEAD; compare to last reviewed SHA. The default `main` may lag the working `feature/task-soft-delete` branch.
2. Read `AGENTS.md`, this directory's README, PROJECT_STATE, IMPACT_REGISTER, decisions and your track log; read applicable historical logs and source.
3. Filter impacts targeting your track. Record reviewed SHA and **ACK / BLOCKED / NOT_APPLICABLE** in the track log and impact entry; ACK only means reviewed.
4. Inspect source/DTO/DB/CI and intervening commits from **all** tracks before implementing changes.

## Proposed impacting change
Before altering a shared contract, create `IMP-XXXX` in IMPACT_REGISTER with producer/date/old→new behavior/status, breaking or compatible classification, affected consumers, link/commit/ADR, required consumer actions, test plan, release gate. Architecture resolves disputes and maintains cross-domain priorities. No silent breaking changes.

Affected contracts include REST paths/payloads/errors, JWT and workspace membership, Flyway migrations, task/economy business rules, offline queue/idempotency, image/cover/avatar endpoints and permissions, translated enums, base URLs, deep links, environment flags, Android package version/signature, deployment/rollback.

## Implementing and integrating
Prefer `feature/backend-*`, `feature/web-*`, `feature/android-*`, `chore/release-*`, `docs/architecture-*` branches and PRs as work becomes parallelized. `feature/task-soft-delete` is the observed existing development branch, not a permanent guarantee. Re-read remote HEAD before committing shared files; never force push to paper over conflicts.

Producer records commit SHA and real test evidence; every consumer records review/required migration in own log and acknowledges the impact; impact remains OPEN if any mandatory action or integration test is missing. Backward-compatible API migration should precede removing old payloads.

## Release gate
Release independently verifies tagged SHA, CI and tests, DB migration safety, 2-workspace authorization, Web and Android compatibility, secrets/environment setup, backups/rollback, signed APK upgrades, smoke tests and actual public endpoints. A source commit is not a deployed BETA. Preserve DEV/WireGuard/Contabo's unrelated 1C/BAS routing and existing user data.

## End of work / next chat
Update track log, impact register, shared PROJECT_STATE and checklist as relevant; commit and provide branch/SHA + [handoff](HANDOFF_TEMPLATE.md). Never pretend another chat has already received the update: the next session must fetch it.
