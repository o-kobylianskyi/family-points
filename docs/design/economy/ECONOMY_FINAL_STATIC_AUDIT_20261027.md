# FamilyPoints — Designer economy v0.7 / v1 seed final static audit (2026-10-27)

**Audit type:** read-only content/static consistency, **NOT** DEV/API/financial E2E approval. Current runtime source `src/main/resources/templates/economy-suggestions-v1.json` is identical to Designer `docs/design/economy/economy_suggestions_v1.json` in parsed JSON. Preliminarily approved model must remain optional and editable.

## Coverage and integrity
| Check | Verified result |
|---|---|
| Global template catalog | 110 TASK + 32 REWARD = 142 |
| Suggestion codes | 110 unique TASK + 32 unique REWARD |
| Missing/orphan/duplicate codes | 0 / 0 / 0 |
| TASK numeric points | 11 proposals; 99 null |
| REWARD numeric prices | 12 proposals; 20 null |
| Total numeric / unset | 23 / 119 |
| pricingState | 23 OPTIONAL_SUGGESTION; 119 UNSET |
| Source/model version | 1, PRELIMINARY_APPROVAL_INTEGRATION_CANDIDATE |
| Source matches design copy | yes |
| Workspace mutation / auto apply | explicitly false / false |
| Adult approval before activation | true |

## Time is not difficulty
Suggestions differentiate effort and responsibility from pure duration. Distribution: EASY 16 (2–10 min), MODERATE 82 (5–75 min), HARD 11 (5–30 min), ADVANCED 1 (30 min). Overlapping time ranges demonstrate no simple monotone time-to-difficulty conversion. **Editorial caveat:** a consistent evaluation scale for cognitive effort, autonomy and responsibility is still not machine-verifiable from these fields; leave parent editability and BETA tuning.

## Numeric TASK proposals
| Code | Difficulty | Minutes | Points |
|---|---|---:|---:|
| fp.task.routine.007 | EASY | 5 | 5 |
| fp.task.routine.009 | MODERATE | 7 | 10 |
| fp.task.chores.013 | EASY | 10 | 10 |
| fp.task.chores.014 | EASY | 10 | 10 |
| fp.task.chores.015 | HARD | 20 | 25 |
| fp.task.chores.016 | MODERATE | 15 | 15 |
| fp.task.chores.017 | MODERATE | 15 | 15 |
| fp.task.chores.018 | MODERATE | 10 | 10 |
| fp.task.chores.020 | MODERATE | 10 | 10 |
| fp.task.chores.023 | MODERATE | 10 | 10 |
| fp.task.chores.024 | MODERATE | 10 | 10 |

## Numeric REWARD proposals
Time: time.001 25, .002 45, .003 35, .004 60, .005 55, .006 30.
Items: items.021 50, .022 60, .023 250, .024 300, .025 200, .026 350.
All prefixes `fp.reward.`; these values are optional initial points amounts, not guarantees or free purchases.

## Acceptance safeguards and review issues
1. **Never coerce null → 0** in Backend DTO/Web/Android editor or persistence, especially 20 rewards without price. Undefined amount means no default; purchase requires explicit valid amount/parent choice.
2. **No blanket earning per occurrence**: routines 007/009 and chores 013/014/020 include repeated-award flags. Verify server earning uniqueness/limits, offline retry and two-parent concurrency; separate source recommendation from award policy.
3. Six screen-time rewards have parental-controls flags. Physical-item prices are family-specific; no amount should be enforced as mandatory global value.
4. The catalog includes basic hygiene, learning, relationship and family-time entries with null suggestions. Never make basic care, essential schooling, or family relationship access contingent on earning or spending.
5. The previous `economy_v0.8_per_code_architect_audit.json` recorded pack membership *before* the curation commit `657554843d`. Its per-code `packs`/pack coverage and `unlistedCodeNames` must be treated as stale and re-generated before comparing 65 off-pack codes. Do not edit price/difficulty records to fix pack membership.
6. No signed-off normative point scale, paid reward purchasing E2E, DEV API, Android/Web numeric preview, data reset or release evidence from this content audit. Preserve `PRELIMINARY` status.
7. Existing private family templates and financial records remain untouched; reset must be specifically authorized on disposable DEV family.

## Recommendation to Architecture / Backend
**CONTENT INTEGRITY PASS** (142/142, unique codes, 23 optional values, null and non-automatic invariants). **BETA ACCEPTANCE PENDING**: validate 4 locales, two-family isolation, template import, null price, explicit activation, exactly-once earning/redeem, time reward draft and reset/re-onboarding, then calibrate numeric values from user BETA feedback. No unsolicited rewrite of seeds or balances.
