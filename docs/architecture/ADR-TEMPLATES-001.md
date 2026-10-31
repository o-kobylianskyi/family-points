# ADR-TEMPLATES-001 — Starter family and reusable tasks/rewards templates

> **Current release-scope override — 2026-10-24:** Feature design remains valid but its **implementation is AFTER_INTERIM_BETA / ON HOLD** by explicit user priority decision ADR-COORD-006. This file is not a directive to start an API/UI/DB project during interim-BETA stabilization. See [post-BETA backlog](../coordination/POST_INTERIM_BETA_BACKLOG.md).
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


## 2026-10-26 — Selective onboarding import into private template library (user requirement)
- Initial setup shows a curated list of suggested global templates with **independent checkboxes** on every task and reward, and optional pack-level **select all/clear all** as convenience only. No forced import of a whole pack.
- Selection defaults must be explicit and clear; show selected count and preview. The user can inspect and optionally edit individual templates before confirmation.
- **Crucial distinction:** confirmed selection copies only the selected template *definitions* to the workspace's **private template library**, not immediately to executable TaskDefinitions or purchasable RewardDefinitions. Creation/activation of live tasks/rewards is a later deliberate action, unless explicitly offered and independently confirmed.
- Unselected suggestions stay available in the global catalog for later import. A user can skip all without blocking onboarding.
- Import must be atomic, idempotent per workspace + template key/version and operationId, preserving existing private edits and preventing duplicates. Dependencies must be shown: if a selected template depends on another unselected item, prompt to add the dependency or omit its requirement with explicit consent; never silently insert extra templates.
- Catalog updates never overwrite user-owned private templates; imported template provenance is retained. Tests cover partial selection, zero selection, different packs, repeat import and cross-workspace isolation.
- Backend API contract must expose selection identifiers and a preview of the **private-library copy** impact; Web and Android implement the checkbox UI, preview, and explicit confirmation.

## 2026-10-26 — Optional installed-app binding and future device enforcement (user decision)
Reward templates for games/app time must support **both** ordinary offline/manual rewards and an optional association with an installed Android app. Do NOT conflate reward kind TIME with mandatory app launch.
- Template metadata: `deliveryMode = MANUAL | ANDROID_APP_OPTIONAL` (extensible), optional human-readable app suggestion/title/icon catalog reference; no global template may hardcode a user's installed app, device ID, or device-local icon URL. A `packageName` or device-app reference is a **workspace/device assignment set later** by parent/device, not global seed content. Android obtains package label/icon and validates installed package at bind time; Web can show synced display metadata with safe fallback.
- At redemption/start, optionally offer **Launch app** when matching package is installed and accessible, or **Continue without launch** for the same reward. Missing/uninstalled app does not silently cancel a valid manual reward; explicit policy flags can govern launch-required cases in the future.
- Distinguish: entitlement/purchase, actual consumption session start, optional OS app launch, elapsed-time tracking, session expiry, and enforcement. Use BKL-008 server-authoritative startedAt/expiresAt and one-time 5/2/1/expiry notifications; BKL-009 handles package binding + Android foreground launch. Launch alone must never be treated as proof of an OS-level block.
- **Future BKL-010, conditional device-control enforcement:** when a supported device management mode has sufficient privileges, monitor app usage and end or block further access after allotted time, including rules for reboot/offline, re-entry, foreground/background switching, multiple devices, administrator overrides, and accessibility. Android capability constraints matter: ordinary apps cannot reliably force-stop arbitrary third-party apps; hard restrictions require Device Owner / managed-device capabilities or other OS-supported management. Explicitly expose enforcement level (`NONE`, `REMINDER_ONLY`, `MANAGED_BLOCK`) and report unsupported devices without claiming restrictions are active. No hidden accessibility abuses or claims of unconditional forced termination.
- Template schema should reserve extensible `appReward` config as OPTIONAL and `enforcementPolicy` for a later migration; never tie app-specific settings to generic ITEM/TIME templates; no immediate database schema is approved by this paragraph.
- Test matrix: manual game, linked installed game, missing app, app changed/uninstalled, launch failure, time expiry, offline resume, same reward used on different devices, management permission revoked, child tries to relaunch after expiry, parent override. Never falsely mark `blocked` from timer alone.

## 2026-10-26 — Adult and whole-family scheduling: not child-only (user requirement)
FamilyPoints template suggestions and personal/family calendars target **adults as well as children**. Adults can create schedules for themselves, an individual family member, or the whole family. Typical scenarios: cleaning, a walk, household errands, appointments with a physician/authority, travel and vacations. Do not impose child-age requirements, reward points, or an approval workflow on adults by default.

**Separate semantics in catalog/domain and UI** (not merely a different category):
- `TASK`: actionable work with status/completion, optionally recurring, may be assigned to oneself, another authorized member, a group or family; points/reputation are optional and default to none for adult planning.
- `EVENT`: dated or all-day family/personal calendar occasion (appointment, trip, holiday, visit), with start/end, timezone, optional location, attendees, reminders and notes; not completed/graded and no ledger side effects.
- `ROUTINE`: reusable recurring pattern of tasks/events (daily chores, weekly walk, school/work timetable), with safe recurrence rules and exceptions. Could be a catalog template kind or schedule metadata; contract to be finalized before code and should reuse existing TaskDefinition recurrence where appropriate.

**Scope/audience:** `SELF`, `MEMBER`, `GROUP`, `FAMILY` are planning targets (authorization must be checked on Backend); catalog target audience `ADULT`, `CHILD`, `FAMILY`, `ANY` are recommendations/filtering only, never a basis for permissions or stereotypes.

**Important boundary:** existing Calendar DTO aggregating task deadlines/reward availability must not be assumed to support persistent appointments. Introduce an explicit event model/API (or prove an existing equivalent) with tenant-scoped participants, organizer/permissions, timezone and reminders. Do not store appointments as fake TaskInstances or reward purchases. Scheduling a medical appointment may be sensitive; default to minimal metadata, private visibility controls, no public/global leakage. Calendar reminders require consent and notification settings.

**Onboarding UX:** Designer supplies adult-oriented and shared household recommendations alongside child items; filter by purpose and age-appropriateness, allow independent selection and import to family-private template library; no automatic live appointments/tasks. Nonselected templates remain globally available, including later event/routine recommendations after schema support.

**Implementation:** Extend schema proposal/DTO first to allow catalog types TASK/REWARD/EVENT and scheduling metadata without breaking existing clients. Implement EVENT persistence/creation only in a separately scoped, tested package; do not pretend current read-only global catalog API already supports it. Test recurring events, timezone/DST, per-member visibility, edits/cancellations, privacy, personal vs family views, no points ledger mutation.
