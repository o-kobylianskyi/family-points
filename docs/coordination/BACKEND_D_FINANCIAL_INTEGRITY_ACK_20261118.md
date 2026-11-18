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

## FIN-05 checkpoint — 2026-11-18

- Commit `f676ab07f5ca12541eddd699bdbcce4bec3a6ace` adds six tests for deterministic repeat reconciliation, duplicate post-retry ledger entry, partial target snapshot, modified transaction type, swapped ID mappings and missing categories.
- Fixed null mapping categories to reject with explicit SecurityException rather than an accidental List.of NPE.
- This is read-only snapshot checking, **not** a PostgreSQL rollback test. Backend B owns PostgreSQL transactional Restore test coverage (e.g. `c5a3b44054cc02999cb286c02417767c1f286908`). No destructive operations performed.
- **Tests/CI have not been verified.** Targeted execution: `mvn -Dtest=DisposableFinancialMappingParityTest test`, capture pinned SHA and logs. Release remains HOLD.
- Next FIN-06: signed source archive -> source-to-target mapping -> per-ledger financial reconciliation, pending explicit Backend B mapping contract.

## FIN-06 signed source-to-target financial acceptance — 2026-11-18

- Committed test-only `DisposableSignedFinancialParityBridgeTest.java` at `b053884584f8b70f5c5f69eee636d6cab0bd5e45`; compensated tamper fixture corrected at `ef9b79ee91b4990ea1f6d548f66ecb5840b33e83`.
- Uses a sealed V50 portable archive, calls `DisposableLedgerSourceGraphVerifier.inspect` for authenticated source graph closure, reopens the same signed source to read `POINT_LEDGER`, and runs `DisposableFinancialMappingParityTest.reconcile` against explicit ledger/member/currency/task/purchase ID mappings.
- Positive: mixed TASK/EARN, REWARD_PURCHASE/SPEND, MANUAL/ADJUSTMENT across two members and two currencies preserves exact signed balances.
- Negative: duplicate purchase debit in the signed source, unsupported POINT_EXCHANGE origin, and compensated mutation to two target rows with unchanged aggregate total.
- Security boundary: no production Restore API calls, no source-to-target writes, no authorization bypass, no DEV/PROD mutation. This is a read-only acceptance bridge only.
- **Verification:** Maven/JUnit and exact-SHA CI execution remain pending. Suggested targeted command: `mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableFinancialMappingParityTest,DisposableSignedFinancialParityBridgeTest test`. Confirm with actual JUnit XML and pinned GitHub Actions logs. BKL-002 release stays HOLD.
- **Next FIN-07:** verify archive-to-target parity against disposable PostgreSQL data written by Backend B's QA-only fixture. Do not duplicate or modify Restore ownership; wait for Backend B to publish a stable mapping/fixture access contract.

## FIN-07 PostgreSQL financial ledger parity — 2026-11-18

- Commit `d00f3aa69745cf16ca0a04703cf3da9559434ad6`, corrected in `c6bd7f06f1ec91a5ce32aeec2bec74934253b80d`: added `DisposableFinancialLedgerPostgresParityTest.java`, five PostgreSQL Testcontainers tests.
- The fixture uses PostgreSQL 18 and a QA-only `qa_fin07_ledger` table keyed by operation UUID, with a Spring transaction boundary. It does **not** mutate production `point_ledger` or call Restore executor.
- Acceptance: exact per-row source-to-target financial parity after SQL reads, rollback after injected mid-write failure, retry from clean rollback, duplicate-replay insert rejection, compensated provenance tamper rejection despite equal net balance, and isolation of independent operation UUIDs.
- FIN-06 remains separately responsible for authenticated source archive V50 -> mapping parity. FIN-07 does **not** yet consume the signed archive itself or prove native financial Restore.
- **Verification:** Source committed only. Targeted command `mvn -Dtest=DisposableFinancialLedgerPostgresParityTest,DisposableSignedFinancialParityBridgeTest,DisposableFinancialMappingParityTest test` (Docker required); Maven/Testcontainers and exact-SHA CI execution are NOT yet confirmed. No PASS claim without logs.
- **Next FIN-08:** integrate a signed archive fixture with the PG-read verification layer, or compare against Backend B's explicitly published real-table disposable Restore fixture once stable. Maintain BKL-002 HOLD until full owner/QA/release gates.

