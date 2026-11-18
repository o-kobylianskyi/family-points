# B-A-04g — financial ledger source-provenance integration gate and B-A-04f red CI repair

**Date:** 2026-11-18 · **Backend A** · **Status:** test-source fixes committed; exact new CI required.
**Owner boundary:** Backend A supplies original protected source graph; Backend D defines financial semantics; Backend B consumes for native restore; Backend C authenticates owner. No production import/restore/retention changes in this package.

## Actual observed failing full CI (not speculative)
Run `44287295299`, exact SHA `25696945dc24cb6696af920e949cfc1cc6ed5eef`: Web, Android, Compose SUCCESS, Java Maven FAILED (744 tests, 3 failures, 0 errors, 0 skipped). Actual failed cases:
1. `DisposablePortableFinancialSourceClosureTest.historicalPurchaseMustRetainBothProductAndCurrencyParents`: synthetic REWARD_DEFINITIONS parent lacked `point_type_id`, while validation correctly mandates this parent currency.
2. `DisposablePortableFinancialSourceClosureTest.historicalTaskCurrencyNullIsExplicitAndNeverInvented`: synthetic TASK_DEFINITIONS parent omitted explicitly nullable `reward_point_type_id` and `penalty_point_type_id`.
3. `DisposablePortableFinancialEdgeSchemaPostgresTest.everyReviewedGraphEdgeExistsWithExplicitNullability`: observed migrated DB column `reward_purchases.reward_definition_id` nullable YES although logical portable source graph requires a non-null existing parent. Record physical nullability separately from replay eligibility; do NOT weaken logical source rule.

Repair commits: `d554266` updates source parent fixtures with explicit mandatory/nullable columns, `5f7d5d1`/ `1ed2d11` model observed physical nullable column without attributing an unverified migration cause, `1dd2e28` adds negative proof that null historical purchase parent and absent currency catalog are still rejected. No production schema change.

## Source graph handoff to Backend D/B — no invented provenance policy
Verified concrete source columns:
- `point_transactions`: `id, member_id, point_type_id, amount, type, source_type, source_id, created_at`; source_type is polymorphic and source_id nullable. For TASK provenance the historical source ID is **task_instances.id**, not task_definitions.id. Preserve source rows exactly.
- `reputation_transactions`: `member_id, amount, source_type, source_id, created_at`. Do not reconstruct missing history from task current value.
- `group_point_transactions`: `group_id, point_type_id, amount, type, source_type, created_at` (no `source_id` in V1 physical schema). Do NOT invent a missing link to a TaskInstance; request D policy for group-ledger provenance and exact replay confidence.
- `reward_purchases`: `reward_definition_id, member_id, point_type_id, price_amount, reward_title, purchased_at, status` and later fields. Historical product and denomination must be independently present; physical DB nullability for `reward_definition_id` does not mean an archive with missing parent is replayable.
- `task_instances`: original `member_id` and `original_assigned_member_id` remain separate; historical reward/penalty amounts and currencies must remain immutable during replay.
- `point_exchange_requests`: original member, processor, rate and from/to point types; never invent reciprocal ledger entries.

**Backend D decision requested:** exact whitelist and immutable signed source graph for polymorphic `source_type` variants (TASK/EARN, TASK/PENALTY, MANUAL, PURCHASE, POINT_EXCHANGE, REFUND, PENALTY_REVERSAL, GROUP); `source_id` interpretation per variant and event uniqueness; handling of group ledger **without** physical `source_id`; whether nullable historical reward parent can ever be represented with separately authenticated immutable parent evidence or must be a blocker. All unsupported/unknown categories remain blocked, not converted to MANUAL.

**Backend B decision requested:** exact vNext category naming, source graph ancestry and source->target remapping for ledger source ID and purchase price; no default original_assignee=executor, no lost-source DB lookup.

**Architecture approval still needed:** A4-01…09 versioned member wire, role catalog, hidden source dependencies, role privacy, IMP. Backend B had conditional *logical* ACK only. Continue test-only source validation until named signoff.

## Targeted QA (disposable only)
```bash
./mvnw -B -ntp -Dtest=DisposablePortableFinancialSourceClosureTest,DisposablePortableFinancialEdgeSchemaPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberExceptionClosurePostgresTest,DisposableLedgerSourceGraphVerifierTest test
```
Need actual pinned SHA full four-component CI and Maven Surefire test count. Do not mark B-A-04g PASS from previous run `44287295299` since it failed.

**Next B-A-04h:** exact-SHA CI triage and remaining physical/semantic source gap inventory; production vNext only after Architecture/B/D signed wire review. BETA HOLD, Reset Execute HTTP423, `restorable=false`, `replaySupported=false`.
