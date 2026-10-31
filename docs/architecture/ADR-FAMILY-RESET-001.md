# ADR-FAMILY-RESET-001 — Selective family data reset and restart setup

> **Current release-scope override — 2026-10-24:** Feature design remains valid but its **implementation is AFTER_INTERIM_BETA / ON HOLD** by explicit user priority decision ADR-COORD-006. This file is not a directive to start an API/UI/DB project during interim-BETA stabilization. See [post-BETA backlog](../coordination/POST_INTERIM_BETA_BACKLOG.md).

**Date:** 2026-10-24
**Product goal:** user requested an administrator-only family reset from settings, selection of categories to clear/preserve, password confirmation and ability to repeat first-run starter template setup.
**Status:** PRODUCT GOAL AGREED; specific API, retention policies and UI defaults PROPOSED for Backend/Web/Android/Release review. No reset implementation or actual deletion performed.
**Related:** ADR-TEMPLATES-001, IMP-0008 (starter setup); new IMP-0009 (destructive reset); global account/workspace isolation ADR-COORD-005.
**Handoffs:** XTR-20261024-P-05 Backend / -06 Web / -07 Android / -08 Release.

## 1. Entry point and modes

Workspace Settings → Family administration → **Reset family data** (danger zone). Must only appear to the authenticated manager who actually has platform-checked authorization for destructive family reset. Existing `MANAGE_WORKSPACE` or `ADMIN_OVERRIDE` is NOT automatically sufficient: introduce explicit privileged operation policy (e.g. original family owner + fresh authentication, or audited designated administrator after owner delegation). Never allow a child, ordinary member, delegated task/group manager or a global administrator of a DIFFERENT family.

Three UX presets built from the same underlying selection model:
1. **Restart progress**: preserve people, groups, task/reward definitions; reset specified balances/reputation and optional execution histories; defaults retain auditable ledgers where supported.
2. **Selective cleanup**: manually choose categories to clear/preserve and inspect dependencies, effects and remaining object counts.
3. **Restart initial setup**: preserve Workspace ID, name if chosen, owner account/global identity, owner membership and minimum family-administrator permissions; clear selected family data, mark setup DRAFT and open the SAME first-family starter wizard / template library defined in ADR-TEMPLATES-001. A reset must not create a new competing family/workspace or duplicate administrator credentials.

All destructive categories should initially be **unselected** in a custom flow. Preset selections are explicit and editable; show a dry-run before confirming. "Start from scratch" is a workspace reset, NOT the deletion of a global user's account or a wipe of unrelated families.

## 2. Selectable data inventory and dependency graph

The Backend produces actual counts from current tables and exact, versioned selection rules; below is a proposal, not a claim of full FK cascade support.

| Category / candidate checkbox | Keep/clear semantics | Dependencies and rules |
|---|---|---|
| Workspace display/config | Keep name, locale/timezone, currency defaults unless explicitly reset | Workspace ID and permissions baseline never deleted |
| Members / profiles | Keep by default; optionally remove **non-owner** members and family associations | Require explicit user selection; if real linked global accounts exist, remove workspace memberships **only**, do not delete UserAccount or its unrelated family memberships; block or require clear child tasks/ledger/groups/devices/invitations referencing them |
| People groups, GroupMembership, hierarchy, role assignments | Keep or clear group definitions/memberships | Do not delete core owner family administrative permissions; role catalog defaults restored only when selected, never casually reset access control mid-request |
| Task folders (WorkNode GROUP/TASK), TaskDefinitions, schedules/slots | Keep or clear | Removing tasks requires handling instances, approvals, audit, reward requirements/obligations, media bindings; no unreviewed ON DELETE CASCADE |
| Task executions, delegation, participation, approvals, audits | Separate history/progress selection | Deleting execution history while preserving definitions must not leave points/rewards inconsistent; historical security audit retained separately; avoid leaving orphan task IDs |
| Reward categories and RewardDefinitions/requirements | Keep or clear | Purchases, requests, redemption/obligations, task links and media follow explicit dependency resolution |
| Reward purchases/requests/obligations and settlement history | Separate selection | May represent already granted entitlements; require explicit warning and consistent points/reputation reconciliation; cannot "refund" automatically unless opted in and semantically defined |
| Points, group balances, reputation values | Balance zeroing separate from transaction history | When retaining history, **append auditable reset/adjustment entries** to exactly reach zero with server timestamp and resetId; never quietly rewrite historical ledger. If history explicitly removed, validate deletion of dependent data and atomic recalculation; keep immutable security reset audit |
| Currency/rates/economy/funding settings | Keep existing or reset to system defaults | Cannot remove point type referenced by retained ledger/task/reward without dependency planning; avoid restoring default currency while leaving dangling references |
| Custom images/media bindings/private uploads | Keep by default; optional unlink/delete workspace-owned unreferenced private assets | System curated catalog is **always preserved**; orphan cleanup follows media retention and ACL, no public leakage |
| Invitations/join requests / member email invites | Keep or cancel/revoke | Clear pending tokens securely if selected; no unintended new invitations |
| Device binding/policies/temporary sessions | Keep by default; recommend separate high-risk reset with warnings | Managed Android/device owner control may remain at OS level. Never imply that DB deletion removes Device Owner or device-side policies. Require safe re-enrollment flow and device confirmation |
| Offline point/task queues, scheduled jobs, client caches | Invalidate for ALL destructive resets affecting their data | Introduce `workspaceDataGeneration`/epoch and server-enforced stale-operation rejection, even if request arrived with old JWT or from an offline device |

