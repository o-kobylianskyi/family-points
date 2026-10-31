# FamilyPoints — Designer pack QA and media icons v1.2

**Date:** 2026-10-26. **Source baseline:** `main`, after starter-pack composition commit `657554843d`. **Status:** SOURCE VERIFIED; DEV LIVE QA PENDING.

## Confirmed in GitHub
- 142 global entries, packs in sequence: **preschool 32 / early_school 32 / older_children 36 / mixed_age 33 / adult_personal 16 / family_shared 20**.
- Pack membership is no longer identical between early_school and mixed_age. Stable codes remain unchanged.
- Latest observed head during this audit: `35c0ea3120`. CI run **40403507796** was `pending` at audit time. Do not infer green CI or deployed DEV from this.
- Existing static media contains fallback icons `task-default.svg`, `reward-star.svg`, `task-group-default.svg`; those are not 18 category-specific approved assets.

## DEV QA — execute against deployment pinned to matching SHA
| Case | Procedure | Expected |
|---|---|---|
| DSP-01 | `GET /template-catalog` authenticated/where supported | 142 codes, six packs, latest membership sizes |
| DSP-02 | Open early_school / older_children / mixed_age in Web | Distinct membership, counts 32/36/33 |
| DSP-03 | Select overlapping packs | Distinct codes deduplicated in preview; no repeated cards |
| DSP-04 | Import a subset into disposable family | Selected private definitions only, no active tasks/rewards or ledger |
| DSP-05 | Repeat the same import | No duplicate private template codes |
| DSP-06 | Choose zero entries | No changes to family |
| DSP-07 | Switch UK/DE/EN/RU | Localized titles and descriptions; no leaked UK-only preview names unless known limitation shown |
| DSP-08 | Existing family imported old pack previously | Previously saved private definitions unchanged |
| DSP-09 | Navigate time reward preview / creation draft | Duration remains correct; nullable price not free |
| DSP-10 | Fetch `/template-catalog/economy-suggestions` | 110 TASK, 32 REWARD, 11/12 optional numeric suggestions; null preserved |
| DSP-11 | Cross-family attempt to access private template | Denied, without existence disclosure |
| DSP-12 | Verify icons at 24px light/dark | Fallback stable until new media is actually delivered |

All cases **PENDING** until a test result with SHA/request and screenshots or response evidence is supplied.

## Graphic delivery plan
The companion `icon_manifest_v1.json` defines 18 category icon **concepts only**. No PNG/SVG assets are claimed created. Existing approved generic fallback icons remain in place; new category icons require an explicit graphic batch, visual sign-off and real media registry keys before catalog references are assigned. Keep avatars separate.

## Next steps
1. Backend/QA: ensure DEV runs catalog code with new pack memberships; record CI status and HTTP evidence.
2. Designer: produce visual drafts for 18 category pictograms, review contrast/cropping at 24px and in light/dark theme.
3. Architecture: decide approved names/keys and what to do with 65 entries outside any pack; no automatic selection.
4. Only after approval update media mappings and re-run QA.