## FIN-08 signed V50 -> PostgreSQL parity — 2026-11-18

- Commit `a84a1197196d07165789cdc3dd6d347847c516ed`: added `DisposableSignedFinancialPostgresParityTest.java` with four Testcontainers PostgreSQL 18 acceptance tests.
- Flow: seal authenticated V50 archive -> source graph verifier `inspect` -> decrypt and materialize its read-only source rows -> explicit mapping -> write to **QA-only** PostgreSQL `qa_fin08_ledger` in a transaction -> query DB rows -> exact provenance and member/currency parity verification.
- Tests: valid signed provenance, injected mid-transaction rollback + clean retry + duplicate replay denial, balanced but altered persisted ledger rejection, and invalid signed source rejection before any DB writes.
- Scope boundary: This is NOT native Restore. It uses only ephemeral QA table and synthetic mapped ledger rows. Backend B retains Restore executor and actual production-schema transaction ownership. No DEV/PROD data touched.
- **Evidence:** implementation committed; Maven/Testcontainers and pinned CI results NOT VERIFIED. Suggested `mvn -Dtest=DisposableSignedFinancialPostgresParityTest,DisposableSignedFinancialParityBridgeTest,DisposableFinancialMappingParityTest test` with Docker available; capture full JUnit XML and exact SHA. BKL-002 BETA remains HOLD.
- **Next FIN-09:** source/target persisted ledger cross-tenant and cross-operation negative testing, then architect/QA gate review. Native Restore readiness must not be inferred from these synthetic fixtures.

## FIN-09–FIN-11 large package — 2026-11-18

**Architect ACK: Backend D delivered maximal independent financial QA package without altering Restore executor or protected real-family data.**

- **FIN-09 — mapping provenance hardening**, commit `de3de69158c5f9597a318aeba65033e964ed8ef9`: source-to-target parity now rejects duplicate `TASK` events for the same `(task, type)`, duplicate `REWARD_PURCHASE` debit for the same purchase, and null target origin/type with explicit `SecurityException`. Three new unit tests.
- **FIN-10 — PostgreSQL isolation/concurrency**, commit `ca217b30b528278ca0b49b6a855ea55fc2626aae`: seven Testcontainers tests with QA-only `qa_fin09_ledger` keyed by `(family_id, operation_id, id)`. Covers same UUID in different families, different UUIDs in one family, failure isolation with shared UUID, full rollback of duplicate replay, concurrent submissions exactly once, cross-currency source-target tamper, and early mid-transaction rollback.
- **FIN-11 — signed V50 negative barriers**, commit `e91737013f344cba0a0112f7c8a400d5a072ef9a`: five additional tests in FIN-08 suite for signed archive orphan TASK, foreign currency, unknown exchange origin before any PostgreSQL write; and persisted target foreign-member, foreign-currency, foreign TASK-parent rejection.
- **Total new methods this batch: 15** (3 unit + 7 PostgreSQL isolation + 5 signed PostgreSQL); no claim that they passed. Together with previous FIN-01–08 they are read-only/test-only acceptance evidence, not product Restore authorization.
- Backend B continues to own transaction fencing, durable approvals and real Restore mapping. Backend D explicitly requests stable actual target ledger mapping and disposable production-schema data contract before real restore acceptance.
- **P0 blockers:** run pinned Maven/JUnit + PostgreSQL Testcontainers under Docker, capture logs, inspect concurrent CI results, verify signed source/export categories are complete for native restore. Unknown `POINT_EXCHANGE`/`REFUND` ancestry remains fail-closed until agreed contract.
- Suggested commands:
  - `mvn -Dtest=DisposableFinancialMappingParityTest,DisposableLedgerSourceGraphVerifierTest,DisposableSignedFinancialParityBridgeTest test`
  - `mvn -Dtest=DisposableFinancialLedgerPostgresParityTest,DisposableSignedFinancialPostgresParityTest,DisposableFinancialTenantIsolationPostgresTest test`
