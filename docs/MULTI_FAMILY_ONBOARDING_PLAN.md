# FamilyPoints — independent family workspaces and first-run wizard

Status: architectural plan, 2026-10-12. **Not implemented by this document.**

## Product decision

The Contabo Beta must begin with **no copied development user data, balances, tasks or accounts**. The service supports many independent families ("workspaces") on one backend. Each family has independent members, permissions, currency economy, balances, tasks, rewards and devices. A shared PostgreSQL database is sufficient for Beta **only if every query and mutation enforces workspace isolation**; a DB per family is not required initially.

The template catalog is shared and read-only. Choosing a template copies editable **instances** into the current workspace; later changes in Family A never change Family B, and template updates never silently overwrite user edits.

## Current implementation audit (branch feature/task-soft-delete)

- Already present: `Workspace` (mapped to `families`), `WorkspaceMember` (`family_members`), `WorkspaceRole`, `UserAccount` bound one-to-one to a member; role-catalog default initialization; `WorkspaceRoleService.createDefaultRoles` for FAMILY_ADMIN/PARENT/CHILD; `EconomyService.getOrCreateSettings` initializes POINTS; task definitions, reward catalog and custom point types are workspace-associated.
- Public user registration/first-admin bootstrap endpoint does **not** exist. `AuthController` has login/refresh only. `UserAccountService.createAccount` requires an existing workspace member.
- **Release-blocking tenant security:** `WorkspaceController` currently has authenticated-but-unscoped GET all, GET/PUT/DELETE by ID and POST workspace creation. `EconomyController` has several endpoints without workspace-specific authorization. Audit all remaining controllers, repositories, services and nested resource IDs before allowing family accounts on the public Internet.
- Data model currently associates one `UserAccount` with exactly one `WorkspaceMember`; user belonging to *multiple* families would need a future identity/membership refactor. This is acceptable for first Beta if each user belongs to one family.
- Database migrations: only V2–V20 exist. A new empty PostgreSQL instance cannot run them without a compatible initial schema. **Do not copy current development data.**

## User-facing setup experience

1. **Landing**: Sign in / Create family (invite-only for initial Beta). Language selection UK/DE/EN/RU.
2. **Family registration**: Family name, first administrator name and login credentials. Atomically create workspace, FAMILY_ADMIN/PARENT/CHILD roles, first parent member and account, default role catalog, POINTS economy and `setup_status=DRAFT`. Sign in newly created admin.
3. **Family members**: Add parents and children by name; select age-appropriate member type and workspace role. Optionally create personal logins now or later. Ensure at least one active administrator remains. No real children's personal data in public template definitions.
4. **Rules and currencies**: Basic POINTS, optionally additional currencies; default task reward/penalty, timezone, language, approval behavior. All settings belong to selected workspace.
5. **Select templates**: Multi-select **individual** example tasks and rewards, with optional bundles (e.g. household, learning, hygiene, screen-time); display exactly what will be added, costs, repetition, assignees and prerequisites. Empty selection is valid.
6. **Assignment mapping**: Set executors/responsible members and group roles for each selected task before installation; no automatic assignment of a child just because of template title.
7. **Review and create**: Show totals per type, warn on duplicates and missing referenced currencies/tasks. Transactionally clone selected templates into this family's objects, preserve own editable copy, record template code/version and completion status. Avoid double imports on Retry.
8. **Dashboard**: Show a "Continue setup" banner until completed; allow skip/finish later. All standard screens should respect workspace setup state and permissions.

Recommended first template sets (examples only):
- Household: tidy room, make bed, put toys away, set table.
- Education: read 20 minutes, homework, study a language, math practice.
- Habits: brush teeth, morning routine, prepare school bag.
- Rewards: 20/30 minutes cartoon time, 30 minutes game time, family activity or custom material reward.
Each template needs localized name/description, sensible default points, recurrence and requirements that are editable before installation.

## API / backend design milestones

