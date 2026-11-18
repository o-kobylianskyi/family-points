# Backend B — B-B-01: V50 source→target restore dependency matrix

**Date:** 2026-11-18. **Owner:** Backend B (Restore Engine). **Status:** SOURCE-REVIEWED / CONTRACT REQUESTED, **NOT** production-ready. **Canonical source files reviewed:** `WorkspacePortableBackupExporter.SCOPED_TABLES`, `ProtectiveBackupImportInspector.CATEGORIES` and `inspect` parent guards, `DisposableLedgerSourceGraphVerifier`. No code or producer archive format mutated by this review.

## Source facts / security boundary

The present V50 exporter and inspector recognize **23** categories. Neither provides a portable `FAMILY_MEMBERS` category. For many member-bound rows the exporter selects the source family using live SQL membership, but the portable archive cannot independently prove a member's historical original-family identity after the source DB has disappeared. The inspector explicitly adds `IDENTITY_REBIND_REQUIRED` rather than authorizing import. Its inspection always includes `RESTORE_ENGINE_NOT_IMPLEMENTED`, `FK_AND_IDENTITY_MAPPING_NOT_APPROVED`, `TARGET_CONFLICT_PREVIEW_REQUIRED`, `PROTECTIVE_TARGET_SNAPSHOT_REQUIRED` and `restorable=false`. **Do not reinterpret successful AES parsing or zero blockers in a subset as restore authorization.**

## Exact 23 V50 categories / restore-consumer mapping

| Category | Source scope | Restore dependency / mapping | Consumer disposition |
|---|---|---|---|
| `PRIVATE_TEMPLATES` | workspace_id | workspace rebinding; local code/version and owned resource references | target conflict preview; no silent overwrite |
| `MEMBER_GROUPS` | family_id | family; member-group membership references require external identity policy | group ID remap before group-linked rows |
| `TASK_DEFINITIONS` | family_id | family; currency/reward policy and assignee dependencies as actually present | task IDs before executions/slots |
| `REWARD_DEFINITIONS` | family_id | family, currency and category link context | reward IDs before purchase/request/obligations |
| `TASK_EXECUTIONS` | family_members membership | original member_id + task_definition_id; historical reward/penalty amount/type snapshots | source member proof required; mapped task first |
| `REWARD_PURCHASES` | family_members membership | member_id; reward_definition_id optional; reward_request_id optional; original point_type_id, price | preserve historical purchase ID ancestry |
| `TASK_SCHEDULE_SLOTS` | definition family | task_definition_id | task before slot; do not synthesize historical schedules |
| `REWARD_REQUESTS` | family_id | requested_by_member_id + optional reward_definition_id and approved_point_type_id | explicit original member and currency rebinding |
| `REWARD_REQUIREMENTS` | family_id | exactly one owner of reward_definition_id or reward_request_id; optional task_definition_id | resolve owning parent first |
| `REWARD_CATEGORIES` | family_id | family; code/category metadata | category ID remap |
| `REWARD_OBLIGATIONS` | family_id | reward_purchase_id; optional requirement_id; member_id | purchase and owner mapping required |
| `REWARD_OBLIGATION_BLOCKED_CATEGORIES` | owner obligation family | obligation_id + category_id | obligation/category IDs first |
| `REWARD_OBLIGATION_BLOCKED_REWARDS` | owner obligation family | obligation_id + reward_definition_id | obligation/reward IDs first |
| `REWARD_DEFINITION_CATEGORIES` | derived reward family | reward_definition_id + category_id | many-to-many after both parents |
| `WORK_NODES` | family_id | work-node graph and links to family content if present | parent/child graph order & cycle protection review |
| `MEDIA_BINDINGS` | workspace_id | target_type WORKSPACE/MEMBER/MEMBER_GROUP/WORK_NODE/TASK; target_id; exactly one asset_id or catalog_key | polymorphic rebind; MEMBER awaits original member identity; media before binding |
| `MEDIA_ASSETS` | workspace_id | UUID identity + authenticated private binary variants; referenced by media binding | hash/size/tenant proof before binding, never copy arbitrary owner |
| `GROUP_POINT_LEDGER` | member group-derived family scope | group_id + point_type_id + polymorphic source_type/source_id | group and denomination first; historical source proof required |
| `POINT_TYPES` | family_id | original point type UUID/ID, code and metadata to mapped target currency | validate code uniqueness; NEVER equate numeric ID across DBs |
| `POINT_EXCHANGE_RATES` | family_id | from_point_type_id/to_point_type_id; original ratio and approval | currencies first |
| `POINT_EXCHANGE_REQUESTS` | member membership | member_id; exchange_rate_id; from/to point types; optional processed_by_member_id; stored ratio snapshots | member/currencies/rate first; preserve original historical terms |
| `POINT_LEDGER` | member membership | member_id + point_type_id + source_type/source_id polymorphic reference | complete original financial ancestry; no blind amount replay |
| `REPUTATION_LEDGER` | member membership | original member_id and historical source metadata | original member ancestry and reputation semantic closure required |

## Explicit topological dependency stages (planning, not approved replay order)

