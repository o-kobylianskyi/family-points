# FamilyPoints — Architecture dispatch 2026-10-26 (priority refresh)

**Owner:** Architecture. **Status:** ASSIGNED, implementation evidence separate. Read the exact latest main and its CI before acting; snapshot at issue: 995889670, CI 40398812683 IN_PROGRESS.

## P0 immediate — Android (single owner) + CI
The previous 9f89da4 Android failure (TextButton imports and missing language) was **already fixed in d3fb1119e / 535a25054**. Do NOT redo or describe this as a new unassigned coding defect. Android must check the new run 40398812683 and obtain all-four-jobs green; fix only genuinely outstanding Kotlin/Compose errors; then emulator QA for collapsed 3-row secondary lists, selection state, four-language expand/collapse, narrow screens. DEV debug remains emulator-only; Huawei signed BETA only.

## P0 / P1 templates v1 coordinated completion
**Backend:** run targeted real-seed PostgreSQL regression (142 catalog items, 6 packs) including import idempotency, overlapping pack codes, workspace operation keys and tenant access. TIME reward 30min mapped to draft. Do not auto-activate templates or touch wallets/ledgers. Close API contract and tests for explicit activation only after consumers and QA accept it.
**Web:** continue structured local draft editing; verify family-scoped member and active-currency selectors, authorizations, validation, TIME duration and nullable recommended price; do not wire unsafe direct activation without Backend approval. Recent fixes cf3df61/24e55ba recorded; browser DEV QA still needed.
**Android:** once CI green, test task/reward seed import preview, selected-only private library, duration, 4-language descriptions and cross-workspace identity on emulator; fix confirmed issues in one package.
**QA:** target TPL-01..TPL-15 and new TPL-03A overlapping starter packs. Validate null != zero, single import across overlapping packs, replay operation key across different template codes rejected, private library isolation, explicit-only activation, Web/Android smoke. BKL-007: currency backfill for older families and local display tests. Report reproducible defects instead of blanket release blocking.
**Designer:** 142 starter seeds accepted as source inventory (110 TASK, 32 REWARD; 6 packs); economy v0.6 review not approved. Complete per-code difficulty/point/price justification and identify 65 unassigned-to-pack templates; none should be automatically priced or applied.
**Release:** after new green CI pin Backend/Web schema V39/V40 + seed, quick live login/catalog/private-library/TIME/currency smoke, publish early test BETA. Do not await broad QA; do not reset DB. Release remains independent of implementation percentage.

## Architecture economy decision queue
Designer ECO-001..ECO-012 is reviewed but has no recorded formal approval. Principles to provisionally accept for contract design: difficulty independent from duration, configurable optional suggestions only, null does not mean free/zero, no payment for basic needs/normal family contact, no auto-ledger writes, exact-once earnings. Proposed numerical point rates and reward prices remain UNAPPROVED. After template v1 publish, separately specify and approve the actual economy v0.6 contract. BKL-008 countdown (5m=1 beep, 2m=2, 1m=3) and BKL-009 app-reward launch remain upcoming packages; no parallel feature sprawl.

## Reporting protocol
Every track reports: last committed SHA, CI job, DEV validation, remaining bugs, next owner. Update its `docs/coordination/logs/*.md`. No automatic claim that this dispatch sends cross-chat messages; shared GitHub is the coordination channel.
