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