### M0: clean schema bootstrap
- Reconstruct correct **original pre-V2** baseline DDL and commit `V1__baseline_schema.sql` (do not dump V20 schema as V1 because V2–V20 would then duplicate objects).
- Alternatively, explicitly adopt **schema-only** snapshot of the *current V20* database (no rows) into a fresh Beta database, with a deliberately recorded Flyway baseline **at version 20**. This is an interim controlled bootstrap; clean future installs should eventually use versioned migrations from V1 or a deliberate new baseline strategy.
- Validate on a disposable empty PostgreSQL database and run Hibernate `ddl-auto=validate`; keep DEV untouched.
- Verify all expected system default role and point-type initialization occurs on workspace creation rather than relying on data manually inserted into DEV.

### M1: security gate before any new family logs in
- Require membership and appropriate role permission for **every** `/workspaces/{id}` route, including WorkspaceController and EconomyController. Never return `getAllWorkspaces` to ordinary family accounts.
- Separate controlled family registration from existing `POST /workspaces` (which must be admin-only/removed).
- Verify referenced `memberId`, `taskDefinitionId`, `groupId`, `rewardId`, `pointTypeId`, `purchaseId` all belong to the authenticated workspace. Reject A-workspace JWT accessing B ID for GET, POST, PUT and DELETE, including nested actors/requirements.
- Keep platform operations isolated from the family-admin role. Only a distinct platform administrator may list tenants; ideally no public endpoint for enumeration.
- Add integration tests for two workspaces with independent tokens and negative cross-workspace reads/edits/purchases.

### M2: invite-only registration (initial Beta)
- `POST /auth/register-family` should require expiring, one-use server-created invitation token or comparable gated flow, plus rate limiting and credential validation. Avoid anonymous open registrations until verification/abuse controls exist.
- In a single transaction, create workspace, default role sets, workspace roles, POINTS economy, first FAMILY_ADMIN member and their account. Password hashed via existing BCrypt encoder. Prevent partial signup and account enumeration.
- Return standard login tokens or ask user to log in; app never trusts client-supplied role IDs during initial registration.

### M3: resumable setup wizard
- Per-workspace state `DRAFT | COMPLETED`, `wizard_step`, optional version. Only permitted family admins modify setup state.
- Draft member changes must be persisted and retry-safe.
- Frontend route `/setup` with multi-step navigation, language support, mobile responsive layout and optional skips.

### M4: catalog templates
- Versioned immutable catalog definitions (JSON resources or DB tables), with stable codes and locale labels, not coupled to any family's primary keys.
- `GET /setup/templates` returns bundles/items; `POST /workspaces/{id}/setup/apply-templates` takes selected item codes, editable values and assignment mapping; server checks role, references and currency availability, then creates workspace-local task/reward definitions.
- Create an `install_batch_id`/idempotency key and unique imported-template item key per workspace/batch to avoid duplicate installations on network retries. Installed items are editable without mutating the source catalog.
- Explicitly support 'skip templates' and setup rerun/import-more later.

### M5: Beta release
- Build and test Java 21, React and signed Android. Deploy isolated PostgreSQL and backend via `deploy/beta/compose.yaml`, serve Web via Nginx/HTTPS. Do not touch WireGuard, DEV port 8443 or 1C DNAT 8080.
- Test two newly registered unrelated families on separate devices, cross-family security, setup retry/resume, Android login, offline cache, reward purchase/replay and server restart.

## Recommended implementation order

**Secure tenant isolation -> clean bootstrap -> transactional admin signup -> basic member wizard -> optional task/reward templates -> Beta activation**. The original code can keep serving local DEV while these are completed. The first Beta may launch after M3 with optional templates postponed, but multi-family isolation and clean bootstrap cannot be postponed.

## 2026-10-13 — Onboarding strategy clarified for server BETA (AGREED / NOT YET IMPLEMENTED)

The actual first-run experience is **self-service family registration**, not manual SQL creation of a fixed `betaadmin`. The public-facing product must let a new visitor create an entirely new isolated Family Workspace and its first administrator through a registration page. Controlled invitation flags/rate limits may temporarily restrict initial BETA *availability*, but must not become a permanent identity architecture. A previously proposed manual `bootstrap-first-admin.sql` was stopped before creation; do not implement a global shared default-family administrator.

