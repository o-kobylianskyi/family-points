# FamilyPoints — Designer handoff: starter seed-content catalog

**Status:** PLANNED / WAITING_FOR_TEMPLATE_SCHEMA. **Owner:** temporary Media Assets / Designer chat. **Coordinator:** Architecture. **Consumers:** Backend templates/seed importer, Web initial setup, Android templates/gallery, QA. **Priority:** after approved BKL-001/BKL-004/BKL-006 schema; does not block current early BETA.

## Assignment
Once Architecture + Backend confirm the final **template database schema, import DTO and versioning contract** (ADR-TEMPLATES-001), Designer prepares **initial realistic reusable content** for FamilyPoints rather than test records. Create structured starter collections for first-run family configuration and reusable private templates, with appropriate artwork and stable media catalog references.

## Deliverables
- **Task template bank:** age-appropriate household chores, hygiene, schoolwork, reading, physical activity, routines, cooperation/helping; title, short child-friendly instructions, category, suggested point reward, estimated duration, suggested recurrence/scheduling, age recommendation, adult approval, optional prerequisites and image/icon key.
- **Reward template bank:** material goods, privileges, games/media time, family activities, personalized options; name, description, category, suggested prices/currency, approval requirement, reward kind (ITEM/TIME), default duration for TIME, availability/reuse limits and matching image keys. App-linked rewards can be examples only after BKL-009 contract exists.
- **Family starter packs:** preschool, early school, older children, mixed-age household; optional role/group templates and default currencies; recommendations rather than hard age/gender restrictions. Users can review/edit/skip each item at initial setup; no automatically assigned historical completions, balances or purchases.
- **Media pack:** consistent icons/art for each category plus item icons where useful. Reuse already approved repository assets when possible; produce new originals only for uncovered entries, keep image provenance/licensing record. Avoid third-party trademarks/copyright characters as default seed assets without explicit rights.
- **Four-language content:** Ukrainian, German, English, Russian labels/descriptions. The data model should distinguish localized display names from stable language-independent IDs. Include suitable currency declension references rather than hardcoded quantity text.
- **Seed artifacts:** versioned machine-readable JSON/CSV adhering to Backend import schema, unique stable keys, source/provenance list, human-readable review sheet and a manifest of expected media files. If the schema is not finalized, designer may research and draft an inventory in a separate document; **do not invent final DB columns, execute SQL, or merge incompatible fixture data**.

## Acceptance
- All records validate against the **approved** schema; IDs stable and collision-free, references resolve, no private member/workspace identifiers or synthetic completed tasks, no destructive DB writes.
- Clear separation between **global reusable templates** and **workspace-instantiated records**; one import is idempotent and does not overwrite user-customized data or seed duplicate rows. Backend owns importer, migrations and uniqueness enforcement.
- Reasonable diversity without stereotyping: tasks/rewards suitable for children with different ages, needs and interests; adult-controlled settings clearly distinguish suggestions from rules.
- Validate localization, monetary points quantities, durations, icon accessibility at small size, fallback when image absent, and initial setup preview/opt-out.
- QA receives coverage matrix after Backend importer and Web/Android consumers implement; current BETA rollout unaffected.

## Dependency and handoff sequence
1. **Architecture/Backend:** publish and approve task/reward/family template schema, DTO, allowed enums, currency model and media keys.
2. **Designer:** assemble content bank + localized metadata + approved images and manifest; submit for Architecture/Backend validation.
3. **Backend:** validate versioned seed manifest, implement idempotent importer and tests, do not silently overwrite user data.
4. **Web/Android:** display first-run preview, allow choice/edit/skip and assign templates to workspace.
5. **QA/Release:** verify actual import and UI in a later BETA update.

**Do not begin actual seed INSERTs until schema approval.** This request is a Designer brief, not authorization to deploy.

## 2026-10-26 — Confirmed product requirement: suggested starter template catalog
**User explicitly requested** initial prepopulation with a **list of suggested templates**, authored by Designer. This is a deliverable of BKL-001/004/006, not merely a documentation wishlist.
- **Designer ownership:** curate and review actual named task/reward templates and pre-grouped family starter collections; each item includes stable ID, four-language text, suggested age/context (nonbinding), optional icon key, recommended points/currency/duration/schedule and opt-in defaults. Provide a human-readable suggested catalog for approval before packaging.
- **Backend ownership:** publish versioned template JSON contract/schema + validation; consume Designer manifest into a GLOBAL read-only catalog through bundled seed resources or versioned data import, idempotent by stable key/version. No direct SQL instructions for Designer; never auto-create user tasks/rewards or modify existing families without explicit confirmation.
- **Web/Android ownership:** during onboarding and later from Template Library, display suggested catalog immediately, support preview, customize, select/deselect, skip, safe apply. Suggested presets are available on fresh install even before any family has chosen a template; no dependence on manually entering seed records in UI.
- **Acceptance:** nonempty curated suggested list after catalog installation, localized labels (UK/DE/EN/RU), working media fallback, no duplicate catalog entries after restart/reimport, existing family edits preserved, selecting a pack does not generate history, balances or ledger transactions. Seed manifest review precedes import test and QA release.
- **Sequence:** Architect/Backend approve contract → Designer supplies *real initial content* and artwork refs → Backend validates/bundles manifest and importer → Web/Android exposes suggestions → QA accepts. Release separately authorizes deployment.
