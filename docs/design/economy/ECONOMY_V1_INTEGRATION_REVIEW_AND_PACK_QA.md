# FamilyPoints — Designer integration review after economy v1

**Date:** 2026-10-26. **Status:** source-level review; no DEV HTTP or BETA certification. **Owner:** Designer.

## Confirmed Backend source integration
- `src/main/resources/templates/economy-suggestions-v1.json` includes Designer's 110 TASK + 32 REWARD codes.
- `TemplateEconomySuggestionsService` loads and validates metadata, exact catalog code coverage, unique codes, nullable/nonnegative monetary suggestions and read-only defensive copies.
- `TemplateCatalogController` now exposes `GET /template-catalog/economy-suggestions`.
- `TemplateEconomySuggestionsServiceTest` has two source tests for 110/32 coverage, numeric 11/12, null retention and defensive copy.
- These are source facts only; **no green CI run, DEV HTTP verification or runtime rollout was inspected by Designer**. In particular, service presence does not establish UI consumption or ledger safety under activation.

## New pack-content QA issue
Current packs:
| Pack | Count |
|---|---:|
| preschool | 32 |
| early_school | 32 |
| older_children | 32 |
| mixed_age | 32 |
| adult_personal | 16 |
| family_shared | 20 |

Confirmed by direct code comparison:
- **early_school and mixed_age are exactly identical**: 32/32 codes.
- early_school and older_children overlap 31/32 codes.
- preschool and early_school overlap 24/32 codes.
- Overlap by itself is expected; **exact identity under different labels may be an unintended UX defect**. Do not edit `catalog-v1.json` until Architect/Product accepts revised composition.
- QA should continue testing idempotent import across overlapping packs regardless of eventual content correction.

## Designer recommended correction proposal (NOT APPLIED)
1. Make `mixed_age` intentionally combine a broader age range, with examples for school-age children and supervised preschool tasks, while retaining optional checkboxes.
2. Make `older_children` more distinctive than `early_school`, prioritizing age-appropriate autonomy and responsibility without excluding by age or gender.
3. Keep personal adult and shared family planners separate, with no default monetary incentives.
4. Preserve stable template `code` values. A pack membership change is a curated global-content update, not a permission to overwrite families' already imported private definitions.
5. Compare onboarding display count and ensure no duplicate cards from selecting multiple packs; Web/Backend dedupe by code.

## Acceptance gates
- Architecture approval for pack composition and preliminary numerical economy policy, separately.
- Backend verify economy API (null and missing/foreign codes), point type selection, no auto-activation; do not infer acceptance from unit tests alone.
- Web/Android preview and edits with unset amount distinguished from free/0; four language display.
- QA tenant access, duplicate concurrent import, mixed-pack selected-only import and ledger invariants on a disposable DEV workspace.
- Release requires pinned SHA and its own smoke evidence.

**Result:** Designer economy v1 data is integrated as read-only Backend source. It is **not** ready to call released. The exact duplicate pack is flagged for product decision.
