# B-A-02 — Portable original FAMILY_MEMBERS source contract (proposal)

**Date:** 2026-11-18. **Owner:** Backend A (Backup & Portable Archive). **State:** PROPOSED — Architecture + Backend B ACK required before changing production exporter/inspector/format. **Baseline reviewed:** main source at 2026-11-18. **No live data access or execution.**

## 1. Evidence from real repository schema
- `V1__baseline_schema.sql` creates `family_members(id, family_id, family_role_id, member_type, name, active, deleted_at)`; member type is PARENT/CHILD/OTHER; family role is an FK. `WorkspaceMember.java` uses the same fields. `softDelete()` sets `active=false`, `deleted_at=now` and overwrites `name` with “Видалено”. Original name is therefore *not* recoverable from a later snapshot. Do **not** invent historical names.
- `V21__account_workspace_memberships.sql` creates `account_workspace_memberships(id, account_id, family_member_id, created_at)`, unique member/pair. `V22__unique_account_workspace_membership.sql` adds `workspace_id`, unique `(account_id, workspace_id)` and a DB trigger requiring `family_members.family_id == account_workspace_memberships.workspace_id`.
- `V29__global_account_identity_foundation.sql` allows nullable legacy `user_accounts.family_member_id` and creates global `account_external_identities` keyed by provider+subject. `V31__allow_shared_account_email.sql` explicitly permits duplicate emails across accounts; email is *not* identity.
- `AccountWorkspaceMembership.java`: member-to-account link is one-to-one **inside a particular workspace**, while an account can have multiple workspace memberships. `UserAccount.java` contains password hash, username, email, email verification, global enabled and optional legacy member link.
- `GroupMembership.java`: member→member_group link plus group_membership_roles. This is NOT the same thing as an account→workspace membership. Both may be referenced by future graph closure.
- `WorkspacePortableBackupExporter.java` and `ProtectiveBackupImportInspector.java` each allow 23 archived categories but omit FAMILY_MEMBERS; inspector deliberately emits `IDENTITY_REBIND_REQUIRED` for member references. `WorkspaceResetSnapshotService.java` category list likewise omits FAMILY_MEMBERS.
- **Existing behavior is safe to preserve:** authenticated AES sealed archive, snapshot digest and epoch, offline inspection, no production Restore Execute, HTTP 423 reset execute, no VERIFIED promotion.

## 2. Proposed logical vNext source catalog (NOT yet a production JSON schema)
```json
{
  "sourceMemberId": 123,
  "sourceWorkspaceId": 55,
  "sourceRoleId": 7,
  "memberType": "PARENT",
  "activeAtSnapshot": false,
  "deletedAt": "2026-11-01T12:00:00",
  "displayNameAtSnapshot": "Видалено",
  "membershipEvidence": {
    "hasLinkedAccount": true,
    "sourceAccountBinding": "opaque-archive-local-reference",
    "linkActiveAtSnapshot": true
  }
}
```
Fields and names here are **design placeholders**, not already existing database columns or finalized DTOs. Use exact original numeric member ID as *source-graph reference only*. Keep immutable source-workspace ancestry inside the authenticated archive. Never treat sourceMemberId, sourceRoleId or sourceAccountBinding as an authorized target ID.

### Privacy / authentication boundaries
- Do **not** export password_hash, email verification credentials, OAuth provider_subject, sessions, JWT, raw email, username, invitation tokens, reset challenges, private auth metadata, or global account profile as a side effect of member backup.
- Archive-local account linkage should be opaque and only within the source family capture. Need explicit Architecture decision whether linkage is included at all and how account provenance is proven after source DB loss.
- Source role FK requires family-owned role catalog/role_code closure or explicit non-restorable blocker; never carry source role id as effective target authorization.
- Soft-deleted members must remain as source graph nodes where financial/task history references them. Preserve snapshot active/deleted state; NEVER use deleted display name to re-identify accounts.
- New target login/guardian access is never granted by a backed-up role; owner must reauthenticate and explicitly approve target member/account pairing in Backend B's separate guarded transaction.