An identity is **global to the platform** while memberships, member profiles and permissions are **scoped to each Workspace**. One person must be able to join several unrelated families (and later offices, teams or businesses) through separate memberships. Login identifies the global account; the user chooses an active Workspace, and the backend verifies both its membership and action permission on every request. Current DB has `account_workspace_memberships` and some switching infrastructure, but older one-to-one `UserAccount.workspaceMember` mapping remains a transitional compatibility constraint. Do not label multi-workspace UX/authorization complete before integration tests.

### First-run onboarding workflow
1. Landing: Login / Register / Create family; language choice.
2. Register global user account or sign in as existing user and create an additional family; verify login uniqueness and enforce password policy, abuse controls and anti-enumeration.
3. In ONE transaction create the isolated family Workspace, administrator membership/profile, default rights and initial economy settings. No client-supplied arbitrary admin role IDs.
4. Family composition: optionally add adults/children, selecting role, approximate age or birth-year/age range and optional gender/other relevant preferences with privacy-minimal fields. These inputs affect **suggestions only**, never intrinsic permissions, task eligibility or fixed stereotypes. Editing/skipping is allowed; no age/gender field should be mandatory where unnecessary.
5. Recommend versioned family-template bundles, individual tasks, goals, habits, point currencies, price ranges and time-based rewards according to composition and preferences. Display selection with recurrence, responsible participants, starting prices, optional quality/approval and prerequisites, and explain which inputs influenced suggestions.
6. Review/edit proposed economics (starting point type, task reward/penalty defaults, prices, duration rules), assignments, roles, and template choices. Never auto-charge, credit, or assign work from an unreviewed suggested template.
7. Atomically import only explicitly accepted templates as independent Workspace-owned objects, with stable catalog codes/versions and idempotency keys; skip or resume, re-run later without duplication. Template evolution must not overwrite edits in existing families.
8. Open Dashboard, allowing the admin to continue the setup wizard later; all Android/Web functionality observes the active Workspace boundary.

### Tenant isolation and release gates
- Shared PostgreSQL + strict `workspace_id` authorization is acceptable to start; families must be logically independent on every row/read/write/reference, including indirectly linked member/group/task/point/reward/device IDs. Re-check API endpoints and negative two-family/one-multimembership integration tests before general public exposure.
- Global platform template catalog contains no actual family/child data. No cross-family shared balances, role privileges, history or actor inheritance; a shared account grants no access to other families beyond explicit membership.
- Family administrative authority is distinct from platform administration. Login/user creation, invite linking, session workspace switching and authorization must not assume account equals workspace.
- Later workgroups/office templates reuse the underlying Workspace engine, but the immediate BETA concentrates on families.
- Age and gender should influence **optional, editable suggestions**, not create mandatory or discriminatory role defaults. Privacy-minimal handling for children is a release requirement.

### Delivery priority
Tenant security audit and compatible global-account multi-membership foundation -> transactional registration / family bootstrap -> responsive Web and native Android signup/setup -> default economy -> personalized optional template recommendations -> family-to-family isolation acceptance testing. Do not bypass this with manually inserted database accounts.


## 2026-10-13 — Delivery order: DEV first, BETA second (AGREED)
- Build and refine the public-family signup, multi-workspace membership, and optional family-composition-based onboarding templates in the existing laptop **DEV** environment first.
- Complete backend authorization/tenant-isolation changes and Web/Android flows on DEV; test two independent families, a single account linked to more than one family, setup retry/idempotency and core task/economy scenarios.
- Run backend integration tests, Web build and local Android build; fix regressions before any server rollout.
- After DEV acceptance, mark a specific Git commit as the candidate, back up the BETA PostgreSQL volume/database, review pending Flyway migrations and deploy **that exact commit** to Contabo. Avoid blindly pulling and restarting the public deployment on every development commit.
- Post-deploy smoke-check HTTPS API/Web, authenticate real BETA-created users, verify cross-family isolation, Android API connectivity, and that WireGuard/1C forwarding remains intact. Revert application image to the previous pinned version if necessary; do not roll back database migrations blindly.
- Current Contabo BETA (already running) remains unchanged while DEV onboarding development proceeds. No manual SQL betaadmin shortcut.