- Gate: **FIN-09–11 COMMITTED; QA/CI UNVERIFIED; PRODUCTION RESTORE HOLD; BKL-002 RELEASE HOLD.**


## FIN-12–FIN-14 — native-family gate and QA release evidence contract

**Architect handoff, 2026-11-18**

**FIN-12 + FIN-13:** commit `d243d127b39f151bcaf653775bf2a981c355b11c` introduces `DisposableFinancialNativeFamilyGatePostgresTest.java`, seven PostgreSQL 18/Testcontainers scenarios. This test **runs repository Flyway migrations** first, anchors target family identities to real `families(id)` and `families.data_epoch`, then uses isolated `qa_fin12_operation_claim` and `qa_fin12_financial_journal` tables. No production financial tables or native Restore executor are written.

- Happy path: persisted financial history resolves to exact per-member/per-currency signed balance and individual ledger lineage after mapped IDs.
- Atomicity: injected partial write aborts journal AND operation claim; clean retry produces one complete journal.
- Idempotency: a repeated operation UUID is rejected; committed ledger history stays identical.
- Epoch gate: stale family epoch prevents claim and financial writes.
- Cross-family gate: another family cannot reuse a committed operation UUID; its journal remains untouched.
- Concurrency: two submissions racing for the same operation ID may commit only one journal.
- Financial fraud gate: compensating changes with identical net totals fail exact ledger parity.

**FIN-14 acceptance evidence matrix (QA owners):**

| Gate | Evidence needed | Current evidence |
| --- | --- | --- |
| Java test compilation | Maven compiler success at pinned SHA | NOT OBSERVED |
| Pure signed V50 graph | `DisposableLedgerSourceGraphVerifierTest` PASS + XML | NOT OBSERVED |
| Financial mapped parity | `DisposableFinancialMappingParityTest`, `DisposableSignedFinancialParityBridgeTest` PASS + XML | NOT OBSERVED |
| Disposable PostgreSQL | `DisposableFinancialLedgerPostgresParityTest`, `DisposableSignedFinancialPostgresParityTest`, `DisposableFinancialTenantIsolationPostgresTest`, `DisposableFinancialNativeFamilyGatePostgresTest` PASS + Docker logs | NOT OBSERVED |
| Backend B actual Restore | Signed source-to-target map, native financial journal, approvals, rollback, re-entrant replay | NOT PROVEN BY D'S QA FIXTURES |
| Real family protection | QA authorization + test family only + invariant before/after other family | NOT PROVEN BY D'S QA FIXTURES |
| Release | Architect + QA + owner explicit signoff | HOLD |

Use this targeted Maven command when Docker/PostgreSQL Testcontainers is available:

`mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableFinancialMappingParityTest,DisposableSignedFinancialParityBridgeTest,DisposableFinancialLedgerPostgresParityTest,DisposableSignedFinancialPostgresParityTest,DisposableFinancialTenantIsolationPostgresTest,DisposableFinancialNativeFamilyGatePostgresTest test`

Request a pinned SHA, surefire XML, full output for failures, and GitHub Actions check URL. The narrow test suite cannot prove the real Restore executor works; do not promote `FIN-12–14` from COMMITTED to PASS without executing it. Production financial Restore and BKL-002 beta release remain HOLD.

**Dependency to Backend B:** publish an explicit contract for native financial ledger DB column names and ownership, one-to-one source-to-target mapping semantics by family/member/currency/task/purchase, epoch fencing, restore operation and idempotency, and disposable real-schema native Restore fixture. Backend D can then attach real journal acceptance without editing Backend B's transactional executor.

**Next work:** evaluate test failures/CI for this precise SHA, fix regressions, then construct a native-ledger acceptance adapter only after Backend B publishes a stable owned contract.


**FIN-12 extension:** `22fb4ff137333246d500e3a6e01dbb898f971dd6` added two additional negative PostgreSQL cases: missing/non-existent family cannot claim or insert history; post-commit family epoch change cannot replay ledger mutation. **FIN-12/13 total: 9 added JUnit tests**, all awaiting Maven/CI verification. The QA-only table scheme and production Restore boundary remain unchanged.