Selections are not necessarily independent. Server must calculate **required co-clears**, **safe retain/archival strategies**, blocked combinations and the number of affected records. Preview renders a dependency explanation; the user must explicitly accept a re-computed selection when dependencies expand. No implicit deletion of additional categories merely because a checkbox was clicked.

**Non-selectable preserved invariants:** Workspace identity, at least one active admin with an authenticated global account+membership; other workspace data; platform-wide catalog and default artwork; security audit of reset; essential system schema/migrations; snapshots/backups according to retention policy. Delete family itself is a DIFFERENT action and not included.

## 3. Password re-verification and destructive-action protection

- A manager clicking Reset must complete a **fresh, server-verified authentication challenge** for their own GLOBAL UserAccount (not their child's password, not another workspace admin's credentials). The UI prompt alone is not authorization. For local password accounts, `PasswordEncoder.matches`/existing authentication manager validates current account password; no client-side-only check, never log/store/transmit plaintext in URL, analytics, journal or export.
- **Google-only or no usable local password:** do not bypass password gate. Provide a separately supported fresh Google re-authentication challenge of equivalent assurance, or require setting a local password through an already verified account-security flow before enabling reset. Password setup itself may require recent provider authentication. No account-linked `MANAGE_MEMBERS` password reset of another user counts as proving current actor identity.
- Separate server-issued short-lived, single-use challenge (suggest <=5 minutes) tied to (actorAccount, workspaceId, previewFingerprint, selectedScopeHash, workspaceDataGeneration). Apply explicit per-actor/IP rate limit, brute-force lock/cooldown and abuse audit; reject challenge reuse and stale access, revoke after completion/failure thresholds.
- Re-confirm with **typed family name** (localized explanation), final item counts and irreversible consequences. Button must be explicit "Delete selected data and restart", never same style as a normal Save.
- A new / old workspace JWT never bypasses authorization: verify membership, owner/admin rule, current password/reauth, preview lock and generation again at execution; enforce CSRF/session protections appropriate to actual auth transport, no general CORS wildcard.
- Audit: actor account/member, workspace, scope hash + category counts, resetId, started/finished timestamps, result and affected client generation; never plaintext secrets or private image bytes.

## 4. Preview + execute contract (PROPOSED; Backend owns final DTO)

