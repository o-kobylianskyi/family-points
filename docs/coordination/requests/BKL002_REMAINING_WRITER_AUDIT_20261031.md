# BKL-002 — remaining Backend writer audit, 2026-10-31

Baseline: main `65e6726db369190e19ed06e50b5c69dca60466c3`. Source audit of 90 controller/service Java files, not runtime acceptance. This extends existing IMP-0009; no new product scope, no reset/deployment authorization.

## Current package

- Family-first guards for three TaskScheduleSlotController mutations, four PointExchangeService mutations, ten PointService ledger entry points and ReputationService.add.
- Internal ledger entries can use only an epoch previously validated for the same family in the same transaction. HTTP calls always validate the original header. Validated state is suspended/resumed with REQUIRES_NEW and removed after commit/rollback; a fresh database epoch is never substituted for captured proof.
- Detached PointType inputs to penalty posting are re-resolved within the scoped family before posting.
- No schema, request body or permissions change. Cloud CI required before acceptance.

## Remaining source groups

| Group | Source entry points to audit next | Required boundary |
|---|---|---|
| Groups and hierarchy | MemberGroupService, LegacyGroupRoleBridgeService, RoleDefinitionService, RoleSetService, WorkspaceRoleService; group ledgers/role assignments | Entire outer transaction, before member/group/role locks or nested service calls |
| Economy/settings | EconomyService, EconomyController, StandardCurrencyForms | Currency GET paths lazily create settings/types/forms; distinguish server-derived current-state refresh from explicit client edits; controller name-form save needs a real outer transaction |
| Task suggestions | TaskSuggestionController | Direct JdbcTemplate writes lack an explicit controller transaction; introduce a transaction before fencing, not only an HTTP check |
| Private templates | WorkspaceTemplateController | Fence before claiming import/activation/idempotency rows and before nested TaskService/RewardService calls; inspect snapshot/payload replay semantics |
| Media | MediaService | Fence before quota/entity/media locks and SQL writes; preserve authorized previews/catalog and explicit binding precedence |
| People and workspace config | WorkspaceMemberService, MemberExceptionPeriodService, WorkspaceService and callers | Check exact transaction boundaries, owner survival, scoped membership soft deletion, immutable identities and reset exclusions |
| Membership/auth edges | WorkspaceInvitationService, WorkspaceJoinService, MemberEmailInvitationService, AccountMembershipLinkService, UserAccountService, EmailAccountController | Keep global auth/other workspaces protected; define which membership/invitation mutations interact with selected reset categories before placing a workspace fence |
| Startup/provisioning | DomainFoundationMigrationService (ApplicationReadyEvent), FamilyWorkspaceProvisioningService, RoleCatalogBootstrapService and registration callers | Fresh workspace creation versus existing-family backfill must have distinct rules; reload existing data after family fencing, avoid treating absent HTTP context as client permission |
| Reset metadata | WorkspaceDataResetPrepareController, WorkspaceResetChallengeController, ResetChallengeAttemptLimiter | Family-before-operation lock ordering; persistent reauth attempt limiting is REQUIRES_NEW and must be reviewed before adding an outer family lock |

## Important remaining checks

1. Constructors/annotations alone are not a complete call graph. Trace upstream callers that already wrote or locked domain rows before invoking protected services.
2. TaskAuditService.save is a subordinate repository write; verify all its callers have a fenced transaction. Do not silently suppress audit when fencing fails.
3. Scope queries in remaining group ledgers, role links, images and activation records must use family scoping even where FKs reference globally unique IDs.
4. Check missing transaction boundaries as well as missing guards; repository-level transactions are insufficient for a multi-write business operation.
5. Confirm the V44 foreign-key graph before placing a family lock around reauth throttling. Independent REQUIRES_NEW writes must not wait on a parent-held family row; preserve failed-attempt persistence without self-blocking.
6. Complete a per-table FK/ledger closure and reset authorization/idempotency/epoch/onboarding transaction only after writer integration. Current execute stays disabled.
7. Verify protected owner/account/workspace survival and behavior of startup jobs, system refreshes, in-flight transactions and stale offline replay on disposable PostgreSQL, then disposable DEV.

## Designer follow-up (P1, issue #17)

Catalog recommendations are read-only at /template-catalog/economy-suggestions. Publish exact versioned nullable DTO examples and explicit acceptance semantics. Media catalog kind labels REWARD/TASK_CATEGORY do not imply writable binding targets: current binding types are WORKSPACE/MEMBER/MEMBER_GROUP/WORK_NODE/TASK. Certify that distinction before consumer persistence work; no silent price/ledger changes.

## Current package acceptance evidence

