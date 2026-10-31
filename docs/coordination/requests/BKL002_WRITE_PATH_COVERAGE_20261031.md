# BKL-002 — Backend write-path coverage and QA handoff, 2026-10-31

**Source inventory pinned to a4c0369a3903829c8919898784d08bbb06b30204.** The inventory covers all 136 POST/PUT/PATCH/DELETE mappings across the controller layer; review read 91 existing controller/service files plus the new WorkspaceResetProofService. It records declared routes and visible source calls, not a proof of every dynamically reachable execution path or runtime deployment. No RequestMapping(method=...) mutations were present. HTTP POST alone does not imply persistence.

## Evidence levels

- Prior exact-tested 4d88716a: [all-green CI41234461782](https://github.com/o-kobylianskyi/family-points/actions/runs/41234461782), 313 tests, 39 real Spring/JPA/PostgreSQL transaction scenarios.
- Current bound-proof source adds fifteen transaction scenarios + one populated upgrade test. Exact tested SHA `b4f62fc9bd13b9c9aec223619f016bac2f7ebcc7`; [GitHub Actions 41249950772](https://github.com/o-kobylianskyi/family-points/actions/runs/41249950772) all four jobs SUCCESS. Backend: 329 tests, zero failures/errors/skips; PointMutationTransactionPostgresTest: 54 PASS. WorkspaceResetProofUpgradePostgresTest: 1 PASS, V44->V45 preservation/constraints/repeat-migrate no-op.
- Source transaction/guard presence is weaker than a real concurrent service/HTTP test. No percentage or universal protection claim follows from this table.
- HTTP epoch filter is a preliminary rejection only. Business write must fence in its real outer transaction before domain reads/locks/claims. HTTP original epoch is never replaced by current database state; explicit server-derived jobs and retained token acceptance are documented exceptions with state reload/revalidation.

## Real PostgreSQL acceptance mapping

All method names below are in PointMutationTransactionPostgresTest unless stated otherwise.

| Area | Actual fixture evidence | Remaining limit |
|---|---|---|
| Point HTTP service wrapper | currentEpochPostsOnceAndStaleReplayCannotReturnOldSuccess; missingEpochWorksOnlyAtZeroAndLedgerRollsBackWithOuterTransaction | Service proxies + original epoch argument; every HTTP transport/device is not exercised |
| Real family reset-shaped lock vs ledger | resetBlocksStaleLedgerWriterButAnotherFamilyCanPost | Holder increments epoch; it does not execute a real reset deletion plan |
| Task/reward writes | taskServiceRejectsStaleRequestAndCurrentRequestPersistsInItsOwnTransaction; rewardServiceRejectsStaleEpochAndRollsBackFlushedCategory; resetFencesRealTaskServiceWhileAnotherFamilyCreatesRewardCategory | Representative real domain operations, not every business branch |
| System generation/deadlines | systemGenerationReloadsAndDoesNotResurrectTaskDeletedByReset; automaticDeadlineScanUsesCurrentGenerationAfterNonzeroEpoch; backgroundDeadlinePenaltiesJoinValidatedGenerationAtNonzeroEpochExactlyOnce | Fixture deletion/concurrency only; live jobs/deployment unverified |
| Schedule/audit and exchanges | scheduleSlotMutationsRejectStaleEpochAndRollbackAuditWithData; exchangeEpochFencesTheRequestAndBothCurrencyPostingsAtomically | Complete destructive economic policy is not implemented |
| Proof of validated transaction epoch | internalLedgerWritesRequireValidatedParentAndProofExpiresAtTransactionEnd; validatedEpochIsSuspendedForRequiresNewAndRestoredOnlyForItsParent; capturedEpochCannotBeReplacedWithFreshDatabaseEpochForInternalPosting; validatedParentCannotAuthorizeOtherFamilyOrOverrideHttpHeader | Captured epoch is not fresh account reauth or reset permission |
| Groups/roles/group ledger | groupsRolesAndGroupLedgerShareEpochFenceAndRollback; RoleCatalogSelectedWorkspaceTest | Role-permutation/owner delegation acceptance still needed |
| Economy lazy reads/forms | lazyEconomyReadsWorkAfterResetAndPreserveExplicitCurrencyForms | GET backfill is a server-current-state write; custom defaults and consumer runtime need QA |
| Templates/activation | templateCrudAndActivationClaimAreFencedBeforeDownstreamCreation | Claim/domain rollback injected before commit; not a database commit I/O failure |
| Media/suggestions | mediaUploadBindingAndClearRollbackUseOneFamilyGeneration; suggestionWritesHaveOuterTransactionAndCannotInventTasksOrPoints | All SVG renderers/privacy retention not covered |
| People/config/startup/new family | workspaceMemberAndTimezoneEditsShareFenceAndRollback; startupBackfillReloadsPerFamilyAfterLockAndRemainsIdempotent; freshFamilyProvisioningDoesNotTreatOldWorkspaceHeaderAsNewFamilyEpoch | New-family creation is separate from existing-family reset |
| Global account + other family | revokingOneFamilyMemberPreservesGlobalAccountAndOtherFamilyMembership; explicitMemberLinkAtNonzeroEpochRejectsStaleThenLinksOnlyTargetMember | Family-admin global-credential controls still need separate authorization policy |
| Public invitation/join authority | publicInvitationRevalidatesRevocationAfterWaitingForFamilyFence; publicJoinCodeRechecksRotationAfterFamilyFenceWithoutGrantingMembership; publicInvitationAtNonzeroEpochUsesRetainedTokenAndKeepsOtherFamilyMembership | Public email-token branch shares requireInvite fence; no real email sent; real Google fresh-reset auth unsupported |
| Reset snapshot/reauth limiter | contentSnapshotIsDeterministicAndDetectsSameCountEdit; contentSnapshotDetectsIndirectRolePermissionAndPrivateTemplateEdits; sameCountEditAfterPrepareRejectsConfirmationAndPreservesAuditState; editAfterConfirmationRejectsChallengeWithoutIssuingProof; failedPasswordBudgetCommitsDespiteChallengeRollbackAndLimitsRepeatedTries; nestedAlreadyFencedChallengeFailsBeforeIndependentLimiterTransaction | Snapshot is conservative full-family content, NOT executable FK closure |
| New bound password proof | 15 additional real scenarios cover binding, current credential, wall-clock TTL, one-use race, rollback/retry and HTTP secrecy; WorkspaceResetProofUpgradePostgresTest covers V44->45 populated upgrade | Internal secret only; owner policy/transport/Google/executor remain disabled and unapproved |

Existing service boundary suites RemainingWorkspaceEpochBoundaryTest (56 entries), TaskRewardEpochBoundaryTest, LedgerEpochBoundaryTest and MembershipEpochBoundaryTest check fail-before-domain ordering with mocks/reflection. They supplement real fixtures and do NOT prove each route over actual HTTP.

## Mutation route source inventory

Controller Tx means declared method/class transaction; service/caller means the effective boundary must be checked in the listed service, helper or explicit exception. Direct family guards shown here are visible source calls. Delegated guards are a one-hop source lookup, not a full call-graph assertion.

| Controller entry | HTTP declared route | Controller Tx | Visible family fence / reviewed exception |
|---|---|---|---|
| `AdditionalFamilyController.create` | `POST /api/account/families` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `AuthController.refresh` | `POST /auth/refresh` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `AuthController.login` | `POST /auth/login` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `CalendarController.changeTimeZone` | `PUT /workspaces/{workspaceId}/calendar/time-zone` | service/caller | CalendarService.updateZone → requireCurrentRequestEpoch |
| `CurrentUserController.changePassword` | `PUT /api/me/password` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `EconomyController.updatePointType` | `PUT /workspaces/{workspaceId}/economy/point-types/{pointTypeId}` | service/caller | EconomyService.updatePointType → requireCurrentRequestEpoch |
| `EconomyController.createPointType` | `POST /workspaces/{workspaceId}/economy/point-types` | service/caller | EconomyService.createPointType → requireCurrentRequestEpoch |
| `EconomyController.enableMultiMode` | `POST /workspaces/{workspaceId}/economy/mode/multi` | service/caller | EconomyService.enableMultiTypeMode → requireCurrentRequestEpoch |
| `EconomyController.enableSimpleMode` | `POST /workspaces/{workspaceId}/economy/mode/simple` | service/caller | EconomyService.enableSimpleMode → requireCurrentRequestEpoch |
| `EconomyController.saveNameForms` | `PUT /workspaces/{workspaceId}/economy/point-types/{pointTypeId}/name-forms/{language}` | yes | local.requireCurrentRequestEpoch |
| `EmailAccountController.verify` | `POST /auth/email/verify` | yes | Global auth or new-family boundary; see exclusion review. |
| `EmailAccountController.requestVerification` | `POST /auth/email/request-verification` | yes | Global auth or new-family boundary; see exclusion review. |
| `EmailAccountController.requestReset` | `POST /auth/email/request-reset` | yes | Global auth or new-family boundary; see exclusion review. |
| `EmailAccountController.resetPassword` | `POST /auth/email/reset-password` | yes | Global auth or new-family boundary; see exclusion review. |
| `FamilyRegistrationController.register` | `POST /auth/register-family` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `FirstFamilyOnboardingController.create` | `POST /api/onboarding/first-family` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `GlobalAccountRegistrationController.create` | `POST /auth/register-account` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `GoogleAccountLinkController.setPassword` | `POST /api/account/google/set-password` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `GoogleAccountLinkController.link` | `POST /api/account/google/link` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `GoogleLoginController.login` | `POST /auth/google` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `GoogleRegistrationController.register` | `POST /auth/google/register` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `MediaController.references` | `POST /workspaces/{workspaceId}/media/references` | service/caller | Read-only effective reference resolution. |
| `MediaController.upload` | `POST /workspaces/{workspaceId}/media/uploads` | service/caller | MediaService.upload → requireCurrentRequestEpoch |
| `MediaController.bind` | `PUT /workspaces/{workspaceId}/media/bindings/{type}/{targetId}/{slot}` | service/caller | MediaService.bind → requireCurrentRequestEpoch |
| `MediaController.clear` | `DELETE /workspaces/{workspaceId}/media/bindings/{type}/{targetId}/{slot}` | service/caller | MediaService.clear → requireCurrentRequestEpoch |
| `MemberEmailInvitationController.issue` | `POST /workspaces/{workspaceId}/members/{memberId}/email-invitation` | service/caller | MemberEmailInvitationService.issue → requireCurrentRequestEpoch |
| `MemberEmailInvitationController.check` | `POST /auth/member-invitations/check` | service/caller | Global auth or new-family boundary; see exclusion review. |
| `MemberEmailInvitationController.acceptGoogle` | `POST /auth/member-invitations/accept-google` | service/caller | Same retained-token fence after verified Google identity; not Google reset reauthentication. |
| `MemberEmailInvitationController.accept` | `POST /auth/member-invitations/accept` | service/caller | Current-state fence inside private requireInvite after target discovery; then active invite/member recheck. |
| `MemberExceptionPeriodController.create` | `POST /workspaces/{workspaceId}/members/{memberId}/exception-periods` | service/caller | MemberExceptionPeriodService.create → requireCurrentRequestEpoch |
| `MemberGroupController.create` | `POST /workspaces/{workspaceId}/member-groups` | service/caller | MemberGroupService.createGroup → requireCurrentRequestEpoch |
| `MemberGroupController.update` | `PUT /workspaces/{workspaceId}/member-groups/{groupId}` | service/caller | MemberGroupService.updateGroup → requireCurrentRequestEpoch |
| `MemberGroupController.delete` | `DELETE /workspaces/{workspaceId}/member-groups/{groupId}` | service/caller | MemberGroupService.deactivateGroup → requireCurrentRequestEpoch |
| `MemberGroupController.addRole` | `POST /workspaces/{workspaceId}/member-groups/{groupId}/roles` | service/caller | MemberGroupService.addRole → requireCurrentRequestEpoch |
| `MemberGroupController.updateRole` | `PUT /workspaces/{workspaceId}/member-groups/{groupId}/roles/{roleId}` | service/caller | MemberGroupService.updateRole → requireCurrentRequestEpoch |
| `MemberGroupController.deleteRole` | `DELETE /workspaces/{workspaceId}/member-groups/{groupId}/roles/{roleId}` | service/caller | MemberGroupService.deleteRole → requireCurrentRequestEpoch |
| `MemberGroupController.updateMemberRoles` | `PUT /workspaces/{workspaceId}/member-groups/{groupId}/members/{memberId}/roles` | service/caller | MemberGroupService.updateMemberRoles → requireCurrentRequestEpoch |
| `MemberGroupController.addMember` | `PUT /workspaces/{workspaceId}/member-groups/{groupId}/members` | service/caller | MemberGroupService.addMember → requireCurrentRequestEpoch |
| `MemberGroupController.removeMember` | `DELETE /workspaces/{workspaceId}/member-groups/{groupId}/members/{memberId}` | service/caller | MemberGroupService.removeMember → requireCurrentRequestEpoch |
| `MemberGroupController.addChild` | `PUT /workspaces/{workspaceId}/member-groups/{groupId}/children` | service/caller | MemberGroupService.addChildGroup → requireCurrentRequestEpoch |
| `MemberGroupController.removeChild` | `DELETE /workspaces/{workspaceId}/member-groups/{groupId}/children/{childId}` | service/caller | MemberGroupService.removeChildGroup → requireCurrentRequestEpoch |
| `MemberGroupController.addPermission` | `POST /workspaces/{workspaceId}/member-groups/{groupId}/roles/{roleId}/permissions` | service/caller | MemberGroupService.addPermissionGrant → requireCurrentRequestEpoch |
| `MemberGroupController.removePermission` | `DELETE /workspaces/{workspaceId}/member-groups/{groupId}/permissions/{grantId}` | service/caller | MemberGroupService.removePermissionGrant → requireCurrentRequestEpoch |
| `MemberGroupController.points` | `POST /workspaces/{workspaceId}/member-groups/{groupId}/points` | service/caller | MemberGroupService.addPoints → requireCurrentRequestEpoch |
| `PointController.applyOfflineOperation` | `POST /workspaces/{workspaceId}/members/{memberId}/points/offline/{operation}` | service/caller | PointMutationService.applyOfflineOperation → requireCurrentEpoch |
| `PointController.applyManualTyped` | `POST /workspaces/{workspaceId}/members/{memberId}/points/manual/{operation}` | service/caller | PointMutationService.applyTypedManualOperation → requireCurrentEpoch |
| `PointController.earn` | `POST /workspaces/{workspaceId}/members/{memberId}/points/earn` | service/caller | PointMutationService.applyManualOperation → requireCurrentEpoch |
| `PointController.spend` | `POST /workspaces/{workspaceId}/members/{memberId}/points/spend` | service/caller | PointMutationService.applyManualOperation → requireCurrentEpoch |
| `PointController.penalty` | `POST /workspaces/{workspaceId}/members/{memberId}/points/penalty` | service/caller | PointMutationService.applyManualOperation → requireCurrentEpoch |
| `PointExchangeController.createRate` | `POST /workspaces/{workspaceId}/point-exchanges/rates` | service/caller | PointExchangeService.createRate → requireCurrentRequestEpoch |
| `PointExchangeController.createExchange` | `POST /workspaces/{workspaceId}/point-exchanges/members/{memberId}/requests` | service/caller | PointExchangeService.createExchange → requireCurrentRequestEpoch |
| `PointExchangeController.approve` | `POST /workspaces/{workspaceId}/point-exchanges/requests/{requestId}/approve` | service/caller | PointExchangeService.approve → requireCurrentRequestEpoch |
| `PointExchangeController.reject` | `POST /workspaces/{workspaceId}/point-exchanges/requests/{requestId}/reject` | service/caller | PointExchangeService.reject → requireCurrentRequestEpoch |
| `RewardController.createCategory` | `POST /workspaces/{workspaceId}/rewards/categories` | service/caller | RewardService.createCategory → requireCurrentRequestEpoch |
| `RewardController.createReward` | `POST /workspaces/{workspaceId}/rewards` | service/caller | RewardService.createReward → requireCurrentRequestEpoch |
| `RewardController.updateReward` | `PUT /workspaces/{workspaceId}/rewards/{rewardId}` | service/caller | RewardService.updateReward → requireCurrentRequestEpoch |
| `RewardController.updateAvailability` | `PUT /workspaces/{workspaceId}/rewards/{rewardId}/availability` | service/caller | RewardAvailabilityService.update → requireCurrentRequestEpoch |
| `RewardController.createRequest` | `POST /workspaces/{workspaceId}/rewards/requests` | service/caller | RewardService.createRequest → requireCurrentRequestEpoch |
| `RewardController.approveRequest` | `POST /workspaces/{workspaceId}/rewards/requests/{requestId}/approve` | service/caller | RewardService.approveRequest → requireCurrentRequestEpoch |
| `RewardController.rejectRequest` | `POST /workspaces/{workspaceId}/rewards/requests/{requestId}/reject` | service/caller | RewardService.rejectRequest → requireCurrentRequestEpoch |
| `RewardController.refreshRequest` | `POST /workspaces/{workspaceId}/rewards/requests/{requestId}/refresh` | service/caller | RewardService.refreshRequest → requireCurrentRequestEpoch |
| `RewardController.purchaseRewardForMember` | `POST /workspaces/{workspaceId}/rewards/{rewardId}/purchase-for-member/{memberId}` | service/caller | RewardService.purchaseReward → requireCurrentRequestEpoch |
| `RewardController.purchaseReward` | `POST /workspaces/{workspaceId}/rewards/{rewardId}/purchase` | service/caller | RewardService.purchaseReward → requireCurrentRequestEpoch |
| `RewardController.purchaseRequest` | `POST /workspaces/{workspaceId}/rewards/requests/{requestId}/purchase` | service/caller | RewardService.purchaseRequest → requireCurrentRequestEpoch |
| `RoleDefinitionController.create` | `POST /workspaces/{workspaceId}/role-definitions` | service/caller | RoleDefinitionService.create → requireCurrentRequestEpoch |
| `RoleDefinitionController.update` | `PUT /workspaces/{workspaceId}/role-definitions/{roleId}` | service/caller | RoleDefinitionService.update → requireCurrentRequestEpoch |
| `RoleDefinitionController.delete` | `DELETE /workspaces/{workspaceId}/role-definitions/{roleId}` | service/caller | RoleDefinitionService.deactivate → requireCurrentRequestEpoch |
| `RoleSetController.create` | `POST /workspaces/{workspaceId}/role-sets` | service/caller | RoleSetService.create → requireCurrentRequestEpoch |
| `RoleSetController.update` | `PUT /workspaces/{workspaceId}/role-sets/{roleSetId}` | service/caller | RoleSetService.update → requireCurrentRequestEpoch |
| `RoleSetController.delete` | `DELETE /workspaces/{workspaceId}/role-sets/{roleSetId}` | service/caller | RoleSetService.deactivate → requireCurrentRequestEpoch |
| `TaskController.createDefinition` | `POST /workspaces/{workspaceId}/tasks/definitions` | service/caller | TaskService.createDefinition → requireCurrentRequestEpoch |
| `TaskController.updateDefinition` | `PUT /workspaces/{workspaceId}/tasks/definitions/{definitionId}` | service/caller | TaskService.updateDefinition → requireCurrentRequestEpoch |
| `TaskController.setDefinitionActive` | `PATCH /workspaces/{workspaceId}/tasks/definitions/{definitionId}/active` | service/caller | TaskService.setDefinitionActive → requireCurrentRequestEpoch |
| `TaskController.createInstance` | `POST /workspaces/{workspaceId}/tasks/definitions/{definitionId}/instances` | service/caller | TaskService.createInstance → requireCurrentRequestEpoch |
| `TaskController.completeScheduledByManager` | `POST /workspaces/{workspaceId}/tasks/definitions/{definitionId}/complete-scheduled` | service/caller | TaskService.completeScheduledByManager → requireCurrentRequestEpoch; TaskService.completeScheduledByManager → requireCurrentRequestEpoch |
| `TaskController.startScheduledOccurrence` | `POST /workspaces/{workspaceId}/tasks/definitions/{definitionId}/start-scheduled` | service/caller | TaskService.startScheduledOccurrence → requireCurrentRequestEpoch; TaskService.startScheduledOccurrence → requireCurrentRequestEpoch |
| `TaskController.claim` | `POST /workspaces/{workspaceId}/tasks/definitions/{definitionId}/claim` | service/caller | TaskService.claim → requireCurrentRequestEpoch |
| `TaskController.delegate` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/delegate` | service/caller | TaskService.delegate → requireCurrentRequestEpoch |
| `TaskController.start` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/start` | service/caller | TaskService.start → requireCurrentRequestEpoch |
| `TaskController.pause` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/pause` | service/caller | TaskService.pause → requireCurrentRequestEpoch |
| `TaskController.resume` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/resume` | service/caller | TaskService.resume → requireCurrentRequestEpoch |
| `TaskController.cancel` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/cancel` | service/caller | TaskService.cancel → requireCurrentRequestEpoch |
| `TaskController.release` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/release` | service/caller | TaskService.release → requireCurrentRequestEpoch |
| `TaskController.approveChildCompletion` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/approve-completion` | service/caller | TaskService.approveChildCompletion → requireCurrentRequestEpoch |
| `TaskController.complete` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/complete` | service/caller | TaskService.complete → requireCurrentRequestEpoch |
| `TaskController.miss` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/miss` | service/caller | TaskService.miss → requireCurrentRequestEpoch |
| `TaskController.excuse` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/excuse` | service/caller | TaskService.excuse → requireCurrentRequestEpoch |
| `TaskController.createRewardRequest` | `POST /workspaces/{workspaceId}/tasks/instances/{instanceId}/reward-requests` | service/caller | TaskRewardNegotiationService.create → requireCurrentRequestEpoch |
| `TaskController.approveRewardRequest` | `POST /workspaces/{workspaceId}/tasks/reward-requests/{requestId}/approve` | service/caller | TaskRewardNegotiationService.approve → requireCurrentRequestEpoch |
| `TaskController.rejectRewardRequest` | `POST /workspaces/{workspaceId}/tasks/reward-requests/{requestId}/reject` | service/caller | TaskRewardNegotiationService.reject → requireCurrentRequestEpoch |
| `TaskController.cancelRewardRequest` | `POST /workspaces/{workspaceId}/tasks/reward-requests/{requestId}/cancel` | service/caller | TaskRewardNegotiationService.cancel → requireCurrentRequestEpoch |
| `TaskController.generate` | `POST /workspaces/{workspaceId}/tasks/generate` | service/caller | TaskGenerationService.generateForWorkspace → requireCurrentRequestEpoch |
| `TaskController.processDeadlines` | `POST /workspaces/{workspaceId}/tasks/process-deadlines` | service/caller | TaskDeadlineService.processDeadlines → requireCurrentRequestOrSystemEpoch |
| `TaskScheduleSlotController.create` | `POST /workspaces/{workspaceId}/tasks/definitions/{definitionId}/schedule-slots` | yes | local.requireCurrentRequestEpoch |
| `TaskScheduleSlotController.update` | `PUT /workspaces/{workspaceId}/tasks/definitions/{definitionId}/schedule-slots/{slotId}` | yes | local.requireCurrentRequestEpoch |
| `TaskScheduleSlotController.setEnabled` | `PATCH /workspaces/{workspaceId}/tasks/definitions/{definitionId}/schedule-slots/{slotId}/enabled` | yes | local.requireCurrentRequestEpoch |
| `TaskSuggestionController.suggest` | `POST /workspaces/{workspaceId}/task-suggestions` | yes | local.requireCurrentRequestEpoch |
| `TaskSuggestionController.decide` | `POST /workspaces/{workspaceId}/task-suggestions/{suggestionId}/decision` | yes | local.requireCurrentRequestEpoch |
| `UserAccountController.createAccount` | `POST /workspaces/{workspaceId}/members/{memberId}/account` | service/caller | UserAccountService.createAccount → requireCurrentRequestEpoch |
| `UserAccountController.updateAccount` | `PUT /workspaces/{workspaceId}/members/{memberId}/account` | service/caller | UserAccountService.updateAccount → requireCurrentRequestEpoch |
| `UserAccountController.resetPassword` | `PUT /workspaces/{workspaceId}/members/{memberId}/account/password` | service/caller | UserAccountService.resetPassword → requireCurrentRequestEpoch |
| `WorkspaceController.createWorkspace` | `POST /workspaces` | service/caller | Disabled403; fresh-family routes use provisioning. |
| `WorkspaceController.updateWorkspace` | `PUT /workspaces/{id}` | service/caller | WorkspaceService.updateWorkspace → requireCurrentRequestEpoch |
| `WorkspaceController.deleteWorkspace` | `DELETE /workspaces/{id}` | service/caller | Disabled403; no family deletion. |
| `WorkspaceDataResetPrepareController.prepare` | `POST /workspaces/{workspaceId}/data-reset/prepare` | yes | local.requireCurrentRequestEpoch; WorkspaceResetSnapshotService.digest → lockCurrentEpoch |
| `WorkspaceDataResetPrepareController.confirm` | `POST /workspaces/{workspaceId}/data-reset/confirm` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceDataResetPreviewController.plan` | `POST /workspaces/{workspaceId}/data-reset/plan` | service/caller | Read-only selection validation. |
| `WorkspaceInvitationController.accept` | `POST /api/workspace-invitations/accept` | service/caller | WorkspaceInvitationService.accept → lockCurrentEpoch |
| `WorkspaceInvitationController.issue` | `POST /api/workspace-invitations/workspaces/{workspaceId}` | service/caller | WorkspaceInvitationService.issue → requireCurrentRequestEpoch |
| `WorkspaceInvitationController.preview` | `POST /api/workspace-invitations/preview` | service/caller | Read-only bearer-token preview. |
| `WorkspaceInvitationController.begin` | `POST /api/workspace-invitations/context` | service/caller | WorkspaceInvitationService.begin → lockCurrentEpoch |
| `WorkspaceInvitationController.resume` | `POST /api/workspace-invitations/context/resume` | service/caller | Read-only pending-context preview. |
| `WorkspaceInvitationController.revoke` | `DELETE /api/workspace-invitations/workspaces/{workspaceId}/{invitationId}` | service/caller | WorkspaceInvitationService.revoke → requireCurrentRequestEpoch |
| `WorkspaceJoinController.request` | `POST /api/workspace-joins/requests` | service/caller | WorkspaceJoinService.requestJoin → lockCurrentEpoch |
| `WorkspaceJoinController.rotate` | `POST /api/workspace-joins/workspaces/{workspaceId}/join-code` | service/caller | WorkspaceJoinService.rotateJoinCode → requireCurrentRequestEpoch |
| `WorkspaceJoinController.disable` | `DELETE /api/workspace-joins/workspaces/{workspaceId}/join-code` | service/caller | WorkspaceJoinService.disableJoinCode → requireCurrentRequestEpoch |
| `WorkspaceJoinController.approve` | `POST /api/workspace-joins/workspaces/{workspaceId}/requests/{requestId}/approve` | service/caller | WorkspaceJoinService.approveRequest → requireCurrentRequestEpoch |
| `WorkspaceJoinController.reject` | `POST /api/workspace-joins/workspaces/{workspaceId}/requests/{requestId}/reject` | service/caller | WorkspaceJoinService.rejectRequest → requireCurrentRequestEpoch |
| `WorkspaceMemberController.createMember` | `POST /workspaces/{workspaceId}/members` | service/caller | WorkspaceMemberService.createMember → requireCurrentRequestEpoch |
| `WorkspaceMemberController.updateMember` | `PUT /workspaces/{workspaceId}/members/{memberId}` | service/caller | WorkspaceMemberService.updateMember → requireCurrentRequestEpoch |
| `WorkspaceMemberController.deleteMember` | `DELETE /workspaces/{workspaceId}/members/{memberId}` | service/caller | WorkspaceMemberService.deleteMember → requireCurrentRequestEpoch |
| `WorkspaceResetChallengeController.challenge` | `POST /workspaces/{workspaceId}/data-reset/challenge` | yes | local.requireUnfencedReauthTransaction; local.requireCurrentRequestEpoch; WorkspaceResetSnapshotService.digest → lockCurrentEpoch |
| `WorkspaceResetChallengeController.execute` | `POST /workspaces/{workspaceId}/data-reset/execute` | service/caller | Disabled423; no consumption or domain mutation. |
| `WorkspaceRoleController.createDefaultRoles` | `POST /workspaces/{workspaceId}/roles/defaults` | service/caller | WorkspaceRoleService.createDefaultRoles → lockCurrentEpoch |
| `WorkspaceSessionController.select` | `POST /auth/workspaces/{workspaceId}/select` | service/caller | Membership-checked token issuance, global auth boundary. |
| `WorkspaceTemplateController.saveFromTask` | `POST /workspaces/{workspaceId}/templates/from-tasks/{taskId}` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.saveFromReward` | `POST /workspaces/{workspaceId}/templates/from-rewards/{rewardId}` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.create` | `POST /workspaces/{workspaceId}/templates` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.update` | `PUT /workspaces/{workspaceId}/templates/{templateId}` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.delete` | `DELETE /workspaces/{workspaceId}/templates/{templateId}` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.applyEconomySuggestionHttp` | `POST /workspaces/{workspaceId}/templates/{templateId}/creation-draft/apply-economy-suggestion` | service/caller | Read-only draft transformation via applyEconomySuggestion; no persistence. |
| `WorkspaceTemplateController.activateTask` | `POST /workspaces/{workspaceId}/templates/{templateId}/activate-task` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.activateReward` | `POST /workspaces/{workspaceId}/templates/{templateId}/activate-reward` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTemplateController.preview` | `POST /workspaces/{workspaceId}/templates/imports/preview` | service/caller | Read-only import preview. |
| `WorkspaceTemplateController.importSelected` | `POST /workspaces/{workspaceId}/templates/imports` | yes | local.requireCurrentRequestEpoch |
| `WorkspaceTokenController.activate` | `POST /api/me/workspaces/{workspaceId}/activate` | yes | Membership-checked token issuance, global auth boundary. |

## Upstream, internal and non-HTTP review

- TaskAuditService.recordUserEvent is subordinate to fenced TaskService/slot operations; standalone use still requires an already authorized/fenced transaction. Source guard absence there is not proof of safe direct use.
- Deadline scanning uses TransactionTemplate per family before scanning, generation reloads existing definitions after family lock, startup task backfill reloads per-family IDs. HTTP-triggered system jobs preserve original epoch; only explicit scheduled/current-state paths can use server current state.
- Economy getOrCreateSettings/getActivePointTypes/getAllPointTypes and MediaService.getBoundVariant contain server-derived write/backfill/access work. Their guards and real tests are listed; GET must not be assumed globally side-effect-free.
- Public invitation begin/accept and join request discover a target without row locks, lock target family current state and revalidate token/code. Email accept/acceptWithGoogle use private requireInvite to fence before active token row lock. New joiners have no target-family client epoch; this exception must not be generalized to client edits.
- Account/global self-service (email verification/reset, Google link/password, own-password changes, login/refresh) is intentionally outside family reset. Reset never deletes/rekeys these credentials. Current family-admin UserAccountService edits to a globally shared account remain a separate authorization/policy risk; epoch integration alone does not resolve it.
- Explicit new-family registration/onboarding/additional-family paths allocate a new family and use current-state provisioning/defaults, preserving unrelated selected-family header semantics. They are not reset.
- Workspace selection/activation issues membership-checked scoped JWTs; it does not erase business data. Scoped JWT lifecycle is distinct from dataEpoch stale-write quarantine.
- The new WorkspaceResetProofService is a trusted internal MANDATORY API with no public secret/consume endpoint; controller owns persistent attempt limiting before family fence. Consumption is NOT deletion authorization.

## Required next acceptance (still OPEN)

1. Approve exact owner identity/delegation and protected owner/membership/admin survival; no first-ID inference. Resolve global-account manager credential policy.
2. Approve financial/entitlement retention and per-table delete/archive order, including non-FK media polymorphism and template provenance. A pg_catalog FK list alone is insufficient.
3. Implement complete idempotent execute/one-use proof delivery/Google fresh reauth/atomic epoch+audit+onboarding, and inject failure at each actual deletion stage and commit failure boundary. Existing rollback tests deliberately throw before commit; no full executor/orphan/ledger success claim.
4. Exercise all critical service paths through real authenticated HTTP and deployed disposable fixtures, including current permission revocation and same global account in two families.
5. Test Android queued stale operations/caches and actual deployment parity. QA Test(14)/Test2(11) are NOT authorized for destructive tests.
6. Independent QA/consumer/Release approval and pinned all-green source remain required. No live reset/migration/deploy performed.

## Final CI acceptance — 2026-10-31
Exact tested SHA `b4f62fc9bd13b9c9aec223619f016bac2f7ebcc7`; [GitHub Actions 41249950772](https://github.com/o-kobylianskyi/family-points/actions/runs/41249950772) all four jobs SUCCESS. Backend: 329 tests, zero failures/errors/skips; PointMutationTransactionPostgresTest: 54 PASS. WorkspaceResetProofUpgradePostgresTest: 1 PASS, V44->V45 preservation/constraints/repeat-migrate no-op.
Backend source through14371d74 is included in this tested compatible descendant; later Web-only changes were preserved. Current HTTP DTO uses application's Boot Jackson3 serializer and contains no secret/verifier. Source inventory pin remains a4c0369a because its controller/service call paths are unchanged by the later test-only corrections. No live migration/reset/deploy or full executor acceptance.
