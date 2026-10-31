# FamilyPoints — Economy suggested preset v1.0 — Backend handoff

**Status:** PRELIMINARILY APPROVED AS DESIGN BASELINE, **NOT** an approved executable seed, runtime migration, or numerical final sign-off. Source: user's 2026-10-26 preliminary approval of v0.8 and Architect dispatch.

## Deliverable
`economy_suggestions_v1.json` contains **110 task mappings** and **32 reward mappings**, joined **only** by stable global catalog codes; four-language labels remain in `catalog-v1.json`. No workspace/member/point-type numeric IDs appear.

| Kind | Total | With optional numeric suggestion | Not set (null) |
|---|---:|---:|---:|
| TASK | 110 | 11 | 99 |
| REWARD | 32 | 12 | 20 |

## Consumer contract — requires Architecture/Backend approval
1. The preset is a **read-only proposal resource**, not an executable DTO or database migration. Backend decides its path, schema and import behavior.
2. A `null` amount is **UNSET**, never equal to 0. A `null` reward price must not become a free purchasable reward. Activation requires editable form, selection of an active workspace currency and explicit confirmation.
3. Difficulty is a separately editable rating and never computed solely from time. Suggested task points remain independent optional values; confirm work context and fairness.
4. No automatic import/overwrite into existing families, no ledger writes, no retroactive edits. Suggested values can apply only after preview and explicit adult review.
5. For TIME rewards, purchasing does not start the consumption timer; device limits/permissions remain authoritative.
6. Group/task recurrence and offline retry must never double-credit the same occurrence; duplicates and corrections use Backend idempotency and auditable compensating ledger entries.
7. Even a non-null recommendation is **not** permission to monetize healthcare, education, basic care or family contact.
8. If one of the 142 catalog codes is renamed, fail validation rather than silently misassigning prices.

## QA acceptance before enabling
- All 142 codes exist once in version 1 global catalog; language labels stay in global catalog.
- 110 TASK and 32 REWARD; no duplicates, orphan codes, workspace IDs or mutable monetary history.
- Null behavior verified across Web / Android / Backend; both clients permit editing and skipping.
- Cross-family isolation; no imports of balances or private history; exact-once task credit and spending.
- Verify priority conflicts: existing workspace manual values always win and stay unchanged.
- DEV regression + CI, release authorization separate.

## Open approvals
Architecture must finalize enumerated difficulty values, rounding, point prices, rate controls and actual preset Backend contract. Proposed values are editorial baseline only. No BETA deployment authorization is implied.