## 3. Category closure and scope semantics requiring decision
- If an archived row references member_id, created_by_member_id, assigned_member_id, requested_by_member_id, processed_by_member_id, buyer, actor or similar, include an authenticated source member node or produce a precise fail-closed blocker. Enumerate actual FK and polymorphic columns from PostgreSQL metadata, not guesses.
- `FAMILY_MEMBERS` should be a hidden mandatory dependency for any category with member references, while still inspectable as a separately named category for audit; final scope UX requires Architecture confirmation.
- Decide whether selected recovery can include historical member *identity nodes* without restoring active membership, login or role. Recommended: identity nodes can be used for audit/replay provenance only; no automatic account creation.
- Distinguish `ACCOUNT_WORKSPACE_MEMBERSHIPS`, `GROUP_MEMBERSHIPS` and `GROUP_MEMBERSHIP_ROLES` if/when included; these have different FK and consent/security semantics.
- Source archive must be self-contained enough to validate complete graph even if source PostgreSQL no longer exists. External source DB lookup cannot be required for consumer verification.
- Current `family-points-export-v1` structure and manifest `schemaVersion` require a compatible migration strategy, not an unannounced in-place change. Legacy archives remain parseable where supported and must stay non-restorable when member ancestry is insufficient.
- `@Transactional` without explicitly verified isolation does not alone prove a single point-in-time snapshot. Review capture/digest transaction isolation before claiming atomic consistency.

## 4. Acceptance cases to implement after approval
| ID | Input | Expected producer/inspector safety outcome |
|---|---|---|
| A-MEM-01 | One source family with parent and child | All original member IDs, family and role ancestry authenticated |
| A-MEM-02 | Same global account in two families | Export contains only selected family member, no neighbor details |
| A-MEM-03 | Two global accounts sharing one email | No identity conflation; emails not used as keys |
| A-MEM-04 | Soft-deleted member referenced by ledger/task | Preserve tombstone source node and source FK ancestry |
| A-MEM-05 | Archived row has member ID missing from catalog | Deterministic MISSING/DANGLING member blocker |
| A-MEM-06 | Member sourceWorkspaceId belongs to another family | Reject archive/export, no cross-family leakage |
| A-MEM-07 | Duplicate source member ID or wrong role family | Reject before any restorable classification |
| A-MEM-08 | Mutated ciphertext or altered sealed manifest | Authentication failure, no partial inspection |
| A-MEM-09 | Old archive missing FAMILY_MEMBERS | Backward-compatible inspect if supported; remain not restorable |
| A-MEM-10 | Family changes during export | Snapshot abort/retry with conflict, never mixed epochs |
| A-MEM-11 | User/row/media limit exceeded | Fail explicitly; never truncate |
| A-MEM-12 | FULL or SELECTIVE includes financial rows | Source member/catalog dependency closure verified; no auto account/permission restore |

## 5. Implementation phases / ownership
1. **B-A-02** (this package): verified schema inventory, draft vNext producer contract and QA negative matrix. Documentation only.
2. **B-A-03:** after Architecture + Backend B ACK, implement additive source selection/digest/export and read-only inspector catalog validation with explicit archive versioning and privacy. PostgreSQL 18 synthetic two-family tests.
3. **B-A-04:** full scope FK closure, group memberships/roles vs account memberships, financial history and media dependencies. Consumer review for each source contract.
4. **B-A-05:** source transaction/epoch atomicity, limits, retention and crypto regression; exact-SHA CI and independent QA packet.

## Required Architecture + Backend B decisions
- Approve minimum identity evidence, account-link privacy level, tombstone semantics, and roles catalog ancestry.
- Approve new archive wire-format version and legacy behavior.
- Approve mandatory dependent categories and FULL/SELECTIVE behavior for historical membership.
- Backend B must ACK fields it needs for identity pairing, source FK remap and orphan proof; Backend A cannot open Restore Execute.

**Safety status:** No migration, archive format, auth, runtime source, data, CI or production change in B-A-02. BETA remains HOLD. Existing `restorable=false`, `replaySupported=false`, HTTP423 unchanged.