Exact source `fc4fa54b06d000f0449e3875ffea11c7ca10a8bb` passed all four jobs in [GitHub Actions 41231246497](https://github.com/o-kobylianskyi/family-points/actions/runs/41231246497). Backend: 280 tests, zero failures/errors/skips; 17 real Spring/JPA/PostgreSQL scenarios. The current 18-entry slot/exchange/ledger package is source/CI verified. Remaining source groups above are still OPEN; nothing was reset or deployed.

## Broad package update — supersedes earlier remaining status for business/config paths

Exact source `cda7e8a07a3d20ca6650bea1ed5df31330865973`, all-green [Actions 41232509336](https://github.com/o-kobylianskyi/family-points/actions/runs/41232509336), Backend 295 PASS. Groups/group ledger/roles/legacy bridge, economy lazy reads/forms, template CRUD/import/activation, media, task suggestions, member/config/timezone edits and per-family startup/default processing now have family fences and source/CI evidence. Startup/bridge inputs reload under locking. Fresh provisioning remains compatible. Selected-family role checks and global-account survival on scoped member removal are repaired and tested.

Still OPEN: independent authentication/invitation/join/account-link lifecycle interactions; reset prepare/challenge/REQUIRES_NEW limiter lock order; exact retained owner/identity/ledger policy and per-table FK closure; strong snapshot freshness; one-use reauthentication and idempotent atomic execute/epoch/onboarding; authenticated disposable DEV/client QA and Release. No current package enables reset or clears data.

Designer #17 source sign-off is now [the implemented economy/media contract](../../api/DESIGNER_ECONOMY_MEDIA_V1_CONTRACT.md). Consumer/runtime acceptance remains pending; a REWARD catalog category is not a writable media target.

## 2026-10-31 — Invitation/reset edge integration (CI pending)
WorkspaceInvitationService, WorkspaceJoinService, MemberEmailInvitationService, AccountMembershipLinkService and workspace-scoped UserAccountService writes now fence family state. Public token/code acceptance revalidates after current-state fence; manager writes check original client epoch. Global self-service auth stays outside family reset.
Prepare/confirm/challenge now lock family before operation records; REQUIRES_NEW reauth budget runs before family lock, nested same-family fenced parent rejected. Content-v2 replaces counts, includes scoped FK descendants; old preparations need NEW operation IDs.
Remaining: upstream/global account auth policy/races (including family-admin edits to global credentials), complete owner/retention/financial/FK deletion policy, fresh Google reauth and one-use proof consumption, idempotent atomic execute/epoch/onboarding, independent client/disposable DEV/QA/Release acceptance. Snapshot traversal is not a deletion plan. Execute still disabled; no data cleared or deployment.

## Invitation/reset edge evidence — 2026-10-31
Exact tested SHA `4d88716a9cde33a15a7f94867ee02d2a69b613c3`; [Actions 41234461782](https://github.com/o-kobylianskyi/family-points/actions/runs/41234461782) all four jobs SUCCESS. Backend: 313 tests, zero failures/errors/skips; 39 actual Spring/JPA/PostgreSQL transaction scenarios.
The preceding CI-pending invitation/reset edge delta is now source/CI verified. Public token/code authority is revalidated after family lock; exact reset content freshness and limiter ordering verified. This does not close the remaining global-auth/admin policy, deletion/owner/financial/FK/Google-proof/atomic execute and client/disposable DEV/QA/Release gates above. No real reset or deploy.

## 2026-10-31 — Internal proof progress (CI pending)
Bound password proof now has MANDATORY issue/consume service and additive V45 state. Controller still withholds secret, execute423 unchanged. Current credential/active membership and wall-clock expiry are rechecked; rollbackable consumed flag supports atomic future executor. This does not close owner/role/financial/deletion/FK/Google/transport/client/DEV/QA/Release policy gates. Scope-specific QA coverage matrix and actual disposable schema inventory follow CI; no live data or migrations touched.

## Bound proof / full source inventory acceptance — 2026-10-31
Exact tested SHA `b4f62fc9bd13b9c9aec223619f016bac2f7ebcc7`; [GitHub Actions 41249950772](https://github.com/o-kobylianskyi/family-points/actions/runs/41249950772) all four jobs SUCCESS. Backend: 329 tests, zero failures/errors/skips; PointMutationTransactionPostgresTest: 54 PASS. WorkspaceResetProofUpgradePostgresTest: 1 PASS, V44->V45 preservation/constraints/repeat-migrate no-op.
See BKL002_WRITE_PATH_COVERAGE_20261031.md for all136 mutation mappings, source fence/delegate notes, concrete real-scenario evidence and explicit remaining runtime/control-flow limits; no universal protection claim.
See BKL002_RETENTION_FK_INVENTORY_20261031.md for exact70-table/156-FK metadata and non-FK semantic dependencies; it is NOT an executable cleanup plan. XTR-20261031-B-01 records remaining owner/retention/global-account/financial/proof transport decisions.
Internal one-use proof lifecycle is now source/CI verified; public proof/Google reauth, owner/financial/deletion policy, atomic execute/epoch/audit/onboarding and independent client/DEV/QA/Release gates remain open. No live reset/migration/deploy.
