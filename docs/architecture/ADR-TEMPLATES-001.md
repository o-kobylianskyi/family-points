# ADR-TEMPLATES-001 — Starter family and reusable tasks/rewards templates
**Date:** 2026-10-24. **Status:** User-requested product goal AGREED; detailed data/API contract PROPOSED for consumer review. No implementation/test/deploy claim.
**Impact:** IMP-0008. **XTR:** 20261024-P-01 Backend, -02 Web, -03 Android, -04 Release.
**Earlier design:** docs/MULTI_FAMILY_ONBOARDING_PLAN.md (M3/M4); ADR-CAL-001 (reward windows), ADR-MEDIA-001 (global stock icons).

## Product experience
Two entry points use ONE engine:
1. **First-family setup wizard** after workspace creation: family participants/role slots, optional real members, choose pack or individual examples, customize and map real members, preview all changes, explicitly confirm. Skip, back and resume always available.
2. **Template Library** for existing families: search/select one task, group, reward or a complete bundle; edit before adding; save existing workspace tasks/rewards as **private reusable templates** (second milestone). Existing family never overwritten by catalog update.

Proposed starter sizes (examples, numbers subject to content review): MINIMAL (~8 tasks/4 rewards/3 categories), FAMILY (~22/10/5), EXTENDED (~38/18/8). Modular packs: home/chores, school & reading, hygiene, sport, habits, weekend/shared family, preschool 3–5, school age 6–9, teens 10–15, adult duties and screen-time rewards. Filter by optional age band and interests; **age/gender/preferences suggest only**, never determine permissions, suitability as a fact, or auto-assign. Avoid stereotypes. Language keys: uk/de/en/ru.

## Correct domain boundaries
- Family: Workspace, WorkspaceMember with actual confirmed member name and WorkspaceRole; template starter creates **placeholders** parent/child-1 etc, never fake persisted users or child accounts/passwords. Adult confirms each member and mapping; no default privileged role grants from client.
- Groups of people: MemberGroup + memberships, nested group relations. Distinct from WorkNode(GROUP) task folders.
- Task folders: WorkNode(GROUP) hierarchy; executable TaskDefinition with WorkNode(TASK). Current TaskService creates WorkNode(TASK) using parentTaskDefinitionId, **not task-category parent ID**. Backend must implement safe task-to-folder attaching/import, not conflate with MemberGroup.
- Reward groups: existing flat RewardCategory; RewardDefinition has many-to-many categories. Do NOT invent nested reward categories in v1.
- Task defaults: title/description, icon catalog key, recurrence, dueTime and daily slots, points/reputation and optional penalties, assignment policy and placeholder, responsible role, mandatory flag; avoid pre-generating TaskInstance. Note scheduledTime is a deadline, not a promised start.
- Reward defaults: category refs, points/price, cooldown, approval, quantity limits, prerequisites pointing to selected catalog task **codes** (resolved to local task IDs upon import). One-time/weekly time availability uses ADR-CAL-001 once its PR/API is actually integrated. Do not silently drop dependency if excluded from selection.

## Catalog and install invariants
Global read-only, server-controlled, immutable versioned content catalog (stable namespaced code/version, localized text, bundle membership, dependency keys, approved system media key); no family/child personal data, no stored global mutable family objects. Controlled JSON resources/seed appropriate for v1, independent of tenant rows. "Save as private template" creates workspace-scoped sanitized copy of definition only; never copy ledger, purchases, audit, members or authentication secrets. Global publishing/sharing is deferred.

**Select → preview → edit → assign → explicit apply**. Preview is side-effect-free and shows precise created counts, categories tree, points/prices/recurrence, unresolved actors/prerequisites, duplicates and permissions. Default advanced penalties/mandatory approvals OFF unless parent explicitly chooses; installing creates NO task completions, purchases, balances, point/reputation entries, invitations or device controls.

Apply is server-authoritative with workspace manager permission, actor and referenced ID scoping, transaction, identity lock/concurrency control, mapping between copied groups/tasks/categories/rewards and template keys, catalogVersion+preview fingerprint, required operationId/idempotency key. Same (workspace,operationId,payload) repeats the same response; conflicting payload fails, no partial rows. Deduplicate using installed provenance key and version not localized name. Default SKIP_EXISTING; never silently overwrite user edits; intentional extra copies require explicit flag/name/new idempotency ID. Reject unresolved dependencies or offer removing requirements with explicit confirmation.

Proposed API (final DTO owned by Backend): GET `/template-catalog`; POST `/workspaces/{id}/template-installs/preview` (no DB write); POST `/workspaces/{id}/template-installs` (idempotent, one transaction); GET `/workspaces/{id}/template-installs`; optional private template CRUD later. Return mapping, created/skipped entries and 403/409/422 reason codes, no arbitrary user-supplied role grants. Schema stores install batch+item provenance, pinned global catalog code/version, target ID, workspace and audit. Choose Flyway numbering only after checking actual main and calendar V35/media V36 merge state.

## Tests and release boundaries
Two isolated families; active workspace switch; child cannot import/change member rights; concurrency/retry; server restart; missing currency/group/selected prerequisite; task folder parent and RewardCategory assignment; multi-slot/deadline; existing family duplicate/skipped; later add-one; old clients; uk/de/en/ru; no imaginary member/transactions; empty skip flow; no side effects during preview; atomic rollback; change of member/point settings after preview → 409/repreview; catalog key/version migration. Test real PostgreSQL fresh+upgrade and scoped API, final CI. Release separately approves BETA/PROD after backups and impacted clients ACK.

## Phasing
1. Backend global catalog, preview/atomic import, starter 2–3 packs + workspace provenance. Web first-run wizard + existing-family library. Static default icons only.
2. Workspace-private save-as-template and partial updates; Android native template library/wizard after current calendar/media UI milestones.
3. Future: optional cross-family template sharing, AI selection, template subscriptions, reward category trees — not part of this implementation.

This extends the 2026-10-12 first-run plan, not a parallel second onboarding system.

## Optional restart after selective family reset (2026-10-24)
The user also requires an administrator-only **reset family data** settings function with selectable categories to clear/retain, fresh password confirmation and option to rerun this same initial setup wizard. Reset does not recreate Workspace/global UserAccount or affect other families; it preserves admin access, real member mapping unless explicitly selected and catalog version. After server-confirmed reset+workspace generation change, wizard state becomes DRAFT and template packs may be selected and installed again without unintended old provenance dedupe. See ADR-FAMILY-RESET-001, IMP-0009; details are PROPOSED until implemented.