0. **Preconditions:** authenticate source artifact / selected-category completeness and immutable source digest, target workspace scope, fresh owner approval, epoch and unique restore operation in one transaction. Current portable identity and owner trust contracts are incomplete.
1. **Stable target identity:** original family-member catalog/role/active/deleted/history proof, manual owner-reviewed sourceMember→targetMember map; account credentials/real-user identity excluded from automatic reset.
2. **Foundations:** family/workspace; `POINT_TYPES`, `MEMBER_GROUPS`, `WORK_NODES`, `REWARD_CATEGORIES`, `MEDIA_ASSETS`.
3. **Definitions:** `TASK_DEFINITIONS`, `REWARD_DEFINITIONS`, `REWARD_REQUESTS`, `POINT_EXCHANGE_RATES`, optional PRIVATE_TEMPLATES (its internal references require inspection).
4. **Relations and children:** task slots/executions, reward definition↔categories, requirements, purchases, exchange requests, reward obligations + blocked category/reward relations, media target bindings.
5. **Financial history:** `POINT_LEDGER`, `REPUTATION_LEDGER`, `GROUP_POINT_LEDGER`; source_type/source_id graph validated against earlier mapped parent identities; order can require two-phase key allocation because source graph polymorphic.
6. **Verification/commit:** target financial aggregates per original member × denomination, category row counts/FKs, source→target immutable ID map proof, media content and provenance, durable restore result receipt. Roll back all writes plus permit consumption on any mismatch.

The stages are conservative and may require two-pass FK allocation. The exporter list order itself is **not** a guaranteed insertion or commit sequence.

## Mandatory selective dependency edges / deny if unselected

- `TASK_EXECUTIONS → TASK_DEFINITIONS + ORIGINAL_MEMBER_IDENTITY`; `TASK_SCHEDULE_SLOTS → TASK_DEFINITIONS`.
- `REWARD_PURCHASES → ORIGINAL_MEMBER_IDENTITY + POINT_TYPES + (REWARD_DEFINITIONS or REWARD_REQUESTS when referenced)`.
- `REWARD_REQUESTS → ORIGINAL_MEMBER_IDENTITY + optional REWARD_DEFINITIONS + POINT_TYPES when approved point type present`.
- `REWARD_REQUIREMENTS → exactly one REWARD_DEFINITIONS/REWARD_REQUESTS + optional TASK_DEFINITIONS`.
- `REWARD_OBLIGATIONS → REWARD_PURCHASES + optional REWARD_REQUIREMENTS + ORIGINAL_MEMBER_IDENTITY`.
- `REWARD_DEFINITION_CATEGORIES → REWARD_DEFINITIONS + REWARD_CATEGORIES`; blocked relations → REWARD_OBLIGATIONS + REWARD_CATEGORIES/REWARD_DEFINITIONS respectively.
- `POINT_EXCHANGE_RATES → POINT_TYPES`; `POINT_EXCHANGE_REQUESTS → POINT_EXCHANGE_RATES + POINT_TYPES + ORIGINAL_MEMBER_IDENTITY`.
- `POINT_LEDGER → ORIGINAL_MEMBER_IDENTITY + POINT_TYPES + source-specific historical parent`; for POINT_EXCHANGE exactly two request legs (SPEND/EARN) with original request ratio, no third leg.
- `GROUP_POINT_LEDGER → MEMBER_GROUPS + POINT_TYPES + source-specific parent`; `REPUTATION_LEDGER → ORIGINAL_MEMBER_IDENTITY + historical source parent proof`.
- `MEDIA_BINDINGS → source asset or catalog + target type-specific parent (including original MEMBER identity)`; private assets have UUID identities and binary content proof.
- Existing target rows **cannot** silently substitute for an omitted archived parent without a documented, tenant-verified, owner-approved stable binding. A missing parent must fail closed or stay in preview with an explicit blocker.

## Critical cross-track gaps / acceptance owner

| Gate | Backend A deliverable | Backend B consumer invariant | Decision maker |
|---|---|---|---|
| Source memberships | privacy-reviewed signed original roster including inactive/deleted/historical membership semantics; new archive version/compatibility | prove EVERY archive member reference belongs to original family; exact target mapping | Architecture + A + B |
| Currency identity | authenticated original code/type metadata and source IDs for all finance parents | validated target ID map without numeric coincidence; ledger economic parity | A ↔ B |
| Source histories | complete polymorphic provenance, refund reason/caller source mappings, point/reputation/group distinctions | match every historical event, reject orphan/mixed source | Architecture + A + B |
| Media | portable content digests, reference ownership and variant rules | verify bytes before any bindings; target ACL/tenant isolation | A ↔ B |
| Authorization | immutable snapshot/epoch and approval binding surface | real owner reauth, durable one-use state, same-SQL-transaction consume | Architecture/Security + B |
| Selective restore | category scope/dependency closure and excluded-category semantics | preview target conflicts, choose fail-closed or approved remap, never implicit deletion | Architecture + A + B |
| Release | artifact content hash/format and crypto verification | full synthetic PG18 regression, QA original/target proof, exact SHA four-component CI | QA + Release |

## Verified current limitations and stop conditions

The source inspector currently explicitly returns `RESTORE_ENGINE_NOT_IMPLEMENTED` and `restorable=false`. Existing synthetic BKL-002 tests verify selected finance/ID-map/rollback properties, but **do not** provide an authenticated portable membership history, full restore engine, durable owner credential, source-target immutability receipt, or full category closure. Production Reset Execute HTTP423 and BETA HOLD remain. This document is a consumer design and contract-gap handoff, **not** proof of implementation or approval to deploy.

**Tracking:** `docs/coordination/requests/BACKEND_B_RESTORE_CONTRACT_REQUEST_20261118.md`; `docs/coordination/handoffs/BACKEND_B_LATEST.md`; QA Issue #15.
