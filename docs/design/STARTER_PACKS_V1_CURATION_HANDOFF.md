# FamilyPoints — Starter packs v1: content differentiation

**2026-10-26 — Designer.** Catalog edit committed: `657554843dc08ae76fe95efb90aa335f7e8f2d55` on main. Scope: only `packs[].templateCodes` for two curated packs; all existing 142 `templates[]` entries, codes, four-locale titles, descriptions and monetary nulls are preserved.

## Revised pack composition
| Pack | After | Purpose |
|---|---:|---|
| preschool | 32 | Supervised elementary chores, play, self-care |
| early_school | 32 | Early school organization, age-appropriate chores |
| older_children | 36 | More autonomy, academic planning, sustained responsibility and optional privileges |
| mixed_age | 33 | Mixed preschool and school-age roles, shared tasks, family activities |
| adult_personal | 16 | Adult planning, no default points |
| family_shared | 20 | Shared calendar/household planning, no default points |

Previous older_children vs early_school similarity was 31/32; now they share **14** codes. Previous mixed_age exactly duplicated early_school 32/32; now they share **21** codes. Distinct curated starter sets intentionally overlap because hygiene/household baseline may apply at multiple ages.

Age indications are recommendation hints only, not account security or permission limits. All entries remain individually optional and should be editable after preview. This edit does **not** instantiate tasks, rewards, balances, ledger rows or alter existing families' private templates.

## QA handoff
1. GET /template-catalog returns 142 templates, 6 packs, lengths 32/32/36/33/16/20; all pack codes resolve.
2. Different packs render distinct collections; importing overlapping packs deduplicates selected codes.
3. Imported private templates already in families remain untouched by changes to global pack memberships.
4. Validate age hint display, four localizations and 0-selection skip.
5. Ensure optional TIME reward duration and economy null-price handling continue to behave correctly.
6. Review family-mixed content for inclusivity, supervision and child suitability; no gender assumptions.

**Implementation status:** content committed; CI and DEV HTTP/UI acceptance are not claimed.