## FIN-15 verification and regression sweep — 2026-11-18

**Committed changes:** `3aae1b9b5cf4e8fca1cd1630794647c49414e083` adds three table-driven JUnit tests: all three ledger-ID pairwise swaps, every independent target provenance field (member/currency/amount/transaction/source-type/source-id), duplicate source task reward and purchase debit. `b2c9d2c787c8a86cd9ab0011eb74617db169bab7` adds an encrypted source fixture mismatch / pre-DB fail-closed case and an `assertNotEquals` guard against no-op fixture mutation; final test-name clarification `eaea300bce99e0e147b20e792aed4d84d1b95df4`.

**Verification reality:**
- This execution environment exposes Java 21 and GitHub file editing, but has **no Maven binary, Docker engine or network route to GitHub clone**. It cannot currently execute this project JUnit/Testcontainers suite. No local or remote PASS is claimed.
- GitHub combined commit status and PR workflow-run API do not expose a positive CI result for recent Backend D commits; absence of these fields is not failure or success.
- Do not remove BKL-002 HOLD on source-only committed test cases.

**QA execution contract — on the user's build host with Docker:**

```powershell
cd C:\Users\Oleh\IdeaProjects\family-points
git pull --ff-only origin main
git rev-parse HEAD
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableFinancialMappingParityTest,DisposableSignedFinancialParityBridgeTest test
mvn -Dtest=DisposableFinancialLedgerPostgresParityTest,DisposableSignedFinancialPostgresParityTest,DisposableFinancialTenantIsolationPostgresTest,DisposableFinancialNativeFamilyGatePostgresTest test
```

If Maven CLI is missing, use IntelliJ's Maven goal with the same `-Dtest` expressions. Record pinned SHA and exact Surefire XML from `target/surefire-reports` plus full logs for all failures. Docker/Testcontainers must be available before PG tests.

**FIN-15 exit:** Java compile succeeds, all 7 targeted test classes pass, no failures/errors/skips that conceal a required PostgreSQL gate, per-row provenance and category balances match, failure injection rolls back financial journal/approval atomically, architecture gate remains explicitly separate.

**Next FIN-16:** address concrete failures and add Backend B native Restore parity wiring only against its versioned financial mapping and authorization contract. No actual restore or family deletion without user approval.


## FIN-16 — 64-bit currency rollover prevention

- **Concrete bug fixed** in read-only V50 source graph verifier: archived `TASK_EXECUTIONS.reward_point_type_id` and `penalty_point_type_id` had integer-shape checks but no `canConvertToLong()` before `asLong()`. An arbitrary-precision JSON number such as `18446744073709551619` (= 2^64 + 3) can truncate to valid currency ID 3. This can incorrectly pass parent-catalog and ledger denomination checks. Commit `b44965f699dd5d55f1d7c9a32b49b1550b60b17b` adds explicit range validation to all three source task-currency check paths.
- **Regression fixtures:** commit `90c2449ee7242d71d0dc8e41fc11b64fa88c7c35` adds `DisposableLedgerCurrencyOverflowTest` with three signed archive tests: overflowing charged-task currency, overflowing unused-task parent currency, and negatively overflowing charged-task currency.
- **Parallel development safety:** two attempts to edit the large preexisting `DisposableLedgerSourceGraphVerifierTest.java` failed with GitHub HTTP 409 because another stream changed main concurrently. To prevent overwriting peers, a **new isolated regression test file** was created instead.
- **Verification:** Java 21 is available in the current container, but Maven and Docker CLI are not; local tests could not be executed. GitHub PR-run lookup may not include push workflows; do not infer PASS from an empty run list.
- **QA targeted command:** `mvn -Dtest=DisposableLedgerCurrencyOverflowTest,DisposableLedgerSourceGraphVerifierTest test`; then run the full finance matrix listed earlier and attach pinned SHA and surefire XML.
- **Gate:** FIN-16 code COMMITTED; Maven/CI UNVERIFIED; native financial Restore and BKL-002 release remain HOLD.