- `GET /workspaces/{id}/reset/capabilities`: permitted modes/categories, dependencies, current counts and whether password or provider reauth available (no private records enumeration).
- `POST /workspaces/{id}/reset/preview` with selected category masks, preserve choices, intended `restartSetup` flag and optional catalog pack selection. Pure read-only dry-run: exact counts, graph of co-clears/blocks, warnings about devices/entitlements, estimated orphan media, owner/admin survival, snapshot readiness, immutable preview ID and fingerprint/version/TTL. No DB mutation, invalidation or balance changes.
- `POST /workspaces/{id}/reset/challenge`: prove actor's local password or verified recent provider authentication bound to exact preview; return one-use opaque challenge token. Never save password, return its validity only.
- `POST /workspaces/{id}/reset/execute`: require preview ID/fingerprint, challenge proof, exact typed family name, unique `operationId`/Idempotency-Key, and acknowledged dependency summary. Request must be idempotent; same op returns previous status and different payload with same key fails.
- `GET /workspaces/{id}/reset/operations/{resetId}`: account/owner-scoped outcome and recovery instructions. No user records/excessive personal details exposed after membership removal.
- For small transactional work, single locked workspace-scoped transaction with proper FK ordering and no partial committed state. For larger media/devices, durable state machine `PLANNED/EXECUTING/COMPLETED/FAILED_NEEDS_RECOVERY` with DB-backed outbox, per-stage idempotency and no partially open wizard on failure. Avoid asynchronous "200 success" before server verifies commit.
- Before mutation: block concurrent workspace writes/queued jobs as appropriate, take verified export/snapshot or create an explicit irreversible-data-loss warning if backup cannot be guaranteed. Server count/selection cannot drift since preview; else 409 and re-preview. Post-commit increment workspace generation **atomically** and invalidate sessions/caches; reject offline queued sync generated under the old epoch. Affected devices fetch fresh state. Other families tied to same account must remain untouched.
- `restartSetup=true` updates setupStatus DRAFT (or compatible wizard state), **preserving owner membership**, and presents existing onboarding/template wizard. Imported template provenance may need clearing/reinitialize so a fresh install is not incorrectly skipped, while retained user-edited records still follow dedupe rules in ADR-TEMPLATES-001. No automatic template install without user confirmation.

## 5. Balance and history policy

An "empty" balance is not achieved by writing `balance=0` if the economy derives balances from `point_transactions` / group transactions. Two explicit actions:
1. **Zero current balance, retain history**: insert an authorized reset ledger adjustment per member/group, currency and reputation type; show history and reset marker, do not erase evidence or invent purchases.
2. **Remove economic history** (only with explicit "clear history" + dependencies): ensure references to task completion, purchases, funding and group transactions are resolved and balances recalculate to zero (unless retained operations); preserve immutable administrative security audit. Do not advertise permanent delete while backups/logs are retained; display retention/expiry.

Likewise task/reward histories cannot be independently purged where a retained reward purchase still references historical tasks and ledgers. Present a blocked dependency or an explicit archive/preserve solution.

## 6. Failure, privacy, UX and release acceptance
- No requested destructive function is implemented merely by writing this ADR.
- Admin must see before/after exact category record counts, effect on linked members/other families, device warning, snapshot/export status; no prechecked all-delete in custom flow.
- Explicit "Download/export family data" before clearing where supported, securely delivered only to entitled manager with privacy rules. A tenant export does not equal validated one-click restore; do not promise undo unless a restore flow exists and is tested.
- The UI must not redirect into wizard until reset is verified server-side, all required generations invalidated and owner retains authorization.
- Test no deletion on preview, failed password, 409 stale snapshot, accidental double clicks, same reset retry, idempotent operations, concurrent Web/Android edits, delayed offline replay, two-family isolation incl. same global account in two families, child rights, token revocation/generation, device bindings and orphan image cleanup.
- Test keeping tasks with reset balances, keeping members while clearing tasks/rewards, removing non-owner with task/ledger dependencies, reset-then-reinstall starter template, skip wizard, failure during transactional delete, backup + recovery plan. Validate both Web and Android behavior with four locales.
- Never run delete or migrations on DEV/BETA/PROD without independent Release gate and explicit user confirmation. **This feature's construction is not a request to erase any real family.**

## 7. Phased release
**MVP safety-first:** admin settings flow + dry-run + reauthentication + start-from-scratch that preserves family identity/owner and clears a carefully reviewed cohesive set (tasks, rewards, histories, balances, category/groups) with audited integrity. Selective categories limited to dependency-safe combinations.

**Phase 2:** richer selective reset across member removal, media, device policies, offline queues and sophisticated history preservation/export, after full per-table FK dependency implementation and backup testing. Do not expose checkboxes that Backend cannot safely execute yet.

**Phase 3:** optional tenant export/import/restore (separate ADR), scheduled recovery snapshots and fine-grained archival. No automatic restoration guarantee.

### Open decisions for owner review
- Retention period / secure snapshot quota and restore offer.
- Exact supported category combinations in first release.
- Provider reauth UX for accounts created by Google only.
- Whether reset task/reward execution history should default keep or clear under "restart progress".
