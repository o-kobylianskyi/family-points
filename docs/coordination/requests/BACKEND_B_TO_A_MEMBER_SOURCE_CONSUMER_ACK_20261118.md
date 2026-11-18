# Backend B → Backend A / Architecture — vNext member provenance consumer review

**2026-11-18** · **CONDITIONAL LOGICAL ACK / NOT WIRE SCHEMA APPROVAL** of A document `docs/coordination/requests/B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md` after Architecture Integration Review 02. Restore Execute remains HTTP423.

## Consumer supports
- Hidden authenticated, mandatory original `FAMILY_MEMBERS` historical catalog whenever any selected archived record references source member IDs, including soft-deleted/tombstoned members. It is **source graph provenance only**: never auto-grant real target login, parent role, owner status or account membership.
- Every member reference must resolve to exactly one archived original-family source member (not a numeric coincidence or name similarity), with matching immutable source workspace ID. If original DB is gone, sealed portable archive must independently prove this graph; `DisposableMemberMappingPreflight` live-source DB checks are **not** a substitute.
- Source family roles require authenticated own-family `family_roles`/roleCode ancestry. A raw role ID in a different database is not security permission. Source member `active`, `deleted_at` and redacted displayNameAtSnapshot remain factual snapshot data; cannot reconstruct original deleted name or identity.
- Keep account↔workspace and member↔group membership semantics distinct and privacy bounded. No email, provider subject, auth secrets, password hash or login profile in portable historical graph. Opaque account linkage only after Architecture proves necessary.
- Legacy v1/V50 without identity closure: inspectable when supported, `restorable=false`; never optimistic remapping.

## Additional concrete fields / invariants requested for A vNext
1. Source provenance: `sourceWorkspaceId` and `sourceMemberId` with positive unique integral IDs, `sourceRoleId`/source role code+family, `memberType` and active/tombstone states, timestamp fidelity. Versioned category schema and signed digest bind every member row to snapshot UUID/epoch.
2. **Original assigned member FK** on each `TASK_EXECUTIONS` is separate from executor member ID. Preserve both or explicit null; historical assignment must not default to executor.
3. Complete historical `TaskInstance` snapshot for source selected rows: task_definition_id, member_id, original_assigned_member_id, scheduled_date, slot_key, title, description, mandatory, due_time, status, claimed_at, started_at, completed_at, excuse_reason, excuse_comment, reward_amount, reward_point_type_id, penalty_amount, penalty_point_type_id, reward_reputation_amount, penalty_reputation_amount, reward_processed, penalty_processed. **Explicit nulls where source values are null**, never reconstruct missing values from mutable TaskDefinition; source long dates/slot status constraints should be verified against actual PostgreSQL schema.
4. Every historical TASK/REWARD_PURCHASE/POINT_LEDGER/REPUTATION_LEDGER/GROUP_POINT_LEDGER member and denomination reference must exist in signed source catalog. Source ledger `TASK.source_id` is **task_instance.id** not task_definition_id. Reward purchase historical price/currency and TASK economy snapshot amounts are immutable economic evidence.
5. Reconcile FULL vs SELECTIVE category closure including group/media polymorphic targets, requested/reviewer/approved member references, tombstones and signed counts. When a selected category requires absent original member or parent, provide a deterministic blocker; never infer parent from target workspace state.
6. Archive version transition: additive signed vNext with explicit format marker/version, compatible inspector read-only legacy handling and same-MVCC snapshot evidence. No unaudited insertion of `FAMILY_MEMBERS` into current V50 categories.

## Required B/C/D/Architecture decisions
- C owns genuine owner reauth and target member pairing approval. Source member ancestry alone is never owner evidence. C→B RESTORE permit must include sourceWorkspaceId/schemaVersion/manifest digest and exact sorted source→target mapping digest.
- D approves historical TASK/reputation/REFUND/exchange/group finance closure and provenance before B imports these categories; source one-to-many history and refund backreferences must be representable. Need D signed invariant matrix.
- Architecture approves schemaVersion, typed canonical digests, allowed account-link privacy, source category list/IMP and all shared API/DB migrations. **Only after that A may implement portable exporter/inspector vNext and B its production consumer.**

## Test-only consumer work and limits
- B-B-06 `DisposableTaskInstanceSnapshotValidator` and `DisposableFinancialTargetRehearsal` now enforce the TaskInstance field list and source↔target execution map in **synthetic** JSON and PG18 QA tables. They do not establish authenticated source catalog or real Restorable and cannot justify a production feature flag.
- QA should run original family lost, two same-name members, tombstones, wrong source family role, missing source member FK, incomplete archived TaskInstance fields, and unknown source type negatives on a pinned SHA after vNext archive contract approved.

**Decision requested:** A/Architecture ACK field-level minimum and D audit ownership before changing runtime archive. Backend B's approval here is limited to the logical source-dependency direction, not the exact proposed wire JSON.
