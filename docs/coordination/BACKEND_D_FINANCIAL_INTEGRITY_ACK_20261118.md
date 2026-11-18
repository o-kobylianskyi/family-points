# Backend D — Financial Integrity ACK / handoff — 2026-11-18

## ACK to Architect

**ACK: Backend D accepted.** Scope is financial integrity / ledger / currencies / rewards / penalties / historical economic invariants and QA financial acceptance. Backend D does **not** own authorization, HTTP endpoints, archive layout, or Restore executor. Contract with Backend B: Backend D validates a signed source financial graph and supplies read-only conservation / provenance acceptance criteria; Backend B owns source-to-target ID mapping, database mutations, rollback and idempotency.

## Committed evidence

- `8f120d00b5e634ac1dd75bf49c253c0a6882dc5c` — require integral ledger IDs, source-family IDs.
- `e4068243ef7fe6830f87bfa66d79abc8cbc636e0` — fractional identity negative fixtures.
- `58ee9b7edc7a6615c1c6dd9a79b86bd6d42df74a` — source multi-currency/member partition tests, invalid currency and amount negatives.
- `982ad942ac501d7aa7b43499f14f8fe672ba293b` — require bounded integral source IDs, parent IDs, archived economics.
- `2cd79268682a9bbf58f713cdf99ac3a605cdc516` — negative fixtures for fractional task economics, invalid purchase economics and 64-bit overflow.

Source verifier: `src/test/java/com/olehkobylianskyi/familypoints/DisposableLedgerSourceGraphVerifier.java`; tests: `DisposableLedgerSourceGraphVerifierTest.java`.

**Verification limitation:** Tests have been committed but were NOT executed by Backend D in this handoff. GitHub commit workflow query and combined status for `2cd7926` returned no result. That API only exposes a limited set of workflow runs, so absence of entries is **not evidence** of CI pass or fail. Do not mark FIN-03 QA PASS / release-approved without pinned SHA Maven and actual CI logs.

## Financial closure / fail-closed contract

1. Preserve archived ledger provenance: source type, source ID, member ID, currency ID, signed amount and transaction type. Never infer or synthesize historical credits/debits.
2. For each `(source member ID, source currency ID)`, sum signed ledger amounts independently. On restore, compare against the target ledger after **explicit** Backend B ID mapping; require exact amounts, counts and ancestry, not just equal aggregate totals.
3. TASK/EARN and TASK/PENALTY must match the archived task's member, economic snapshot (amount and currency), and have no duplicated event of the same source task/event kind.
4. REWARD_PURCHASE/SPEND must equal the archived price, member and currency; reject duplicate debit of a purchase.
5. MANUAL ledger requires null source ID. Unknown `source_type`, including exchange/refund variants with no separately approved semantics, remains **unsupported / fail-closed**; never interpret these as MANUAL.
6. Reject malformed, fractional, overflowing, missing or duplicate IDs; refuse currency catalog inconsistencies and unsupported parent graphs.
7. A source-to-target mapping must be injective per entity category, preserve cross-family isolation, reject missing parents and preserve the source financial snapshot. Backend D's verifier is currently **test-only read-only**; it does not authorize production Restore.
8. Historical ledger and balance verification is not a substitute for transaction rollback, epoch fencing, fresh approval or audit; those belong to Backend B/C.

## QA / Backend B acceptance matrix

- Positive: mixed MANUAL + TASK/EARN + TASK/PENALTY + REWARD_PURCHASE/SPEND with multiple members and multiple point currencies; exact balance and lineage agreement.
- Negative: same purchase debited twice; duplicate TASK/EARN or TASK/PENALTY for one instance; foreign/unknown currency; mismatched member, amount, currency or historical snapshot.
- Negative: missing archived task/purchase parent; orphan mapping; cross-family parent; malformed/overflowing IDs and prices; missing signed category arrays.
- Negative: unknown/exchange/refund source type with no approved historical contract; no silent balancing or invented compensation event.
- Restore-specific: source-to-target renumbering preserves category-specific injectivity, row count and exact signed totals; mid-transaction failure rolls back all financial rows; retry does not multiply ledger entries; untouched family remains byte/row-equivalent.
- Always run only on an authorized disposable environment; no real-family reset or restore is authorized by this handoff.

## Status / blockers / next package

**Status: ACK + source verification hardening committed; TESTS_NOT_RUN / CI_UNVERIFIED; FINANCIAL_RESTORE_NOT_APPROVED; BKL-002 BETA HOLD.**

**Dependency:** Backend B/A must agree on portable protected `FAMILY_MEMBERS` mapping and parent catalog closure before claiming native financial restoration. Backend C approval transport and epoch gates are independent prerequisites. Backend D will keep archive/read-only financial checks isolated while Restore B matures.

**Next (FIN-04):** read-only financial source-to-target reconciliation contract and disposable PostgreSQL acceptance fixture, including exact provenance and rollback checks. Publish test counts, SHA and CI job links when genuinely verified.

## FIN-04 follow-up — 2026-11-18

- Committed `a634809a2518bce87cf4c01cdf2c08452b631a3b`: `DisposableFinancialMappingParityTest.java`, a read-only source-to-target ledger mapping verifier with six JUnit tests.
- Compares exact row lineage (ledger/member/currency/task/purchase ID mappings, amount, transaction and source type), not just aggregate totals.
- Requires per-category injective maps and exact ledger row coverage. Rejects duplicated mappings, absent parents, foreign member/currency, altered ancestry, and balanced fraudulent compensation.
- Backend B continues to own transactional Restore and PostgreSQL rollback tests. This fixture does not mutate DB, is not plugged into production API, and cannot unlock financial restore.
- **Evidence limitation:** GitHub commit-status and workflow-run lookups for `a634809` returned no entries. JUnit/Maven execution has **not** been observed; do not mark FIN-04 PASS before actual pinned SHA CI.
- QA handoff: execute `./mvnw -Dtest=DisposableFinancialMappingParityTest test` where Maven wrapper is available, or `mvn -Dtest=DisposableFinancialMappingParityTest test`; capture JUnit XML, SHA and CI URL, then incorporate mapped-ledger parity in disposable PostgreSQL Restore rehearsal.
